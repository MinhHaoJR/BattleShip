package battleship.ui;

import javax.swing.JFrame;

/** Cửa sổ chính. TODO (UI): chuyển giữa SetupPanel (đặt tàu) và GamePanel (bắn). */
public class MainFrame extends JFrame {
    public MainFrame() {
        super("BattleShip");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(null);
    }
}
