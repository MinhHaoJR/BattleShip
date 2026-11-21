package battleship.ui;

import battleship.model.ShipType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Dedicated 1-100 Campaign & Minigame Stage Select Screen ("Bản Đồ Chiến Dịch 1–100").
 * Renders 5 Ocean Chapters × 20 Stages each along an interactive serpentine naval S-path,
 * paired with a live Stage Briefing card and 1-click sortie launch.
 */
public final class StageSelectPanel extends JPanel {
    public enum StageFilter {
        ALL("Tất Cả"),
        MINIGAME("Mini-game"),
        ZOMBIE("Zombie"),
        BOSS("Boss"),
        UNPERFECT("Chưa 3★");

        public final String label;

        StageFilter(String label) { this.label = label; }

        public boolean matches(CampaignMode.CampaignStage s) {
            return switch (this) {
                case ALL -> true;
                case MINIGAME -> s.ruleType() == CampaignMode.StageRuleType.SNIPER_AMMO
                        || s.ruleType() == CampaignMode.StageRuleType.BLITZ_TIMED
                        || s.ruleType() == CampaignMode.StageRuleType.SONAR_FOG;
                case ZOMBIE -> s.ruleType() == CampaignMode.StageRuleType.ZOMBIE_ASSAULT;
                case BOSS -> s.isBossStage();
                case UNPERFECT -> CampaignMode.getStars(s.stageNumber()) < 3;
            };
        }
    }

    /** Visual transformation state of the 2.5D Isometric Campaign City World. */
    public enum CityWorldState {
        NORMAL("THÀNH PHỐ HẢI CẢNG · BÌNH THƯỜNG", new Color(0x38BDF8)),
        ZOMBIE("BÁO ĐỘNG ĐẠI DỊCH · THÀNH PHỐ ZOMBIE", new Color(0x22C55E)),
        BOSS("CHIẾN SỰ KAIJU · KHU VỰC BOSS TITAN", new Color(0xEF4444));

        public final String bannerLabel;
        public final Color accent;

        CityWorldState(String bannerLabel, Color accent) {
            this.bannerLabel = bannerLabel;
            this.accent = accent;
        }
    }

    private final Map<CampaignMode.CampaignChapter, ChapterTabButton> chapterTabs =
            new EnumMap<>(CampaignMode.CampaignChapter.class);
    private final Map<StageFilter, FilterChipButton> filterChips = new EnumMap<>(StageFilter.class);
    private final List<StageNodeTile> stageTiles = new ArrayList<>(CampaignMode.STAGES_PER_CHAPTER);
    private final SerpentineGridCanvas serpentineCanvas = new SerpentineGridCanvas();

    private final JLabel totalProgressBadge = new JLabel();
    private final UnlockToggleButton unlockToggleButton = new UnlockToggleButton();
    private final JTextField jumpInput = new JTextField(3);
    private final JButton jumpButton = new SmallActionButton("Đi");
    private final JButton chapterStoryButton = new SmallActionButton("Đọc Chương");
    private final JLabel chapterBannerTitle = new JLabel();
    private final JLabel chapterBannerSub = new JLabel();
    private final JLabel filterCountBadge = new JLabel();

    private final JPanel mainContent = new JPanel(new BorderLayout(0, 10));
    private final StoryComicOverlay storyOverlay = new StoryComicOverlay(this);

    // Right Briefing Card widgets
    private final JLabel briefingStageBadge = new JLabel();
    private final JLabel briefingRuleBadge = new JLabel();
    private final JLabel briefingLockBadge = new JLabel();
    private final HomeScreenPanel.WrappedTextLabel briefingTitle =
            new HomeScreenPanel.WrappedTextLabel(new Font(Font.SANS_SERIF, Font.BOLD, 15), UiTokens.TEXT_PRIMARY);
    private final StageDioramaCanvas dioramaCanvas = new StageDioramaCanvas();
    private final BestStarsBar bestStarsBar = new BestStarsBar();
    private final JLabel chipFleet = createMetaChip();
    private final JLabel chipLimit = createMetaChip();
    private final JLabel chipMana = createMetaChip();
    private final StoryTeaserCard storyTeaserCard = new StoryTeaserCard();
    private final HomeScreenPanel.WrappedTextLabel briefingRuleText =
            new HomeScreenPanel.WrappedTextLabel(new Font(Font.SANS_SERIF, Font.PLAIN, 12), UiTokens.TEXT_PRIMARY);
    private final JLabel briefingEnvLabel = new JLabel();
    private final HomeScreenPanel.WrappedTextLabel briefingThreeStarText =
            new HomeScreenPanel.WrappedTextLabel(new Font(Font.SANS_SERIF, Font.PLAIN, 11), new Color(0xFDE68A));
    private final TacticalButton launchQuickButton = new TacticalButton("Xuất Kích Màn 1", true);
    private final TacticalButton launchSetupButton = new TacticalButton("Xếp Tàu", false);
    private final TacticalButton storyStageButton = new TacticalButton("Truyện Tranh", false);

    private CampaignMode.CampaignChapter selectedChapter = CampaignMode.CampaignChapter.CH1_NAVAL;
    private CampaignMode.CampaignStage selectedStage = CampaignMode.stage(1);
    private StageFilter activeFilter = StageFilter.ALL;
    private Timer animTimer;

    private Consumer<CampaignMode.CampaignStage> onLaunchQuick;
    private Consumer<CampaignMode.CampaignStage> onOpenSetup;

    public StageSelectPanel() {
        SpriteAssets.ensureAllMapPropsLoaded();
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(UiTokens.BG_CANVAS);

        mainContent.setOpaque(false);
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 14));
        mainContent.add(buildTopControls(), BorderLayout.NORTH);

        JPanel centerSplit = new JPanel(new BorderLayout(12, 0));
        centerSplit.setOpaque(false);
        centerSplit.add(buildChapterMapPanel(), BorderLayout.CENTER);
        centerSplit.add(buildBriefingCard(), BorderLayout.EAST);
        mainContent.add(centerSplit, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);

        animTimer = new Timer(60, e -> repaint());

        selectStage(1);
    }

    @Override public void addNotify() {
        super.addNotify();
        if (animTimer != null && !animTimer.isRunning()) animTimer.start();
    }

    @Override public void removeNotify() {
        if (animTimer != null) animTimer.stop();
        super.removeNotify();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        double t = OceanMapBackdrop.elapsedSeconds();
        OceanMapBackdrop.setMapTheme(selectedChapter.theme);
        OceanMapBackdrop.setSeason(selectedStage.season());
        OceanMapBackdrop.setWeatherMode(selectedStage.weather());
        OceanMapBackdrop.paintOceanMap(g2, getWidth(), getHeight(), t, false);
        OceanMapBackdrop.paintAtmosphericWeather(g2, getWidth(), getHeight(), t);
        OceanMapBackdrop.paintPatrollingDragons(g2, getWidth(), getHeight(), t);
        g2.setColor(new Color(4, 10, 20, 152));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    private JPanel buildTopControls() {
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        // Row 1: 5 Chapter Tabs
        JPanel tabRow = new JPanel(new GridLayout(1, 5, 6, 0));
        tabRow.setOpaque(false);
        tabRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        tabRow.setPreferredSize(new Dimension(0, 38));
        tabRow.setAlignmentX(LEFT_ALIGNMENT);
        for (CampaignMode.CampaignChapter ch : CampaignMode.CampaignChapter.values()) {
            ChapterTabButton btn = new ChapterTabButton(ch);
            btn.addActionListener(e -> {
                SoundEngine.playUiClick();
                selectChapter(ch.index);
            });
            chapterTabs.put(ch, btn);
            tabRow.add(btn);
        }
        top.add(tabRow);
        top.add(Box.createVerticalStrut(6));

        // Row 2: Filters (WEST) + Progress / Unlock Toggle / Jump (EAST)
        JPanel strip = new JPanel(new BorderLayout(8, 0));
        strip.setOpaque(false);
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        strip.setPreferredSize(new Dimension(0, 32));
        strip.setAlignmentX(LEFT_ALIGNMENT);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        filterPanel.setOpaque(false);
        for (StageFilter f : StageFilter.values()) {
            FilterChipButton chip = new FilterChipButton(f);
            chip.addActionListener(e -> {
                SoundEngine.playUiClick();
                setFilter(f);
            });
            filterChips.put(f, chip);
            filterPanel.add(chip);
        }
        strip.add(filterPanel, BorderLayout.WEST);

        JPanel rightStrip = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        rightStrip.setOpaque(false);

        totalProgressBadge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        totalProgressBadge.setForeground(new Color(0xFDE68A));
        rightStrip.add(totalProgressBadge);

        unlockToggleButton.setPreferredSize(new Dimension(112, 26));
        unlockToggleButton.addActionListener(e -> {
            SoundEngine.playUiClick();
            CampaignMode.toggleSequentialUnlockOnly();
            refreshFromProgress();
        });
        rightStrip.add(unlockToggleButton);

        JLabel jumpLbl = new JLabel("Màn:");
        jumpLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        jumpLbl.setForeground(UiTokens.TEXT_MUTED);
        rightStrip.add(jumpLbl);

        jumpInput.setText("1");
        jumpInput.setHorizontalAlignment(SwingConstants.CENTER);
        jumpInput.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        jumpInput.setForeground(UiTokens.TEXT_PRIMARY);
        jumpInput.setBackground(new Color(14, 24, 42));
        jumpInput.setCaretColor(UiTokens.ACCENT_SIGNAL);
        jumpInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105), 1),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        jumpInput.setPreferredSize(new Dimension(42, 26));
        jumpInput.addActionListener(e -> performJumpFromField());
        rightStrip.add(jumpInput);

        jumpButton.setPreferredSize(new Dimension(42, 26));
        jumpButton.addActionListener(e -> performJumpFromField());
        rightStrip.add(jumpButton);

        strip.add(rightStrip, BorderLayout.EAST);
        top.add(strip);
        return top;
    }

    private JPanel buildChapterMapPanel() {
        JPanel mapCard = new JPanel(new BorderLayout(0, 6)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(9, 17, 32, 208));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(selectedChapter.accent.getRed(), selectedChapter.accent.getGreen(),
                        selectedChapter.accent.getBlue(), 105));
                g2.setStroke(new BasicStroke(1.3f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                g2.dispose();
            }
        };
        mapCard.setOpaque(false);
        mapCard.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel head = new JPanel(new BorderLayout(8, 2));
        head.setOpaque(false);
        JPanel topHeadRow = new JPanel(new BorderLayout(8, 0));
        topHeadRow.setOpaque(false);
        chapterBannerTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        filterCountBadge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        filterCountBadge.setForeground(UiTokens.TEXT_MUTED);
        chapterStoryButton.setPreferredSize(new Dimension(92, 22));
        chapterStoryButton.setToolTipText("Mở truyện tranh hoạt hình mở đầu Chương đang chọn");
        chapterStoryButton.addActionListener(e -> {
            SoundEngine.playUiClick();
            openChapterStoryComic(selectedChapter.index);
        });
        JPanel eastHead = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        eastHead.setOpaque(false);
        eastHead.add(filterCountBadge);
        eastHead.add(chapterStoryButton);
        topHeadRow.add(chapterBannerTitle, BorderLayout.CENTER);
        topHeadRow.add(eastHead, BorderLayout.EAST);
        head.add(topHeadRow, BorderLayout.NORTH);
        chapterBannerSub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        chapterBannerSub.setForeground(UiTokens.TEXT_MUTED);
        head.add(chapterBannerSub, BorderLayout.SOUTH);
        mapCard.add(head, BorderLayout.NORTH);

        for (int i = 0; i < CampaignMode.STAGES_PER_CHAPTER; i++) {
            StageNodeTile tile = new StageNodeTile(i);
            stageTiles.add(tile);
            serpentineCanvas.add(tile);
        }
        mapCard.add(serpentineCanvas, BorderLayout.CENTER);
        return mapCard;
    }

    private JPanel buildBriefingCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(10, 18, 33, 230));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                Color acc = selectedStage.ruleType().accent;
                g2.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 145));
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(332, 0));
        card.setMinimumSize(new Dimension(320, 0));
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Top header
        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.setOpaque(false);
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badges.setOpaque(false);
        briefingStageBadge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        briefingRuleBadge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        briefingLockBadge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        badges.add(briefingStageBadge);
        badges.add(briefingRuleBadge);
        badges.add(briefingLockBadge);
        north.add(badges, BorderLayout.NORTH);
        north.add(briefingTitle, BorderLayout.CENTER);
        card.add(north, BorderLayout.NORTH);

        // Center briefing body
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        dioramaCanvas.setAlignmentX(LEFT_ALIGNMENT);
        dioramaCanvas.setPreferredSize(new Dimension(308, 102));
        dioramaCanvas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 102));
        body.add(dioramaCanvas);
        body.add(Box.createVerticalStrut(5));

        bestStarsBar.setAlignmentX(LEFT_ALIGNMENT);
        bestStarsBar.setPreferredSize(new Dimension(308, 22));
        bestStarsBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        body.add(bestStarsBar);
        body.add(Box.createVerticalStrut(5));

        JPanel chipsRow = new JPanel(new GridLayout(1, 3, 6, 0));
        chipsRow.setOpaque(false);
        chipsRow.setAlignmentX(LEFT_ALIGNMENT);
        chipsRow.setPreferredSize(new Dimension(308, 24));
        chipsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        chipsRow.add(chipFleet);
        chipsRow.add(chipLimit);
        chipsRow.add(chipMana);
        body.add(chipsRow);
        body.add(Box.createVerticalStrut(6));

        storyTeaserCard.setAlignmentX(LEFT_ALIGNMENT);
        storyTeaserCard.setPreferredSize(new Dimension(308, 54));
        storyTeaserCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        body.add(storyTeaserCard);
        body.add(Box.createVerticalStrut(6));

        JLabel ruleHead = new JLabel("LUẬT CHƠI & MÔI TRƯỜNG");
        ruleHead.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        ruleHead.setForeground(UiTokens.ACCENT_SIGNAL);
        ruleHead.setAlignmentX(LEFT_ALIGNMENT);
        body.add(ruleHead);
        body.add(Box.createVerticalStrut(2));

        briefingRuleText.setAlignmentX(LEFT_ALIGNMENT);
        body.add(briefingRuleText);
        body.add(Box.createVerticalStrut(3));

        briefingEnvLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        briefingEnvLabel.setForeground(UiTokens.TEXT_MUTED);
        briefingEnvLabel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(briefingEnvLabel);
        body.add(Box.createVerticalStrut(5));

        JPanel starBox = new JPanel(new BorderLayout(0, 3)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(45, 33, 12, 175));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 9, 9);
                g2.setColor(new Color(251, 191, 36, 145));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
                g2.dispose();
            }
        };
        starBox.setOpaque(false);
        starBox.setAlignmentX(LEFT_ALIGNMENT);
        starBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        starBox.setBorder(BorderFactory.createEmptyBorder(6, 9, 6, 9));
        JLabel starHead = new JLabel("ĐIỀU KIỆN ĐẠT 3★");
        starHead.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        starHead.setForeground(new Color(0xFBBF24));
        starBox.add(starHead, BorderLayout.NORTH);
        starBox.add(briefingThreeStarText, BorderLayout.CENTER);
        body.add(starBox);
        body.add(Box.createVerticalGlue());

        card.add(body, BorderLayout.CENTER);

        // Bottom CTAs
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

        launchQuickButton.setAlignmentX(LEFT_ALIGNMENT);
        launchQuickButton.setPreferredSize(new Dimension(308, 40));
        launchQuickButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        launchQuickButton.addActionListener(e -> {
            SoundEngine.playUiClick();
            if (CampaignMode.isUnlocked(selectedStage.stageNumber()) && onLaunchQuick != null) {
                onLaunchQuick.accept(selectedStage);
            }
        });
        footer.add(launchQuickButton);
        footer.add(Box.createVerticalStrut(6));

        JPanel subCtaRow = new JPanel(new GridLayout(1, 2, 6, 0));
        subCtaRow.setOpaque(false);
        subCtaRow.setAlignmentX(LEFT_ALIGNMENT);
        subCtaRow.setPreferredSize(new Dimension(308, 34));
        subCtaRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        launchSetupButton.addActionListener(e -> {
            SoundEngine.playUiClick();
            if (CampaignMode.isUnlocked(selectedStage.stageNumber()) && onOpenSetup != null) {
                onOpenSetup.accept(selectedStage);
            }
        });
        storyStageButton.setToolTipText("Xem truyện tranh hoạt hình cốt truyện của màn đang chọn");
        storyStageButton.addActionListener(e -> {
            SoundEngine.playUiClick();
            openStageStoryComic(selectedStage.stageNumber());
        });
        subCtaRow.add(launchSetupButton);
        subCtaRow.add(storyStageButton);
        footer.add(subCtaRow);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    private static JLabel createMetaChip() {
        JLabel lbl = new JLabel("", SwingConstants.CENTER);
        lbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        lbl.setForeground(UiTokens.TEXT_PRIMARY);
        lbl.setOpaque(true);
        lbl.setBackground(new Color(18, 31, 52));
        lbl.setBorder(BorderFactory.createLineBorder(new Color(62, 86, 120), 1));
        return lbl;
    }

    private void performJumpFromField() {
        try {
            int n = Integer.parseInt(jumpInput.getText().trim());
            int clamped = Math.max(1, Math.min(CampaignMode.TOTAL_STAGES, n));
            SoundEngine.playUiClick();
            selectStage(clamped);
        } catch (NumberFormatException ex) {
            jumpInput.setText(String.valueOf(selectedStage.stageNumber()));
        }
    }

    public void triggerJump(int stageNumber) {
        int clamped = Math.max(1, Math.min(CampaignMode.TOTAL_STAGES, stageNumber));
        jumpInput.setText(String.valueOf(clamped));
        selectStage(clamped);
    }

    public void setOnLaunchQuick(Consumer<CampaignMode.CampaignStage> listener) {
        this.onLaunchQuick = listener;
    }

    public void setOnOpenSetup(Consumer<CampaignMode.CampaignStage> listener) {
        this.onOpenSetup = listener;
    }

    public void selectChapter(int chapterIndex) {
        CampaignMode.CampaignChapter target = CampaignMode.CampaignChapter.byIndex(chapterIndex);
        this.selectedChapter = target;
        if (selectedStage == null || selectedStage.chapter() != target) {
            selectedStage = CampaignMode.stage(target.startStage);
        }
        jumpInput.setText(String.valueOf(selectedStage.stageNumber()));
        refreshFromProgress();
    }

    public void selectStage(int stageNumber) {
        CampaignMode.CampaignStage st = CampaignMode.stage(stageNumber);
        this.selectedStage = st;
        this.selectedChapter = st.chapter();
        jumpInput.setText(String.valueOf(st.stageNumber()));
        refreshFromProgress();
    }

    public void setFilter(StageFilter filter) {
        this.activeFilter = filter != null ? filter : StageFilter.ALL;
        if (this.activeFilter != StageFilter.ALL && (selectedStage == null || !this.activeFilter.matches(selectedStage))) {
            for (CampaignMode.CampaignStage st : CampaignMode.stagesForChapter(selectedChapter.index)) {
                if (this.activeFilter.matches(st)) {
                    this.selectedStage = st;
                    this.jumpInput.setText(String.valueOf(st.stageNumber()));
                    break;
                }
            }
        }
        refreshFromProgress();
    }

    public void clickUnlockToggle() {
        CampaignMode.toggleSequentialUnlockOnly();
        refreshFromProgress();
    }

    /** Returns the active 2.5D City World visual state driven by current stage selection & filter. */
    public CityWorldState getCityWorldState() {
        if (activeFilter == StageFilter.ZOMBIE) return CityWorldState.ZOMBIE;
        if (activeFilter == StageFilter.BOSS) return CityWorldState.BOSS;
        if (selectedStage != null) {
            if (selectedStage.ruleType() == CampaignMode.StageRuleType.ZOMBIE_ASSAULT) return CityWorldState.ZOMBIE;
            if (selectedStage.isBossStage()) return CityWorldState.BOSS;
        }
        return CityWorldState.NORMAL;
    }

    public StageFilter getFilter() { return activeFilter; }

    public CampaignMode.CampaignChapter getSelectedChapter() { return selectedChapter; }

    public CampaignMode.CampaignStage getSelectedStage() { return selectedStage; }

    public int getChapterTabCount() { return chapterTabs.size(); }

    public int getStageTileCount() { return stageTiles.size(); }

    public String getTotalStarsText() { return totalProgressBadge.getText(); }

    public List<JComponent> getStageNodeComponentsForTest() {
        return List.copyOf(stageTiles);
    }

    public StoryComicOverlay getStoryOverlay() {
        return storyOverlay;
    }

    public boolean isStoryComicVisible() {
        return storyOverlay.isStoryActive();
    }

    public void openStageStoryComic(int stageNumber) {
        CampaignMode.CampaignStage st = CampaignMode.stage(stageNumber);
        storyOverlay.showEpisode(StoryMode.introEpisodeForStage(st), null);
    }

    public void openChapterStoryComic(int chapterIndex) {
        CampaignMode.CampaignChapter ch = CampaignMode.CampaignChapter.byIndex(chapterIndex);
        storyOverlay.showEpisode(StoryMode.chapterIntroEpisode(ch), null);
    }

    public void openStageEpilogueComic(int stageNumber) {
        CampaignMode.CampaignStage st = CampaignMode.stage(stageNumber);
        storyOverlay.showEpisode(StoryMode.epilogueEpisodeForStage(st), null);
    }

    public void closeStoryComic() {
        if (storyOverlay.isStoryActive()) storyOverlay.skipCutscene();
    }

    public void refreshFromProgress() {
        for (Map.Entry<CampaignMode.CampaignChapter, ChapterTabButton> entry : chapterTabs.entrySet()) {
            entry.getValue().sync(entry.getKey() == selectedChapter);
        }
        for (Map.Entry<StageFilter, FilterChipButton> entry : filterChips.entrySet()) {
            entry.getValue().sync(entry.getKey() == activeFilter);
        }

        totalProgressBadge.setText("★ " + CampaignMode.totalStars() + "/" + CampaignMode.MAX_TOTAL_STARS
                + " · " + CampaignMode.clearedCount() + "/100");
        unlockToggleButton.sync();

        chapterBannerTitle.setText(selectedChapter.fullTitle.toUpperCase()
                + " (" + selectedChapter.startStage + "–" + selectedChapter.endStage + ")");
        chapterBannerTitle.setForeground(selectedChapter.accent);
        chapterBannerSub.setText(selectedChapter.subtitle);

        List<CampaignMode.CampaignStage> chapterStages = CampaignMode.stagesForChapter(selectedChapter.index);
        int matchingCount = 0;
        for (int i = 0; i < stageTiles.size() && i < chapterStages.size(); i++) {
            CampaignMode.CampaignStage st = chapterStages.get(i);
            boolean matches = activeFilter.matches(st);
            if (matches) matchingCount++;
            stageTiles.get(i).bind(st, st.stageNumber() == selectedStage.stageNumber(), matches);
        }
        filterCountBadge.setText(activeFilter == StageFilter.ALL
                ? ("20 màn · ★ " + CampaignMode.chapterStars(selectedChapter.index) + "/60")
                : ("Khớp lọc: " + matchingCount + "/20"));

        updateBriefingCard();
        revalidate();
        repaint();
    }

    private void updateBriefingCard() {
        CampaignMode.CampaignStage st = selectedStage;
        boolean unlocked = CampaignMode.isUnlocked(st.stageNumber());

        briefingStageBadge.setText("MÀN " + st.stageNumber() + "/100");
        briefingStageBadge.setForeground(UiTokens.TEXT_PRIMARY);
        briefingRuleBadge.setText("· " + st.ruleType().shortLabel.toUpperCase());
        briefingRuleBadge.setForeground(st.ruleType().accent);
        briefingLockBadge.setText(unlocked ? "SẴN SÀNG" : "ĐANG KHÓA");
        briefingLockBadge.setForeground(unlocked ? UiTokens.STATUS_SUCCESS : new Color(0xF87171));

        briefingTitle.setText(st.title());

        if (st.isBossStage()) {
            chipFleet.setText(st.boss().shortLabel);
            chipLimit.setText(st.bossDifficulty().label);
            chipMana.setText("HP " + st.bossHpOverride());
        } else {
            chipFleet.setText(st.enemyFleetTypes().size() + " Tàu địch");
            if (st.shotLimit() > 0) {
                chipLimit.setText("Tối đa " + st.shotLimit() + " phát");
            } else if (st.timeLimitSec() > 0) {
                chipLimit.setText("Giới hạn " + st.timeLimitSec() + "s");
            } else {
                chipLimit.setText("Par " + st.parShots() + " phát");
            }
            chipMana.setText("Mana " + st.initialMana()
                    + (st.initialSonarHints() > 0 ? " · +" + st.initialSonarHints() + " Ô" : ""));
        }

        briefingRuleText.setText(st.ruleSummary());
        briefingEnvLabel.setText(st.chapter().theme.displayName + " · " + st.season().shortLabel + " · " + st.weather().shortLabel);
        briefingThreeStarText.setText(st.threeStarCondition());

        launchQuickButton.setText("Xuất Kích Màn " + st.stageNumber());
        launchQuickButton.setEnabled(unlocked);
        launchQuickButton.setToolTipText(unlocked
                ? "Tự xếp 5 tàu và khai chiến ngay Màn " + st.stageNumber()
                : "Chưa mở khóa: hãy vượt Màn " + (st.stageNumber() - 1) + " hoặc bấm Mở Hết 1–100");

        launchSetupButton.setText("Xếp Tàu Màn " + st.stageNumber());
        launchSetupButton.setEnabled(unlocked);
        launchSetupButton.setToolTipText(unlocked
                ? "Vào bảng xếp hạm đội trước khi đánh Màn " + st.stageNumber()
                : "Màn đang khóa");

        boolean keyEp = StoryMode.hasAutoIntroCutscene(st.stageNumber());
        storyStageButton.setText(keyEp ? "Truyện Chính ★" : "Xem Truyện");
        storyStageButton.setToolTipText(keyEp
                ? "Mở tập truyện tranh hoạt hình chính của Màn " + st.stageNumber()
                : "Xem hồ sơ truyện tranh chiến dịch Màn " + st.stageNumber());
        storyTeaserCard.repaint();
    }

    private static int serpentineCol(int indexInChapter) {
        int row = indexInChapter / 5;
        int col = indexInChapter % 5;
        return (row % 2 == 0) ? col : (4 - col);
    }

    private final class SerpentineGridCanvas extends JPanel {
        private SerpentineGridCanvas() {
            super(null);
            setOpaque(false);
        }

        @Override public void doLayout() {
            int w = getWidth();
            int h = getHeight();
            if (w <= 40 || h <= 40 || stageTiles.isEmpty()) return;
            int minY = 72;
            int maxBottom = h - 20;
            int usableH = Math.max(250, maxBottom - minY);
            int tileW = Math.min(92, Math.max(74, (w - 64) / 5 - 8));
            int tileH = Math.min(68, Math.max(58, (usableH - 108) / 4));
            int colSpan = (w - 36 - tileW * 5);
            int colGap = Math.max(8, Math.min(26, colSpan / 4));
            int rowSpanW = tileW * 5 + colGap * 4;
            int freeSlack = Math.max(0, w - 24 - rowSpanW);
            int baseStartX = (w - rowSpanW) / 2;

            int totalTilesH = tileH * 4;
            int rowGap = Math.max(14, (usableH - totalTilesH) / 3);
            int startY = minY + Math.max(0, (usableH - (totalTilesH + rowGap * 3)) / 2);

            for (int i = 0; i < stageTiles.size(); i++) {
                int row = i / 5;
                int col = serpentineCol(i);
                int rowSkew = (int) Math.round((row - 1.5) * Math.min(9.0, freeSlack / 4.2));
                int roadWaveY = (int) Math.round(Math.sin(i * 1.12) * Math.min(3.5, rowGap * 0.12));
                int rawX = baseStartX + rowSkew + col * (tileW + colGap);
                int rawY = startY + row * (tileH + rowGap) + roadWaveY;
                int x = Math.max(10, Math.min(w - tileW - 10, rawX));
                int y = Math.max(70, Math.min(h - tileH - 18, rawY));
                stageTiles.get(i).setBounds(x, y, tileW, tileH);
            }
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            double t = OceanMapBackdrop.elapsedSeconds();
            CityWorldState world = getCityWorldState();
            Color chapAcc = selectedChapter.accent;

            g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, 10, 10));
            drawCitySkyAndHorizon(g2, w, h, t, world, chapAcc);
            draw25DCityIslandDeck(g2, w, h, t, world, chapAcc);
            draw25DCityLandmarks(g2, w, h, t, world, chapAcc);
            draw25DRouteHighways(g2, t, world, chapAcc);
            draw25DWorldHudBanner(g2, w, world, chapAcc);
            g2.dispose();
        }

        private void drawCitySkyAndHorizon(Graphics2D g2, int w, int h, double t, CityWorldState world, Color chapAcc) {
            Color skyTop = switch (world) {
                case NORMAL -> new Color(8, 28, 56);
                case ZOMBIE -> new Color(7, 26, 19);
                case BOSS -> new Color(40, 10, 14);
            };
            Color seaBot = switch (world) {
                case NORMAL -> new Color(9, 22, 44);
                case ZOMBIE -> new Color(5, 16, 13);
                case BOSS -> new Color(18, 6, 14);
            };
            g2.setPaint(new GradientPaint(0, 0, skyTop, 0, h, seaBot));
            g2.fillRect(0, 0, w, h);

            // Distant sunlit/plague/volcanic radial glow at horizon center
            Color glowCol = switch (world) {
                case NORMAL -> new Color(chapAcc.getRed(), chapAcc.getGreen(), chapAcc.getBlue(), 68);
                case ZOMBIE -> new Color(34, 197, 94, 72);
                case BOSS -> new Color(249, 115, 22, 84);
            };
            g2.setPaint(new RadialGradientPaint(w * 0.52f, 44f, Math.max(80f, w * 0.42f),
                    new float[]{0f, 1f}, new Color[]{glowCol, new Color(0, 0, 0, 0)}));
            g2.fillRect(0, 0, w, 120);

            // 2.5D Distant Skyline Silhouette along y = 24..64
            int baseLine = 64;
            Color silCol = switch (world) {
                case NORMAL -> new Color(14, 38, 68, 210);
                case ZOMBIE -> new Color(12, 32, 24, 215);
                case BOSS -> new Color(34, 14, 18, 225);
            };
            Color winCol = switch (world) {
                case NORMAL -> new Color(125, 211, 252, 135);
                case ZOMBIE -> new Color(74, 222, 128, 125);
                case BOSS -> new Color(251, 146, 60, 155);
            };
            int step = Math.max(18, w / 28);
            for (int bx = 8, idx = 0; bx < w - 12; bx += step, idx++) {
                int bw = step - 3;
                int bh = 10 + ((idx * 17 + (world == CityWorldState.BOSS ? 7 : 3)) % 22);
                int by = baseLine - bh;
                g2.setColor(silCol);
                if (world == CityWorldState.NORMAL) {
                    g2.fillRect(bx, by, bw, bh + 6);
                } else if (world == CityWorldState.ZOMBIE) {
                    // Broken jagged tops for infected ruins
                    Polygon ruin = new Polygon(
                            new int[]{bx, bx + bw / 2, bx + bw, bx + bw, bx},
                            new int[]{by + (idx % 2) * 5, by + ((idx + 1) % 3) * 4, by + 2, baseLine + 6, baseLine + 6},
                            5);
                    g2.fillPolygon(ruin);
                } else {
                    // Fractured volcanic spires & cleaved skyscrapers
                    Polygon crag = new Polygon(
                            new int[]{bx, bx + bw / 3, bx + bw, bx + bw, bx},
                            new int[]{by + 3, Math.max(12, by - 4), by + 6, baseLine + 6, baseLine + 6},
                            5);
                    g2.fillPolygon(crag);
                }
                if (bh > 15 && idx % 2 == 0) {
                    g2.setColor(winCol);
                    g2.fillRect(bx + 3, by + 5, Math.max(2, bw - 8), 2);
                    if (bh > 22) g2.fillRect(bx + 3, by + 10, Math.max(2, bw - 8), 2);
                }
            }

            // Animated sea horizon ripples behind island slab
            Color surfCol = switch (world) {
                case NORMAL -> new Color(56, 189, 248, 42);
                case ZOMBIE -> new Color(34, 197, 94, 46);
                case BOSS -> new Color(239, 68, 68, 52);
            };
            g2.setColor(surfCol);
            for (int s = 0; s < 5; s++) {
                int wy = h - 18 + (s % 2) * 5;
                int sx = (int) ((s * w / 5.0 + Math.sin(t * 1.4 + s) * 14) % w);
                g2.fillRoundRect(sx - 30, wy, 72, 3, 3, 3);
            }
        }

        private void draw25DCityIslandDeck(Graphics2D g2, int w, int h, double t, CityWorldState world, Color chapAcc) {
            int topY = 62;
            int botY = h - 20;
            int cliffDepth = 14;
            int topInset = (int) Math.round(w * 0.055);
            int botInset = (int) Math.round(w * 0.015);
            int midY = (topY + botY) / 2;

            // 8-point sculpted 2.5D peninsula polygon
            int[] xs = {topInset + 18, w - topInset - 18, w - botInset - 14, w - botInset, w - botInset - 18, botInset + 18, botInset, botInset + 14};
            int[] ys = {topY, topY, midY - 18, botY - 10, botY, botY, botY - 10, midY - 18};
            Polygon deck = new Polygon(xs, ys, 8);

            // Extruded 3D vertical cliff rim below foreground edge (y = botY .. botY + cliffDepth)
            Polygon cliff = new Polygon(
                    new int[]{botInset, botInset + 18, w - botInset - 18, w - botInset, w - botInset - 10, botInset + 10},
                    new int[]{botY - 10, botY, botY, botY - 10, botY + cliffDepth - 4, botY + cliffDepth - 4},
                    6);
            Color cliffTopCol = switch (world) {
                case NORMAL -> new Color(26, 48, 76);
                case ZOMBIE -> new Color(18, 42, 30);
                case BOSS -> new Color(46, 20, 22);
            };
            Color cliffBotCol = switch (world) {
                case NORMAL -> new Color(11, 22, 38);
                case ZOMBIE -> new Color(8, 22, 14);
                case BOSS -> new Color(20, 8, 10);
            };
            g2.setPaint(new GradientPaint(0, botY - 6, cliffTopCol, 0, botY + cliffDepth, cliffBotCol));
            g2.fillPolygon(cliff);

            // Deck pavement fill with 2.5D atmospheric surface tint
            Color deckNorth = switch (world) {
                case NORMAL -> new Color(17, 36, 62, 238);
                case ZOMBIE -> new Color(14, 34, 24, 240);
                case BOSS -> new Color(32, 16, 20, 244);
            };
            Color deckSouth = switch (world) {
                case NORMAL -> new Color(23, 48, 82, 245);
                case ZOMBIE -> new Color(18, 46, 30, 246);
                case BOSS -> new Color(44, 18, 18, 248);
            };
            g2.setPaint(new GradientPaint(0, topY, deckNorth, 0, botY, deckSouth));
            g2.fillPolygon(deck);

            // Clip inner terrain for perspective isometric city avenues, canals, & cracks
            java.awt.Shape oldClip = g2.getClip();
            g2.setClip(deck);

            // Vanishing-point perspective isometric avenue blocks
            Color blockStroke = switch (world) {
                case NORMAL -> new Color(chapAcc.getRed(), chapAcc.getGreen(), chapAcc.getBlue(), 42);
                case ZOMBIE -> new Color(34, 197, 94, 46);
                case BOSS -> new Color(249, 115, 22, 48);
            };
            g2.setColor(blockStroke);
            g2.setStroke(new BasicStroke(1.2f));
            for (int k = -4; k <= 4; k++) {
                int xTop = w / 2 + k * Math.max(46, w / 11);
                int xBot = w / 2 + (int) Math.round(k * Math.max(66, w / 8.0));
                g2.drawLine(xTop, topY, xBot, botY);
            }
            for (int r = 1; r <= 4; r++) {
                int gy = topY + (int) Math.round((botY - topY) * (r * 0.22));
                g2.drawLine(0, gy, w, gy);
            }

            // Central Scenic Waterway / Toxic Effluent Canal / Molten Lava Rift winding across center city
            Path2D.Double canal = new Path2D.Double();
            double cY1 = topY + (botY - topY) * 0.44;
            double cY2 = topY + (botY - topY) * 0.56;
            canal.moveTo(0, cY1 + 14);
            canal.curveTo(w * 0.30, cY1 - 12, w * 0.65, cY2 + 12, w, cY2 - 14);
            Color canalOuter = switch (world) {
                case NORMAL -> new Color(14, 90, 152, 130);
                case ZOMBIE -> new Color(22, 163, 74, 145);
                case BOSS -> new Color(220, 38, 38, 165);
            };
            Color canalCore = switch (world) {
                case NORMAL -> new Color(56, 189, 248, 130);
                case ZOMBIE -> new Color(132, 248, 86, 140);
                case BOSS -> new Color(251, 146, 60, 185);
            };
            g2.setColor(canalOuter);
            g2.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(canal);
            g2.setColor(canalCore);
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(canal);

            // State-specific surface particles: Green bio-slime puddles & hazard lines (Zombie) OR Magma Cracks (Boss)
            if (world == CityWorldState.ZOMBIE) {
                g2.setColor(new Color(74, 222, 128, 58));
                for (int p = 0; p < 7; p++) {
                    int px = (int) (w * (0.14 + p * 0.12));
                    int py = topY + 34 + (p % 3) * ((botY - topY - 60) / 3);
                    g2.fillOval(px - 18, py - 7, 36 + (p % 2) * 14, 13);
                    // Rising toxic bio-spore motes
                    float drift = (float) ((t * 16 + p * 9) % 28);
                    g2.setColor(new Color(163, 255, 78, 145));
                    g2.fillOval(px - 4 + (int) (Math.sin(t * 2 + p) * 5), (int) (py - drift), 4, 4);
                    g2.setColor(new Color(74, 222, 128, 58));
                }
            } else if (world == CityWorldState.BOSS) {
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int f = 0; f < 5; f++) {
                    int fx = (int) (w * (0.18 + f * 0.16));
                    int fy = topY + 42 + (f % 2) * 110;
                    g2.setColor(new Color(249, 115, 22, 165));
                    g2.drawLine(fx - 26, fy - 10, fx + 4, fy + 6);
                    g2.drawLine(fx + 4, fy + 6, fx + 28, fy - 6);
                    g2.setColor(new Color(253, 224, 71, 185));
                    float sparkY = (float) (fy - ((t * 24 + f * 11) % 30));
                    g2.fillOval(fx + (int) (Math.cos(t * 3 + f) * 6), (int) sparkY, 4, 4);
                }
            } else {
                // Normal City sunlit park terraces & harbor dock lights
                g2.setColor(new Color(34, 197, 94, 44));
                for (int p = 0; p < 4; p++) {
                    int px = (int) (w * (0.22 + p * 0.18));
                    int py = topY + 38 + (p % 2) * 95;
                    g2.fillRoundRect(px - 20, py - 8, 40, 16, 10, 10);
                }
            }
            g2.setClip(oldClip);

            // Crisp beveled shoreline rim around peninsula deck
            Color rimColor = switch (world) {
                case NORMAL -> new Color(chapAcc.getRed(), chapAcc.getGreen(), chapAcc.getBlue(), 165);
                case ZOMBIE -> new Color(74, 222, 128, 178);
                case BOSS -> new Color(251, 146, 60, 190);
            };
            g2.setColor(rimColor);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawPolygon(deck);
        }

        private void draw25DCityLandmarks(Graphics2D g2, int w, int h, double t, CityWorldState world, Color chapAcc) {
            if (stageTiles.size() < 20) return;
            // Compute vertical corridor centers strictly in open gaps between row 0..3 pedestals
            int yRow0 = stageTiles.get(0).getY();
            int yRow1 = stageTiles.get(5).getY();
            int yRow2 = stageTiles.get(10).getY();
            int yRow3 = stageTiles.get(15).getY();
            int tH = stageTiles.get(0).getHeight();

            int gap01 = (yRow0 + tH + yRow1) / 2;
            int gap12 = (yRow1 + tH + yRow2) / 2;
            int gap23 = (yRow2 + tH + yRow3) / 2;
            int northVistaY = Math.max(58, yRow0 + 6);

            String hubKey = switch (world) {
                case NORMAL -> "city_normal_hub";
                case ZOMBIE -> "city_zombie_hub";
                case BOSS -> "city_boss_citadel";
            };
            String blockKey = switch (world) {
                case NORMAL -> "city_normal_block";
                case ZOMBIE -> "city_zombie_block";
                case BOSS -> "city_boss_ruins";
            };
            String outpostKey = switch (world) {
                case NORMAL -> "city_normal_fort";
                case ZOMBIE -> "city_zombie_nest";
                case BOSS -> "city_boss_altar";
            };
            Color padAura = switch (world) {
                case NORMAL -> new Color(chapAcc.getRed(), chapAcc.getGreen(), chapAcc.getBlue(), 75);
                case ZOMBIE -> new Color(34, 197, 94, 96);
                case BOSS -> new Color(249, 115, 22, 110);
            };

            int hubSz = Math.min(88, Math.max(64, (int) (w * 0.130)));
            int subSz = Math.min(70, Math.max(52, (int) (w * 0.102)));

            // Back-to-front depth order: Northern Harbor/Citadel skyline vista -> Gap01 -> Gap12 -> Gap23
            drawCityBuildingAnchored(g2, blockKey, (int) (w * 0.24), northVistaY, subSz - 4, padAura);
            drawCityBuildingAnchored(g2, hubKey, (int) (w * 0.50), northVistaY - 2, hubSz - 6, padAura);
            drawCityBuildingAnchored(g2, outpostKey, (int) (w * 0.74), northVistaY, subSz - 2, padAura);

            drawCityBuildingAnchored(g2, blockKey, (int) (w * 0.30), gap01, subSz, padAura);
            drawCityBuildingAnchored(g2, outpostKey, (int) (w * 0.70), gap01, subSz, padAura);

            drawCityBuildingAnchored(g2, hubKey, (int) (w * 0.50), gap12, hubSz - 2, padAura);
            drawCityBuildingAnchored(g2, blockKey, (int) (w * 0.27), gap12 + 2, subSz - 4, padAura);
            drawCityBuildingAnchored(g2, outpostKey, (int) (w * 0.73), gap12 + 2, subSz - 4, padAura);

            drawCityBuildingAnchored(g2, blockKey, (int) (w * 0.30), gap23, subSz, padAura);
            drawCityBuildingAnchored(g2, hubKey, (int) (w * 0.70), gap23, subSz + 4, padAura);

            // Dynamic Aerial Patrol & Boss Avatar flanking Upper-Right Open Skyline
            if (world == CityWorldState.ZOMBIE) {
                int frame = (int) ((t * 7.0) % SpriteAssets.dragonFrameCount());
                BufferedImage zDragon = SpriteAssets.getDragonFrame(SpriteAssets.DragonSpecies.ZOMBIE, frame);
                if (zDragon != null) {
                    int dx = (int) (w * 0.78 + Math.sin(t * 0.9) * 26);
                    int dy = 10 + (int) (Math.cos(t * 1.8) * 3);
                    SpriteAssets.drawSharp(g2, zDragon, dx, dy, 48, 38);
                }
            } else if (world == CityWorldState.BOSS && selectedStage != null) {
                TitanMode.TitanBoss boss = selectedStage.boss() != null ? selectedStage.boss() : TitanMode.TitanBoss.GODZILLA;
                BufferedImage bossImg = SpriteAssets.getTitanSprite(boss, false, true);
                if (bossImg != null) {
                    int bh = 56;
                    int bw = Math.min(76, (int) Math.round(bh * (bossImg.getWidth() / (double) Math.max(1, bossImg.getHeight()))));
                    int bob = (int) Math.round(Math.sin(t * 2.6) * 2.5);
                    SpriteAssets.drawSharp(g2, bossImg, w - bw - 14, 8 + bob, bw, bh);
                }
            }
        }

        private void drawCityBuildingAnchored(Graphics2D g2, String key, int cx, int cy, int size, Color aura) {
            BufferedImage img = SpriteAssets.getMapProp(key);
            if (img == null) return;
            // Subtle isometric ground shadow/glow oval beneath building
            g2.setColor(new Color(0, 0, 0, 110));
            g2.fillOval(cx - size / 2 + 4, cy - (int) (size * 0.20) + 4, size - 6, (int) (size * 0.36));
            g2.setColor(aura);
            g2.fillOval(cx - size / 2 + 2, cy - (int) (size * 0.22), size - 4, (int) (size * 0.36));
            // Anchor building bottom near cy so rooftops rise upward cleanly in 2.5D depth
            SpriteAssets.drawSharp(g2, img, cx - size / 2, cy - (int) (size * 0.66), size, size);
        }

        private void draw25DRouteHighways(Graphics2D g2, double t, CityWorldState world, Color chapAcc) {
            List<Point> anchors = new ArrayList<>(stageTiles.size());
            for (StageNodeTile tile : stageTiles) {
                // Anchor road right under each stage's 2.5D pedestal center
                anchors.add(new Point(tile.getX() + tile.getWidth() / 2, tile.getY() + (int) (tile.getHeight() * 0.64)));
            }
            for (int i = 0; i < anchors.size() - 1; i++) {
                Point a = anchors.get(i);
                Point b = anchors.get(i + 1);
                StageNodeTile tA = stageTiles.get(i);
                StageNodeTile tB = stageTiles.get(i + 1);
                boolean lit = tA.stars > 0 || (tA.unlocked && tB.unlocked);
                boolean bridgeTurn = (i % 5 == 4);

                // 2.5D Road Shadow + Tarmac/Bridge Deck
                g2.setColor(new Color(4, 8, 16, 150));
                g2.setStroke(new BasicStroke(bridgeTurn ? 13f : 11f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(a.x, a.y + 4, b.x, b.y + 4);

                Color roadDeck = bridgeTurn
                        ? (world == CityWorldState.BOSS ? new Color(82, 34, 24, 225)
                        : (world == CityWorldState.ZOMBIE ? new Color(28, 62, 42, 225) : new Color(38, 66, 102, 225)))
                        : (world == CityWorldState.BOSS ? new Color(46, 22, 24, 215)
                        : (world == CityWorldState.ZOMBIE ? new Color(22, 40, 30, 215) : new Color(26, 44, 72, 215)));
                g2.setColor(roadDeck);
                g2.setStroke(new BasicStroke(bridgeTurn ? 9.5f : 7.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(a.x, a.y, b.x, b.y);

                // Illuminated animated convoy dash line
                Color linkColor = tA.stars > 0
                        ? new Color(251, 191, 36, 215)
                        : (lit ? new Color(world.accent.getRed(), world.accent.getGreen(), world.accent.getBlue(), 195)
                        : new Color(71, 85, 105, 105));
                g2.setColor(linkColor);
                float dashPhase = lit ? Math.max(0f, 24f - (float) ((Math.abs(t) * 28.0) % 24.0)) : 0f;
                g2.setStroke(new BasicStroke(lit ? 2.6f : 1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        0f, new float[]{7f, 6f}, dashPhase));
                g2.drawLine(a.x, a.y, b.x, b.y);
            }
        }

        private void draw25DWorldHudBanner(Graphics2D g2, int w, CityWorldState world, Color chapAcc) {
            String bossSuffix = (world == CityWorldState.BOSS && selectedStage != null && selectedStage.boss() != null)
                    ? (" · " + selectedStage.boss().shortLabel.toUpperCase())
                    : "";
            String title = "BẢN ĐỒ 2.5D · " + world.bannerLabel + bossSuffix;
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int pillW = Math.min(w - 24, fm.stringWidth(title) + 24);
            g2.setColor(new Color(8, 14, 26, 206));
            g2.fillRoundRect(10, 8, pillW, 20, 10, 10);
            g2.setColor(new Color(world.accent.getRed(), world.accent.getGreen(), world.accent.getBlue(), 190));
            g2.setStroke(new BasicStroke(1.1f));
            g2.drawRoundRect(10, 8, pillW, 20, 10, 10);
            g2.fillOval(17, 15, 6, 6);
            g2.setColor(Color.WHITE);
            g2.drawString(title, 28, 22);
        }
    }

    private final class StageNodeTile extends JComponent {
        private final int indexInChapter;
        private CampaignMode.CampaignStage stage;
        private boolean selected;
        private boolean filterMatch = true;
        private boolean hovered;
        private boolean unlocked = true;
        private int stars;

        private StageNodeTile(int indexInChapter) {
            this.indexInChapter = indexInChapter;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    if (stage != null) {
                        SoundEngine.playUiClick();
                        selectStage(stage.stageNumber());
                    }
                }
            });
        }

        private void bind(CampaignMode.CampaignStage stage, boolean selected, boolean filterMatch) {
            this.stage = stage;
            this.selected = selected;
            this.filterMatch = filterMatch;
            this.unlocked = CampaignMode.isUnlocked(stage.stageNumber());
            this.stars = CampaignMode.getStars(stage.stageNumber());
            setToolTipText("Màn " + stage.stageNumber() + ": " + stage.title() + " (" + stage.ruleType().shortLabel + ") · ★" + stars + "/3");
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            if (stage == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            double t = OceanMapBackdrop.elapsedSeconds();
            Color acc = stage.ruleType().accent;

            Composite oldComp = g2.getComposite();
            if (!filterMatch) {
                g2.setComposite(AlphaComposite.SrcOver.derive(0.32f));
            } else if (!unlocked) {
                g2.setComposite(AlphaComposite.SrcOver.derive(0.62f));
            }

            // 2.5D Isometric Pedestal Anchored on the City Avenue
            int pedW = Math.min(w - 8, 66);
            int pedH = 22;
            int px = (w - pedW) / 2;
            int py = h - pedH - 12;
            int lift = selected ? -3 : (hovered ? -2 : 0);

            // Ground shadow
            g2.setColor(new Color(0, 0, 0, 125));
            g2.fillOval(px + 2, py + 7, pedW - 2, pedH);

            // Extruded 3D pedestal skirt (5px thickness)
            Color rimDark = stage.isBossStage() ? new Color(92, 26, 20)
                    : (stage.ruleType() == CampaignMode.StageRuleType.ZOMBIE_ASSAULT ? new Color(18, 68, 38)
                    : new Color(24, 44, 72));
            g2.setColor(rimDark);
            g2.fillOval(px, py + 4, pedW, pedH);

            // Top isometric cap of stage pin pedestal
            Color capTop = selected ? acc
                    : (stage.isBossStage() ? new Color(128, 34, 24)
                    : (stage.ruleType() == CampaignMode.StageRuleType.ZOMBIE_ASSAULT ? new Color(24, 92, 48)
                    : (hovered ? new Color(36, 72, 114) : new Color(20, 42, 74))));
            g2.setPaint(new GradientPaint(0, py, capTop, 0, py + pedH, rimDark));
            g2.fillOval(px, py, pedW, pedH);
            g2.setColor(selected ? Color.WHITE
                    : (stars == 3 ? new Color(251, 191, 36, 225) : new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 210)));
            g2.setStroke(new BasicStroke(selected ? 2.0f : 1.3f));
            g2.drawOval(px, py, pedW, pedH);

            // Vertical selection pulse halo
            if (selected) {
                int pulse = 3 + (int) Math.round(Math.sin(t * 4.2) * 2.0);
                g2.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 85));
                g2.drawOval(px - pulse, py - pulse / 2, pedW + pulse * 2, pedH + pulse);
            }

            // Center 3D node sprite rising from the pedestal
            int spriteSize = Math.min(34, Math.min(w - 22, h - 30));
            int sx = (w - spriteSize) / 2;
            int sy = Math.max(12, py - spriteSize + 15 + lift);
            BufferedImage nodeImg = SpriteAssets.getMapProp(stage.ruleType().spriteKey);
            if (nodeImg != null) {
                SpriteAssets.drawSharp(g2, nodeImg, sx, sy, spriteSize, spriteSize);
            }

            // Floating compact header pill at top of pin: Stage Number + Mode Tag
            String numStr = (stage.isBossStage() ? "★" : "") + stage.stageNumber();
            String tag = switch (stage.ruleType()) {
                case SKIRMISH -> "HẢI";
                case SNIPER_AMMO -> "ĐẠN";
                case BLITZ_TIMED -> "TỐC";
                case SONAR_FOG -> "RDR";
                case ZOMBIE_ASSAULT -> "ZOM";
                case TITAN_MILESTONE -> "BOSS";
            };
            String pill = numStr + "·" + tag;
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            FontMetrics pfm = g2.getFontMetrics();
            int pillW = Math.min(w - 4, pfm.stringWidth(pill) + 10);
            int pillX = (w - pillW) / 2;
            g2.setColor(selected ? new Color(10, 18, 34, 240) : new Color(11, 19, 34, 218));
            g2.fillRoundRect(pillX, 1, pillW, 15, 7, 7);
            g2.setColor(selected ? Color.WHITE : acc);
            g2.setStroke(new BasicStroke(selected ? 1.3f : 0.9f));
            g2.drawRoundRect(pillX, 1, pillW - 1, 14, 7, 7);
            g2.setColor(selected ? Color.WHITE : (stage.isBossStage() ? new Color(0xFDBA74) : UiTokens.TEXT_PRIMARY));
            g2.drawString(pill, pillX + (pillW - pfm.stringWidth(pill)) / 2, 12);

            if (!unlocked) {
                g2.setColor(new Color(9, 13, 24, 210));
                g2.fillRoundRect(w / 2 - 17, sy + spriteSize / 2 - 7, 34, 14, 6, 6);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
                g2.setColor(new Color(0xF87171));
                g2.drawString("KHÓA", w / 2 - 12, sy + spriteSize / 2 + 3);
            }

            // Bottom row: 3 crisp Blender 3D stars tucked cleanly on a dark pill badge along the pedestal apron
            BufferedImage starGold = SpriteAssets.getMapProp("stage_star_gold");
            BufferedImage starEmpty = SpriteAssets.getMapProp("stage_star_empty");
            int starSz = 12;
            int starsW = starSz * 3 + 4;
            int starX0 = (w - starsW) / 2;
            int starY = h - starSz - 1;
            g2.setColor(new Color(6, 10, 18, 195));
            g2.fillRoundRect(starX0 - 4, starY - 1, starsW + 8, starSz + 2, 8, 8);
            for (int s = 0; s < 3; s++) {
                BufferedImage img = (s < stars) ? starGold : starEmpty;
                if (img != null) {
                    SpriteAssets.drawSharp(g2, img, starX0 + s * (starSz + 2), starY, starSz, starSz);
                }
            }

            g2.setComposite(oldComp);
            g2.dispose();
        }
    }

    private final class StageDioramaCanvas extends JComponent {
        private StageDioramaCanvas() { setOpaque(false); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            double t = OceanMapBackdrop.elapsedSeconds();
            CampaignMode.CampaignStage st = selectedStage;
            CityWorldState world = getCityWorldState();
            Color acc = st.ruleType().accent;
            Color chapAcc = st.chapter().accent;

            g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, 10, 10));
            Color bgTop = switch (world) {
                case NORMAL -> new Color(8, 22, 42);
                case ZOMBIE -> new Color(8, 28, 19);
                case BOSS -> new Color(38, 10, 14);
            };
            g2.setPaint(new GradientPaint(0, 0, bgTop,
                    0, h, new Color(Math.max(8, chapAcc.getRed() / 5), Math.max(14, chapAcc.getGreen() / 5), Math.max(26, chapAcc.getBlue() / 4))));
            g2.fillRect(0, 0, w, h);

            // Background 2.5D City Horizon Landmark matching stage world state (Normal vs Zombie vs Boss)
            String cityKey = switch (world) {
                case NORMAL -> "city_normal_hub";
                case ZOMBIE -> "city_zombie_hub";
                case BOSS -> "city_boss_citadel";
            };
            BufferedImage cityImg = SpriteAssets.getMapProp(cityKey);
            if (cityImg != null) {
                Composite old = g2.getComposite();
                g2.setComposite(AlphaComposite.SrcOver.derive(0.46f));
                SpriteAssets.drawSharp(g2, cityImg, w / 2 - 48, 4, 96, 96);
                g2.setComposite(old);
            }

            // Animated sonar rings + horizon waves inside diorama card
            g2.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 42));
            for (int row = 0; row < 4; row++) {
                int wy = (int) (h * 0.48) + row * 16;
                int shift = (int) Math.round(Math.sin(t * 2.2 + row) * 6);
                g2.drawArc(-20 + shift, wy, w + 40, 26, 0, 180);
            }

            g2.setPaint(new RadialGradientPaint(w * 0.5f, h * 0.52f, Math.max(24f, h * 0.55f),
                    new float[]{0f, 1f},
                    new Color[]{new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 78), new Color(0, 0, 0, 0)}));
            g2.fillRect(0, 0, w, h);

            int bob = (int) (Math.sin(t * 2.5) * 4);
            if (st.isBossStage()) {
                BufferedImage superShip = SpriteAssets.getSuperShipSprite(true);
                if (superShip != null) SpriteAssets.drawSharp(g2, superShip, 14, h / 2 - 12 - bob / 2, 106, 30);
                BufferedImage bossImg = SpriteAssets.getTitanSprite(st.boss(), false, true);
                if (bossImg != null) {
                    double ratio = bossImg.getWidth() / (double) Math.max(1, bossImg.getHeight());
                    int bh = Math.min(108, h - 22);
                    int bw = Math.min(138, (int) Math.round(bh * ratio));
                    SpriteAssets.drawSharp(g2, bossImg, w - bw - 14, (h - bh) / 2 + bob, bw, bh);
                }
            } else if (st.ruleType() == CampaignMode.StageRuleType.ZOMBIE_ASSAULT) {
                BufferedImage ally = SpriteAssets.getShipSprite(ShipType.BATTLESHIP, true);
                if (ally != null) SpriteAssets.drawSharp(g2, ally, 16, h / 2 + 10 - bob / 2, 104, 30);
                for (int i = 0; i < Math.min(3, st.enemyFleetTypes().size()); i++) {
                    BufferedImage zi = SpriteAssets.getZombieShipSprite(st.enemyFleetTypes().get(i), true);
                    if (zi != null) SpriteAssets.drawSharp(g2, zi, w - 126 - (i % 2) * 28, 22 + i * 32 + bob / 2, 108, 30);
                }
            } else {
                BufferedImage crest = SpriteAssets.getMapProp(st.ruleType().spriteKey);
                if (crest != null) SpriteAssets.drawSharp(g2, crest, 16, (h - 58) / 2 + bob, 58, 58);
                for (int i = 0; i < Math.min(3, st.enemyFleetTypes().size()); i++) {
                    BufferedImage si = SpriteAssets.getShipSprite(st.enemyFleetTypes().get(i), true);
                    if (si != null) SpriteAssets.drawSharp(g2, si, w - 134 - (i % 2) * 32, 22 + i * 32 - bob / 2, 112, 32);
                }
            }

            g2.setClip(null);
            g2.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 155));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
            g2.dispose();
        }
    }

    private final class BestStarsBar extends JComponent {
        private BestStarsBar() { setOpaque(false); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stars = CampaignMode.getStars(selectedStage.stageNumber());
            int best = CampaignMode.getBestScore(selectedStage.stageNumber());

            BufferedImage starGold = SpriteAssets.getMapProp("stage_star_gold");
            BufferedImage starEmpty = SpriteAssets.getMapProp("stage_star_empty");
            for (int i = 0; i < 3; i++) {
                BufferedImage img = (i < stars) ? starGold : starEmpty;
                if (img != null) SpriteAssets.drawSharp(g2, img, i * 22, 2, 20, 20);
            }
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            g2.setColor(stars > 0 ? new Color(0xFDE68A) : UiTokens.TEXT_MUTED);
            String text = stars > 0 ? ("Kỷ lục: " + best + " điểm (★" + stars + "/3)") : "Chưa chinh phục ải này";
            g2.drawString(text, 74, 17);
            g2.dispose();
        }
    }

    private static final class ChapterTabButton extends JButton {
        private final CampaignMode.CampaignChapter chapter;
        private boolean selected;

        private ChapterTabButton(CampaignMode.CampaignChapter chapter) {
            this.chapter = chapter;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(chapter.fullTitle + " (" + chapter.startStage + "–" + chapter.endStage + ")");
        }

        private void sync(boolean selected) {
            this.selected = selected;
            setText(chapter.shortTab + " · ★" + CampaignMode.chapterStars(chapter.index) + "/60");
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            boolean hover = getModel().isRollover();
            Color acc = chapter.accent;
            g2.setColor(selected
                    ? new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 66)
                    : (hover ? new Color(25, 42, 68, 224) : new Color(13, 24, 42, 210)));
            g2.fillRoundRect(1, 1, w - 2, h - 2, 9, 9);
            g2.setColor(selected ? acc : (hover ? new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 160) : new Color(56, 76, 108, 150)));
            g2.setStroke(new BasicStroke(selected ? 1.8f : 1.0f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 9, 9);

            g2.setColor(acc);
            g2.fillOval(8, h / 2 - 3, 6, 6);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g2.setColor(selected ? Color.WHITE : UiTokens.TEXT_MUTED);
            FontMetrics fm = g2.getFontMetrics();
            String txt = getText();
            int tx = 18 + Math.max(0, (w - 22 - fm.stringWidth(txt)) / 2);
            g2.drawString(txt, tx, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private static final class FilterChipButton extends JButton {
        private final StageFilter filter;
        private boolean selected;

        private FilterChipButton(StageFilter filter) {
            super(filter.label);
            this.filter = filter;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            FontMetrics fm = getFontMetrics(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            setPreferredSize(new Dimension(fm.stringWidth(filter.label) + 18, 26));
        }

        private void sync(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            boolean hover = getModel().isRollover();
            Color acc = switch (filter) {
                case ALL -> new Color(0x38BDF8);
                case MINIGAME -> new Color(0xFACC15);
                case ZOMBIE -> new Color(0x22C55E);
                case BOSS -> new Color(0xF97316);
                case UNPERFECT -> new Color(0xFBBF24);
            };
            g2.setColor(selected
                    ? new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 62)
                    : (hover ? new Color(25, 42, 68, 224) : new Color(13, 24, 42, 206)));
            g2.fillRoundRect(1, 1, w - 2, h - 2, 8, 8);
            g2.setColor(selected ? acc : new Color(56, 76, 108, 150));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 8, 8);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g2.setColor(selected ? Color.WHITE : UiTokens.TEXT_MUTED);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(filter.label, (w - fm.stringWidth(filter.label)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private static final class UnlockToggleButton extends JButton {
        private UnlockToggleButton() {
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Bật/tắt chế độ mở hết 100 màn hoặc mở khóa tuần tự từng ải");
            sync();
        }

        private void sync() {
            boolean seq = CampaignMode.isSequentialUnlockOnly();
            setText(seq ? "Mở Tuần Tự" : "Mở Hết 1–100");
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean seq = CampaignMode.isSequentialUnlockOnly();
            Color acc = seq ? new Color(0xF97316) : new Color(0x34D399);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 42));
            g2.fillRoundRect(1, 1, w - 2, h - 2, 8, 8);
            g2.setColor(acc);
            g2.drawRoundRect(1, 1, w - 3, h - 3, 8, 8);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private static final class SmallActionButton extends JButton {
        private SmallActionButton(String text) {
            super(text);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(getModel().isRollover() ? new Color(56, 189, 248, 95) : new Color(56, 189, 248, 52));
            g2.fillRoundRect(1, 1, w - 2, h - 2, 7, 7);
            g2.setColor(UiTokens.ACCENT_SIGNAL);
            g2.drawRoundRect(1, 1, w - 3, h - 3, 7, 7);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private final class StoryTeaserCard extends JComponent {
        private boolean hover;

        private StoryTeaserCard() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Bấm để mở khung truyện tranh hoạt hình Cốt Truyện cho Màn này");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    SoundEngine.playUiClick();
                    if (selectedStage != null) {
                        openStageStoryComic(selectedStage.stageNumber());
                    }
                }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            CampaignMode.CampaignStage st = selectedStage != null ? selectedStage : CampaignMode.stage(1);
            StoryMode.StoryEpisode ep = StoryMode.introEpisodeForStage(st);
            StoryMode.StoryFrame lead = ep.leadFrame();
            if (lead == null) return;

            int w = getWidth();
            int h = getHeight();
            if (w <= 20 || h <= 20) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            Color spCol = lead.speaker().accent;
            g2.setColor(hover ? new Color(22, 38, 66, 235) : new Color(15, 27, 49, 212));
            g2.fillRoundRect(0, 0, w, h, 10, 10);
            g2.setColor(hover ? Color.WHITE : new Color(spCol.getRed(), spCol.getGreen(), spCol.getBlue(), 165));
            g2.setStroke(new BasicStroke(hover ? 1.5f : 1.1f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

            int pSz = Math.min(40, h - 14);
            int px = 8;
            int py = (h - pSz) / 2;
            java.awt.Shape oldClip = g2.getClip();
            g2.setClip(new Ellipse2D.Double(px, py, pSz, pSz));
            BufferedImage portrait = lead.speaker().portraitKey != null
                    ? SpriteAssets.getStoryArt(lead.speaker().portraitKey) : null;
            if (portrait != null) {
                g2.drawImage(portrait, px, py, pSz, pSz, null);
            } else {
                BufferedImage art = SpriteAssets.getStoryArt(lead.artKey());
                if (art != null) {
                    g2.drawImage(art, px, py, pSz, pSz, null);
                } else {
                    g2.setColor(new Color(24, 40, 72));
                    g2.fillRect(px, py, pSz, pSz);
                }
            }
            g2.setClip(oldClip);
            g2.setColor(spCol);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawOval(px, py, pSz, pSz);

            int textX = px + pSz + 10;
            String readPill = StoryMode.hasAutoIntroCutscene(st.stageNumber()) ? "TRUYỆN ★" : "ĐỌC ▸";
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            int pillW = g2.getFontMetrics().stringWidth(readPill) + 14;
            int pillX = w - pillW - 8;
            g2.setColor(new Color(spCol.getRed(), spCol.getGreen(), spCol.getBlue(), 52));
            g2.fillRoundRect(pillX, 8, pillW, 18, 9, 9);
            g2.setColor(spCol);
            g2.drawRoundRect(pillX, 8, pillW - 1, 18 - 1, 9, 9);
            g2.drawString(readPill, pillX + 7, 21);

            int textMaxW = Math.max(60, pillX - textX - 6);
            g2.setColor(spCol);
            String hdr = fitTeaserText(lead.speaker().displayName + " · " + ep.chapterTag(), g2.getFontMetrics(), textMaxW);
            g2.drawString(hdr, textX, 19);

            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            g2.setColor(UiTokens.TEXT_PRIMARY);
            String line = fitTeaserText("\"" + lead.dialogue() + "\"", g2.getFontMetrics(), Math.max(80, w - textX - 10));
            g2.drawString(line, textX, 39);
            g2.dispose();
        }

        private String fitTeaserText(String txt, FontMetrics fm, int maxW) {
            if (txt == null || maxW <= 10) return "";
            if (fm.stringWidth(txt) <= maxW) return txt;
            int end = txt.length();
            while (end > 1 && fm.stringWidth(txt.substring(0, end) + "…") > maxW) end--;
            return txt.substring(0, Math.max(0, end)) + "…";
        }
    }
}
