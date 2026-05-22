package arena3.menu;

import javax.swing.*;
import java.awt.*;
import arena3.core.GameLauncher;
import arena3.core.GameState;
import arena3.core.GameState.*;
import arena3.ui.ThemeConstants;

public class PlayerSetupScreen extends JPanel {
    private GameType gameType;
    private final CardLayout cardLayout;
    private final JPanel cardsPanel;
    private GameConfig config;

    // Snake options
    private JComboBox<String> snakePlayers, snakeDifficulty, snakeBoardSize, snakeSpeed;
    // Ludo options
    private JComboBox<String> ludoPlayers, ludoDifficulty;
    private JToggleButton[] ludoAIToggles = new JToggleButton[4];
    // Chess options
    private JComboBox<String> chessPlayers, chessDifficulty, chessSide, chessTimer;
    private JCheckBox chessHints;

    public PlayerSetupScreen() {
        setLayout(new BorderLayout());
        setBackground(ThemeConstants.BG_DARK);

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);

        cardsPanel.add(createSnakeSetup(), "SNAKE");
        cardsPanel.add(createLudoSetup(), "LUDO");
        cardsPanel.add(createChessSetup(), "CHESS");

        add(createTopBar(), BorderLayout.NORTH);
        add(cardsPanel, BorderLayout.CENTER);
        add(createBottomBar(), BorderLayout.SOUTH);
    }

    public void configure(GameType type) {
        this.gameType = type;
        config = new GameConfig();
        config.gameType = type;
        cardLayout.show(cardsPanel, type.name());
    }

    private JPanel createTopBar() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setColor(ThemeConstants.BG_LIGHTER);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ThemeConstants.NEON_GOLD);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
                ThemeConstants.enableAntialiasing(g2);
                g2.setFont(ThemeConstants.FONT_HEADING);
                g2.setColor(ThemeConstants.TEXT_PRIMARY);
                g2.drawString("GAME SETUP", 20, 35);
            }
        };
        p.setPreferredSize(new Dimension(0, 50));
        return p;
    }

    private JPanel createBottomBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        p.setBackground(ThemeConstants.BG_LIGHTER);
        p.setPreferredSize(new Dimension(0, 65));

        JButton back = ThemeConstants.makeStyledButton("\u2190 BACK", ThemeConstants.BG_CARD, ThemeConstants.TEXT_PRIMARY);
        back.addActionListener(e -> GameLauncher.getInstance().showMainMenu());

        JButton start = ThemeConstants.makeStyledButton("START GAME", ThemeConstants.NEON_GOLD, ThemeConstants.BG_DARK);
        start.addActionListener(e -> startGame());

        p.add(back);
        p.add(start);
        return p;
    }

    private void startGame() {
        config = new GameConfig();
        config.gameType = gameType;

        switch (gameType) {
            case SNAKE:
                config.playerCount = snakePlayers.getSelectedIndex() == 0 ? 1 : 2;
                config.aiDifficulty = snakeDifficulty.getSelectedIndex();
                int[] sizes = {20, 30, 40};
                config.boardSize = sizes[snakeBoardSize.getSelectedIndex()];
                config.gameSpeed = snakeSpeed.getSelectedIndex();
                config.isAI = new boolean[]{false, config.playerCount == 1};
                break;
            case LUDO:
                config.ludoPlayerCount = ludoPlayers.getSelectedIndex() + 2;
                config.playerCount = config.ludoPlayerCount;
                config.aiDifficulty = ludoDifficulty.getSelectedIndex();
                config.isAI = new boolean[4];
                for (int i = 0; i < 4; i++) {
                    config.isAI[i] = (i < config.ludoPlayerCount) && ludoAIToggles[i].isSelected();
                }
                config.isAI[0] = false; // Player 1 always human
                break;
            case CHESS:
                config.playerCount = chessPlayers.getSelectedIndex() == 0 ? 1 : 2;
                config.aiDifficulty = chessDifficulty.getSelectedIndex();
                config.playAsWhite = chessSide.getSelectedIndex() == 0;
                config.showHints = chessHints.isSelected();
                int[] timers = {0, 5, 10};
                config.timerMinutes = timers[chessTimer.getSelectedIndex()];
                config.isAI = new boolean[]{false, config.playerCount == 1};
                break;
        }

        GameLauncher.getInstance().startGame(config);
    }

    // --- Setup panels ---

    private JPanel createSnakeSetup() {
        JPanel p = createSetupPanel();
        snakePlayers = addCombo(p, "Players:", new String[]{"1 Player (vs AI)", "2 Players (Local)"});
        snakeDifficulty = addCombo(p, "AI Difficulty:", new String[]{"Easy", "Medium", "Hard"});
        snakeBoardSize = addCombo(p, "Board Size:", new String[]{"Small (20x20)", "Medium (30x30)", "Large (40x40)"});
        snakeBoardSize.setSelectedIndex(1);
        snakeSpeed = addCombo(p, "Speed:", new String[]{"Slow", "Normal", "Fast"});
        snakeSpeed.setSelectedIndex(1);
        addFiller(p);
        return wrapCenter(p);
    }

    private JPanel createLudoSetup() {
        JPanel p = createSetupPanel();
        ludoPlayers = addCombo(p, "Players:", new String[]{"2 Players", "3 Players", "4 Players"});
        ludoPlayers.setSelectedIndex(2);
        ludoDifficulty = addCombo(p, "AI Difficulty:", new String[]{"Easy", "Hard"});
        ludoDifficulty.setSelectedIndex(1);

        String[] colors = {"Red", "Green", "Yellow", "Blue"};
        Color[] colorVals = {ThemeConstants.LUDO_RED, ThemeConstants.LUDO_GREEN,
                ThemeConstants.LUDO_YELLOW, ThemeConstants.LUDO_BLUE};

        for (int i = 0; i < 4; i++) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(500, 40));

            JLabel dot = new JLabel("\u25CF ");
            dot.setFont(ThemeConstants.FONT_BODY);
            dot.setForeground(colorVals[i]);

            JLabel lbl = new JLabel("Player " + (i + 1) + " (" + colors[i] + ")");
            lbl.setFont(ThemeConstants.FONT_BODY);
            lbl.setForeground(ThemeConstants.TEXT_PRIMARY);

            ludoAIToggles[i] = new JToggleButton(i == 0 ? "Human" : "Computer");
            ludoAIToggles[i].setSelected(i != 0);
            ludoAIToggles[i].setFont(ThemeConstants.FONT_SMALL);
            ludoAIToggles[i].setBackground(i == 0 ? ThemeConstants.NEON_GREEN : ThemeConstants.NEON_RED);
            ludoAIToggles[i].setForeground(ThemeConstants.BG_DARK);
            ludoAIToggles[i].setFocusPainted(false);
            if (i == 0) ludoAIToggles[i].setEnabled(false);

            final int idx = i;
            ludoAIToggles[i].addActionListener(e -> {
                JToggleButton btn = ludoAIToggles[idx];
                btn.setText(btn.isSelected() ? "Computer" : "Human");
                btn.setBackground(btn.isSelected() ? ThemeConstants.NEON_RED : ThemeConstants.NEON_GREEN);
            });

            row.add(dot); row.add(lbl); row.add(ludoAIToggles[i]);
            p.add(row);
        }
        addFiller(p);
        return wrapCenter(p);
    }

    private JPanel createChessSetup() {
        JPanel p = createSetupPanel();
        chessPlayers = addCombo(p, "Players:", new String[]{"1 Player (vs AI)", "2 Players (Local)"});
        chessDifficulty = addCombo(p, "AI Difficulty:", new String[]{"Easy", "Medium", "Hard"});
        chessDifficulty.setSelectedIndex(1);
        chessSide = addCombo(p, "Play as:", new String[]{"White", "Black"});
        chessTimer = addCombo(p, "Timer:", new String[]{"No Timer", "5 Minutes", "10 Minutes"});
        chessHints = new JCheckBox("Show move hints", true);
        chessHints.setFont(ThemeConstants.FONT_BODY);
        chessHints.setForeground(ThemeConstants.TEXT_PRIMARY);
        chessHints.setOpaque(false);
        chessHints.setMaximumSize(new Dimension(500, 35));
        p.add(chessHints);
        addFiller(p);
        return wrapCenter(p);
    }

    // Helpers
    private JPanel createSetupPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));
        p.setMaximumSize(new Dimension(500, 600));
        return p;
    }

    private JComboBox<String> addCombo(JPanel parent, String label, String[] items) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(500, 40));
        JLabel lbl = new JLabel(label);
        lbl.setFont(ThemeConstants.FONT_BODY);
        lbl.setForeground(ThemeConstants.TEXT_PRIMARY);
        lbl.setPreferredSize(new Dimension(150, 30));
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setFont(ThemeConstants.FONT_BODY);
        combo.setPreferredSize(new Dimension(250, 30));
        row.add(lbl);
        row.add(combo);
        parent.add(row);
        return combo;
    }

    private void addFiller(JPanel p) { p.add(Box.createVerticalGlue()); }

    private JPanel wrapCenter(JPanel inner) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(inner);
        return wrapper;
    }
}
