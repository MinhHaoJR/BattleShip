package battleship.ui;

import battleship.model.Ship;
import battleship.model.ShipType;

import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coordinates 2-Player Co-op Titan Raid sessions across:
 * <ul>
 *     <li>{@link CoopTransport#LAN_HOST} / {@link CoopTransport#LAN_CLIENT}: Authoritative TCP LAN Host & Client</li>
 *     <li>{@link CoopTransport#HOTSEAT_2P}: Local 2-Player alternating turns on one computer</li>
 *     <li>{@link CoopTransport#AI_ALLY}: Solo Co-op practice with autonomous smart AI Wingman</li>
 * </ul>
 */
public final class CoopSession implements AutoCloseable {

    public static final int DEFAULT_PORT = 47589;
    public static final int DEFAULT_COOP_HP_PERCENT = 175;

    public enum CoopTransport {
        LAN_HOST("Tạo Phòng LAN", "Mở máy chủ LAN — chia sẻ IP:Port cho bạn bè vào phòng"),
        LAN_CLIENT("Vào Phòng LAN", "Nhập IP:Port máy bạn bè để ghép đội hình Raid Co-op"),
        HOTSEAT_2P("2P Chung Máy", "Hai người chơi luân phiên điều khiển trên cùng 1 máy"),
        AI_ALLY("Đồng Minh AI", "Tập luyện Co-op cùng Phó Đô Đốc Vega AI thông minh");

        public final String title;
        public final String description;

        CoopTransport(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    public record CoopConfig(
            TitanMode.TitanBoss boss,
            TitanMode.TitanDifficulty difficulty,
            OceanMapBackdrop.MapTheme mapTheme,
            OceanMapBackdrop.Season season,
            OceanMapBackdrop.WeatherMode weatherMode,
            String p1Name,
            String p2Name,
            int coopHpPercent
    ) {
        public CoopConfig normalized() {
            return new CoopConfig(
                    boss != null ? boss : TitanMode.TitanBoss.GODZILLA,
                    difficulty != null ? difficulty : TitanMode.TitanDifficulty.NORMAL,
                    mapTheme != null ? mapTheme : OceanMapBackdrop.MapTheme.NAVAL_CITADEL,
                    season != null ? season : OceanMapBackdrop.Season.SUMMER,
                    weatherMode != null ? weatherMode : OceanMapBackdrop.WeatherMode.CLEAR,
                    cleanName(p1Name, "Đô Đốc P1"),
                    cleanName(p2Name, "Phó Đô Đốc P2"),
                    Math.max(120, Math.min(300, coopHpPercent > 0 ? coopHpPercent : DEFAULT_COOP_HP_PERCENT))
            );
        }

        private static String cleanName(String raw, String fallback) {
            if (raw == null) return fallback;
            String t = raw.trim();
            if (t.isEmpty()) return fallback;
            return t.length() > 18 ? t.substring(0, 18) : t;
        }
    }

    public interface CoopListener {
        default void onStateChanged() {}
        default void onChatMessage(String sender, String text) {}
        default void onMatchStarted(long seed, CoopConfig config, List<Ship> sharedFleet) {}
        default void onPacketReceived(String cmd, String[] args) {}
        default void onDisconnected(String reason) {}
    }

    private final List<CoopListener> listeners = new CopyOnWriteArrayList<>();
    private final Object sendLock = new Object();

    private volatile CoopTransport transport = CoopTransport.AI_ALLY;
    private volatile CoopConfig config = new CoopConfig(
            TitanMode.TitanBoss.GODZILLA,
            TitanMode.TitanDifficulty.NORMAL,
            OceanMapBackdrop.MapTheme.NAVAL_CITADEL,
            OceanMapBackdrop.Season.SUMMER,
            OceanMapBackdrop.WeatherMode.CLEAR,
            "Đô Đốc P1",
            "Vega AI",
            DEFAULT_COOP_HP_PERCENT
    );

    private volatile boolean closed;
    private volatile boolean peerConnected = true;
    private volatile boolean peerReady = true;
    private volatile boolean localReady = true;
    private volatile String statusText = "Sẵn sàng cùng Đồng Minh AI Vega";
    private volatile String boundHostIp = "127.0.0.1";
    private volatile int boundPort = DEFAULT_PORT;
    private volatile long activeSeed = 0L;

    private ServerSocket serverSocket;
    private Socket socket;
    private PrintWriter outWriter;

    private final List<String> chatHistory = new ArrayList<>();

    public CoopSession() {
        List<String> ips = discoverLanIpv4Addresses();
        if (!ips.isEmpty()) boundHostIp = ips.get(0);
    }

    public void addListener(CoopListener l) {
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(CoopListener l) {
        listeners.remove(l);
    }

    public void clearNonMatchListeners(CoopListener keep) {
        listeners.removeIf(l -> l != keep);
    }

    public CoopTransport getTransport() { return transport; }
    public CoopConfig getConfig() { return config; }
    public boolean isNetworkMode() {
        return transport == CoopTransport.LAN_HOST || transport == CoopTransport.LAN_CLIENT;
    }
    public boolean isHostSide() { return transport != CoopTransport.LAN_CLIENT; }
    public int localPlayerIndex() { return transport == CoopTransport.LAN_CLIENT ? 2 : 1; }
    public boolean isPeerConnected() { return peerConnected; }
    public boolean isPeerReady() { return peerReady; }
    public boolean isLocalReady() { return localReady; }
    public String getStatusText() { return statusText; }
    public String getBoundHostIp() { return boundHostIp; }
    public int getBoundPort() { return boundPort; }
    public String getJoinAddressString() { return boundHostIp + ":" + boundPort; }
    public long getActiveSeed() { return activeSeed; }

    public synchronized List<String> getChatHistory() {
        return List.copyOf(chatHistory);
    }

    public boolean canLaunchMatch() {
        return switch (transport) {
            case AI_ALLY, HOTSEAT_2P -> true;
            case LAN_HOST -> peerConnected && peerReady;
            case LAN_CLIENT -> false; // Host launches authoritative raid
        };
    }

    /** Switches local offline transport mode cleanly, closing any prior network sockets. */
    public void configureOfflineMode(CoopTransport mode) {
        closeNetworkOnly();
        CoopTransport next = (mode == CoopTransport.HOTSEAT_2P) ? CoopTransport.HOTSEAT_2P : CoopTransport.AI_ALLY;
        this.transport = next;
        this.peerConnected = true;
        this.peerReady = true;
        this.localReady = true;
        String defaultP2 = (next == CoopTransport.HOTSEAT_2P) ? "Người Chơi P2" : "Đô Đốc Vega AI";
        this.config = new CoopConfig(
                config.boss(), config.difficulty(), config.mapTheme(),
                config.season(), config.weatherMode(),
                config.p1Name(), defaultP2, config.coopHpPercent()
        ).normalized();
        this.statusText = (next == CoopTransport.HOTSEAT_2P)
                ? "Chế độ 2P Chung Máy — luân phiên lượt trên 1 máy"
                : "Đô Đốc Vega AI đã sẵn sàng yểm trợ!";
        fireStateChanged();
    }

    /** Starts an authoritative LAN Host TCP ServerSocket on preferredPort (or near fallback if busy). */
    public boolean startHosting(int preferredPort, String hostPlayerName) {
        closeNetworkOnly();
        this.transport = CoopTransport.LAN_HOST;
        this.peerConnected = false;
        this.peerReady = false;
        this.localReady = true;
        int startPort = (preferredPort > 1024 && preferredPort <= 65535) ? preferredPort : DEFAULT_PORT;

        List<String> ips = discoverLanIpv4Addresses();
        this.boundHostIp = ips.isEmpty() ? "127.0.0.1" : ips.get(0);

        ServerSocket ss = null;
        int chosenPort = startPort;
        for (int offset = 0; offset < 6; offset++) {
            int candidate = startPort + offset;
            if (candidate > 65535) break;
            try {
                ss = new ServerSocket(candidate);
                chosenPort = candidate;
                break;
            } catch (BindException ignored) {
                // try next port cleanly
            } catch (IOException ex) {
                this.statusText = "Lỗi mở phòng: " + ex.getMessage();
                fireStateChanged();
                return false;
            }
        }
        if (ss == null) {
            try {
                ss = new ServerSocket(0);
                chosenPort = ss.getLocalPort();
            } catch (IOException ex) {
                this.statusText = "Không mở được cổng mạng: " + ex.getMessage();
                fireStateChanged();
                return false;
            }
        }

        this.serverSocket = ss;
        this.boundPort = chosenPort;
        this.config = new CoopConfig(
                config.boss(), config.difficulty(), config.mapTheme(),
                config.season(), config.weatherMode(),
                hostPlayerName, "Đang chờ bạn...", config.coopHpPercent()
        ).normalized();
        this.statusText = "Đang chờ bạn kết nối tại " + boundHostIp + ":" + boundPort;
        appendSystemChat("Phòng LAN đã mở tại " + boundHostIp + ":" + boundPort);
        fireStateChanged();

        final ServerSocket acceptSs = ss;
        Thread acceptThread = new Thread(() -> runHostAcceptLoop(acceptSs), "Coop-LAN-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        return true;
    }

    /** Connects asynchronously to a LAN Host using ip (or "ip:port") and explicit port fallback. */
    public void connectToHost(String hostInput, int fallbackPort, String clientName) {
        closeNetworkOnly();
        this.transport = CoopTransport.LAN_CLIENT;
        this.peerConnected = false;
        this.peerReady = false;
        this.localReady = false;

        String trimmed = hostInput == null ? "" : hostInput.trim();
        String targetIp = trimmed.isEmpty() ? "127.0.0.1" : trimmed;
        int targetPort = (fallbackPort > 0 && fallbackPort <= 65535) ? fallbackPort : DEFAULT_PORT;
        int colonIdx = trimmed.lastIndexOf(':');
        if (colonIdx > 0 && colonIdx < trimmed.length() - 1) {
            String hostPart = trimmed.substring(0, colonIdx).trim();
            String portPart = trimmed.substring(colonIdx + 1).trim();
            try {
                targetPort = Integer.parseInt(portPart);
                if (!hostPart.isEmpty()) targetIp = hostPart;
            } catch (NumberFormatException ignored) {
                // keep hostInput as host name
            }
        }
        final String fIp = targetIp;
        final int fPort = Math.max(1, Math.min(65535, targetPort));
        final String cleanP2 = CoopConfig.cleanName(clientName, "Bạn Bè P2");

        this.boundHostIp = fIp;
        this.boundPort = fPort;
        this.config = new CoopConfig(
                config.boss(), config.difficulty(), config.mapTheme(),
                config.season(), config.weatherMode(),
                "Host P1", cleanP2, config.coopHpPercent()
        ).normalized();
        this.statusText = "Đang kết nối tới " + fIp + ":" + fPort + "...";
        fireStateChanged();

        Thread connectThread = new Thread(() -> {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(fIp, fPort), 3500);
                s.setTcpNoDelay(true);
                attachConnectedSocket(s, false, cleanP2);
            } catch (IOException ex) {
                if (!closed) {
                    SwingUtilities.invokeLater(() -> {
                        this.peerConnected = false;
                        this.statusText = "Không kết nối được (" + fIp + ":" + fPort + ")";
                        fireStateChanged();
                        fireDisconnected(this.statusText);
                    });
                }
            }
        }, "Coop-LAN-Connect");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    public void updateHostConfig(CoopConfig nextConfig) {
        if (nextConfig == null) return;
        this.config = nextConfig.normalized();
        if (transport == CoopTransport.LAN_HOST && peerConnected) {
            sendConfigToPeer();
        }
        fireStateChanged();
    }

    public void setLocalReady(boolean ready) {
        this.localReady = ready;
        if (transport == CoopTransport.LAN_CLIENT && peerConnected) {
            sendPacket("READY", ready ? "1" : "0", config.p2Name());
        }
        fireStateChanged();
    }

    public void sendChat(String text) {
        if (text == null || text.trim().isEmpty()) return;
        String clean = text.trim();
        if (clean.length() > 90) clean = clean.substring(0, 90);
        String sender = localPlayerIndex() == 1 ? config.p1Name() : config.p2Name();
        appendChat(sender, clean);
        if (isNetworkMode() && peerConnected) {
            sendPacket("CHAT", sender, clean);
        }
    }

    /** Host launches the Co-op raid and pushes identical seed, config, and fleet layout to Friend. */
    public List<Ship> launchMatchAsHost(List<Ship> candidateFleet) {
        long seed = new Random().nextLong();
        this.activeSeed = seed;
        List<Ship> fleet = (candidateFleet != null && candidateFleet.size() >= 5)
                ? new ArrayList<>(candidateFleet)
                : generateDefaultFleet(seed);
        String serialized = serializeShips(fleet);
        if (transport == CoopTransport.LAN_HOST && peerConnected) {
            sendPacket("START",
                    Long.toString(seed),
                    config.boss().name(),
                    config.difficulty().name(),
                    config.mapTheme().name(),
                    config.season().name(),
                    config.weatherMode().name(),
                    config.p1Name(),
                    config.p2Name(),
                    Integer.toString(config.coopHpPercent()),
                    serialized);
        }
        return fleet;
    }

    public void sendPacket(String cmd, Object... args) {
        if (!isNetworkMode() || !peerConnected) return;
        StringBuilder sb = new StringBuilder(cmd);
        if (args != null) {
            for (Object a : args) {
                sb.append('|').append(escapeToken(a == null ? "" : Objects.toString(a)));
            }
        }
        final String line = sb.toString();
        synchronized (sendLock) {
            if (outWriter != null) {
                outWriter.println(line);
                outWriter.flush();
            }
        }
    }

    private void runHostAcceptLoop(ServerSocket ss) {
        try {
            while (!closed && !ss.isClosed()) {
                Socket incoming = ss.accept();
                if (closed) {
                    closeQuietly(incoming);
                    break;
                }
                if (this.socket != null && !this.socket.isClosed()) {
                    // Reject second simultaneous connection politely
                    try (PrintWriter reject = new PrintWriter(new OutputStreamWriter(
                            incoming.getOutputStream(), StandardCharsets.UTF_8), true)) {
                        reject.println("BUSY");
                    } catch (IOException ignored) {}
                    closeQuietly(incoming);
                    continue;
                }
                incoming.setTcpNoDelay(true);
                attachConnectedSocket(incoming, true, config.p1Name());
            }
        } catch (IOException ignored) {
            // Expected when closeNetworkOnly() closes serverSocket
        }
    }

    private void attachConnectedSocket(Socket s, boolean asHost, String localName) throws IOException {
        this.socket = s;
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        synchronized (sendLock) {
            this.outWriter = writer;
        }
        this.peerConnected = true;

        if (asHost) {
            SwingUtilities.invokeLater(() -> {
                this.statusText = "Đã ghép nối với bạn bè! Chờ bạn nhấn Sẵn Sàng...";
                appendSystemChat("Bạn bè đã kết nối vào phòng!");
                fireStateChanged();
            });
            sendPacket("WELCOME", localName);
            sendConfigToPeer();
        } else {
            SwingUtilities.invokeLater(() -> {
                this.statusText = "Đã vào phòng Host (" + boundHostIp + ":" + boundPort + ")";
                appendSystemChat("Đã kết nối tới Host!");
                fireStateChanged();
            });
            sendPacket("HELLO", localName);
        }

        Thread readThread = new Thread(() -> runReadLoop(s, reader), asHost ? "Coop-Host-Read" : "Coop-Client-Read");
        readThread.setDaemon(true);
        readThread.start();
    }

    private void sendConfigToPeer() {
        sendPacket("CONFIG",
                config.boss().name(),
                config.difficulty().name(),
                config.mapTheme().name(),
                config.season().name(),
                config.weatherMode().name(),
                config.p1Name(),
                config.p2Name(),
                Integer.toString(config.coopHpPercent()));
    }

    private void runReadLoop(Socket s, BufferedReader reader) {
        try {
            String line;
            while (!closed && !s.isClosed() && (line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] rawParts = splitEscaped(line);
                if (rawParts.length == 0) continue;
                String cmd = rawParts[0];
                String[] args = new String[Math.max(0, rawParts.length - 1)];
                System.arraycopy(rawParts, 1, args, 0, args.length);
                SwingUtilities.invokeLater(() -> handleIncomingLine(cmd, args));
            }
        } catch (IOException ignored) {
            // Connection dropped or closed locally
        } finally {
            if (!closed) {
                SwingUtilities.invokeLater(() -> {
                    boolean wasConn = this.peerConnected;
                    this.peerConnected = false;
                    this.peerReady = false;
                    if (wasConn) {
                        this.statusText = isHostSide()
                                ? "Bạn bè đã rời phòng — đang chờ tại " + boundHostIp + ":" + boundPort
                                : "Mất kết nối tới Host";
                        appendSystemChat(this.statusText);
                        fireStateChanged();
                        fireDisconnected(this.statusText);
                    }
                });
            }
            closeQuietly(s);
            if (this.socket == s) this.socket = null;
            synchronized (sendLock) {
                this.outWriter = null;
            }
        }
    }

    private void handleIncomingLine(String cmd, String[] args) {
        switch (cmd) {
            case "HELLO" -> {
                String p2 = args.length >= 1 ? CoopConfig.cleanName(args[0], "Bạn Bè P2") : "Bạn Bè P2";
                this.config = new CoopConfig(
                        config.boss(), config.difficulty(), config.mapTheme(),
                        config.season(), config.weatherMode(),
                        config.p1Name(), p2, config.coopHpPercent()
                ).normalized();
                sendConfigToPeer();
                fireStateChanged();
            }
            case "WELCOME" -> {
                String p1 = args.length >= 1 ? CoopConfig.cleanName(args[0], "Host P1") : "Host P1";
                this.config = new CoopConfig(
                        config.boss(), config.difficulty(), config.mapTheme(),
                        config.season(), config.weatherMode(),
                        p1, config.p2Name(), config.coopHpPercent()
                ).normalized();
                this.peerReady = true;
                fireStateChanged();
            }
            case "CONFIG" -> {
                if (args.length >= 8) {
                    this.config = new CoopConfig(
                            parseEnum(TitanMode.TitanBoss.class, args[0], TitanMode.TitanBoss.GODZILLA),
                            parseEnum(TitanMode.TitanDifficulty.class, args[1], TitanMode.TitanDifficulty.NORMAL),
                            parseEnum(OceanMapBackdrop.MapTheme.class, args[2], OceanMapBackdrop.MapTheme.NAVAL_CITADEL),
                            parseEnum(OceanMapBackdrop.Season.class, args[3], OceanMapBackdrop.Season.SUMMER),
                            parseEnum(OceanMapBackdrop.WeatherMode.class, args[4], OceanMapBackdrop.WeatherMode.CLEAR),
                            args[5],
                            transport == CoopTransport.LAN_CLIENT ? config.p2Name() : args[6],
                            parseInt(args[7], DEFAULT_COOP_HP_PERCENT)
                    ).normalized();
                    fireStateChanged();
                }
            }
            case "READY" -> {
                boolean ready = args.length >= 1 && "1".equals(args[0]);
                this.peerReady = ready;
                if (args.length >= 2 && isHostSide()) {
                    String p2 = CoopConfig.cleanName(args[1], config.p2Name());
                    this.config = new CoopConfig(
                            config.boss(), config.difficulty(), config.mapTheme(),
                            config.season(), config.weatherMode(),
                            config.p1Name(), p2, config.coopHpPercent()
                    ).normalized();
                }
                this.statusText = ready ? "Bạn bè ĐÃ SẴN SÀNG! Có thể Xuất Kích!" : "Bạn bè đã nối phòng — chờ Sẵn Sàng...";
                fireStateChanged();
            }
            case "CHAT" -> {
                if (args.length >= 2) {
                    appendChat(args[0], args[1]);
                }
            }
            case "START" -> {
                if (args.length >= 10) {
                    long seed = parseLong(args[0], System.nanoTime());
                    this.activeSeed = seed;
                    CoopConfig startedCfg = new CoopConfig(
                            parseEnum(TitanMode.TitanBoss.class, args[1], TitanMode.TitanBoss.GODZILLA),
                            parseEnum(TitanMode.TitanDifficulty.class, args[2], TitanMode.TitanDifficulty.NORMAL),
                            parseEnum(OceanMapBackdrop.MapTheme.class, args[3], OceanMapBackdrop.MapTheme.NAVAL_CITADEL),
                            parseEnum(OceanMapBackdrop.Season.class, args[4], OceanMapBackdrop.Season.SUMMER),
                            parseEnum(OceanMapBackdrop.WeatherMode.class, args[5], OceanMapBackdrop.WeatherMode.CLEAR),
                            args[6], args[7], parseInt(args[8], DEFAULT_COOP_HP_PERCENT)
                    ).normalized();
                    this.config = startedCfg;
                    List<Ship> fleet = deserializeShips(args[9]);
                    if (fleet.size() < 5) fleet = generateDefaultFleet(seed);
                    for (CoopListener l : listeners) l.onMatchStarted(seed, startedCfg, fleet);
                }
            }
            case "BUSY" -> {
                this.peerConnected = false;
                this.statusText = "Phòng Host đã đủ 2 người chơi!";
                fireStateChanged();
                fireDisconnected(this.statusText);
            }
            default -> {
                for (CoopListener l : listeners) l.onPacketReceived(cmd, args);
            }
        }
    }

    private void appendSystemChat(String msg) {
        appendChat("HỆ THỐNG", msg);
    }

    private void appendChat(String sender, String msg) {
        String formatted = sender + ": " + msg;
        synchronized (chatHistory) {
            chatHistory.add(formatted);
            if (chatHistory.size() > 30) chatHistory.remove(0);
        }
        for (CoopListener l : listeners) l.onChatMessage(sender, msg);
    }

    private void fireStateChanged() {
        Runnable task = () -> {
            for (CoopListener l : listeners) l.onStateChanged();
        };
        if (SwingUtilities.isEventDispatchThread()) task.run();
        else SwingUtilities.invokeLater(task);
    }

    private void fireDisconnected(String reason) {
        for (CoopListener l : listeners) l.onDisconnected(reason);
    }

    public void closeNetworkOnly() {
        synchronized (sendLock) {
            if (outWriter != null) {
                try { outWriter.close(); } catch (Exception ignored) {}
                outWriter = null;
            }
        }
        closeQuietly(socket);
        socket = null;
        closeQuietly(serverSocket);
        serverSocket = null;
    }

    @Override
    public void close() {
        closed = true;
        closeNetworkOnly();
    }

    private static void closeQuietly(Socket s) {
        if (s != null) {
            try { s.close(); } catch (Exception ignored) {}
        }
    }

    private static void closeQuietly(ServerSocket ss) {
        if (ss != null) {
            try { ss.close(); } catch (Exception ignored) {}
        }
    }

    /** Discovers active non-loopback IPv4 LAN addresses, falling back to 127.0.0.1. */
    public static List<String> discoverLanIpv4Addresses() {
        List<String> preferred = new ArrayList<>();
        List<String> others = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces != null && ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            preferred.add(ip);
                        } else {
                            others.add(ip);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // fallback cleanly
        }
        preferred.addAll(others);
        if (preferred.isEmpty()) preferred.add("127.0.0.1");
        return preferred;
    }

    public static List<Ship> generateDefaultFleet(long seed) {
        battleship.model.CellState[][] tmp = new battleship.model.CellState[battleship.model.Board.SIZE][battleship.model.Board.SIZE];
        SandboxController.clearGrid(tmp);
        List<Ship> list = new ArrayList<>();
        SandboxController.generateRandomFleet(tmp, list, new Random(seed));
        return list;
    }

    public static String serializeShips(List<Ship> ships) {
        if (ships == null || ships.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ships.size(); i++) {
            Ship s = ships.get(i);
            if (i > 0) sb.append(';');
            sb.append(s.type.name()).append(',')
              .append(s.row).append(',')
              .append(s.col).append(',')
              .append(s.horizontal ? '1' : '0');
        }
        return sb.toString();
    }

    public static List<Ship> deserializeShips(String spec) {
        List<Ship> out = new ArrayList<>();
        if (spec == null || spec.isBlank()) return out;
        for (String item : spec.split(";")) {
            String[] f = item.split(",");
            if (f.length < 4) continue;
            try {
                ShipType type = ShipType.valueOf(f[0].trim());
                int r = Integer.parseInt(f[1].trim());
                int c = Integer.parseInt(f[2].trim());
                boolean h = "1".equals(f[3].trim());
                out.add(new Ship(type, r, c, h));
            } catch (Exception ignored) {}
        }
        return out;
    }

    /** Returns owning Co-op player index (1 = Cyan Heavy Flagships, 2 = Emerald Escorts). */
    public static int ownerPlayerForShip(ShipType type) {
        if (type == ShipType.CRUISER || type == ShipType.SUBMARINE) return 2;
        return 1;
    }

    public static String escapeToken(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(raw.length() + 4);
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\\') sb.append("\\\\");
            else if (ch == '|') sb.append("\\p");
            else if (ch == '\n' || ch == '\r') sb.append(' ');
            else sb.append(ch);
        }
        return sb.toString();
    }

    public static String[] splitEscaped(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean esc = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (esc) {
                if (ch == 'p') cur.append('|');
                else cur.append(ch);
                esc = false;
            } else if (ch == '\\') {
                esc = true;
            } else if (ch == '|') {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        if (esc) cur.append('\\');
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> cls, String raw, E fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Enum.valueOf(cls, raw.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int parseInt(String raw, int fallback) {
        try { return Integer.parseInt(raw.trim()); } catch (Exception e) { return fallback; }
    }

    private static long parseLong(String raw, long fallback) {
        try { return Long.parseLong(raw.trim()); } catch (Exception e) { return fallback; }
    }
}
