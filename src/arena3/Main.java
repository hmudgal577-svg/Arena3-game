package arena3;

import javax.swing.*;
import arena3.core.GameLauncher;
import arena3.ui.ThemeConstants;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ARENA 3");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(ThemeConstants.WINDOW_WIDTH, ThemeConstants.WINDOW_HEIGHT);
            frame.setMinimumSize(new java.awt.Dimension(900, 650));
            frame.setLocationRelativeTo(null);
            frame.setBackground(ThemeConstants.BG_DARK);

            GameLauncher launcher = new GameLauncher(frame);
            frame.setContentPane(launcher);
            frame.setVisible(true);
        });
    }
}
