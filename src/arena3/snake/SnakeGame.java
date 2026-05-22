package arena3.snake;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import arena3.core.GameLauncher;
import arena3.core.GameState;
import arena3.core.GameState.*;
import arena3.core.SoundEngine;
import arena3.ui.ThemeConstants;

public class SnakeGame extends JPanel {
    private final GameConfig config;
    private final SnakeEngine engine;
    private final SnakeAI ai;
    private final Timer gameTimer;
    private boolean paused = false;
    private final SnakeBoard board;
    private long startTime;
    private boolean gameEnded = false;

    public SnakeGame(GameConfig config) {
        this.config = config;
        int w = config.boardSize, h = config.boardSize;
        engine = new SnakeEngine(w, h);

        // Player 1 (human)
        Point p1Start = new Point(w / 4, h / 2);
        SnakePlayer player1 = new SnakePlayer("Player 1", config.playerColors[0], true, p1Start, 3);
        engine.addPlayer(player1);

        if (config.playerCount == 1) {
            // AI snake
            Point aiStart = new Point(3 * w / 4, h / 2);
            SnakePlayer aiPlayer = new SnakePlayer("AI", ThemeConstants.NEON_RED, false, aiStart, 3,
                    SnakePlayer.Direction.LEFT);
            engine.addPlayer(aiPlayer);
            ai = new SnakeAI(config.aiDifficulty);
        } else {
            // Player 2
            Point p2Start = new Point(3 * w / 4, h / 2);
            SnakePlayer player2 = new SnakePlayer("Player 2", config.playerColors[1], true, p2Start, 3,
                    SnakePlayer.Direction.LEFT);
            engine.addPlayer(player2);
            ai = null;
        }

        engine.init();
        startTime = System.currentTimeMillis();

        setLayout(new BorderLayout());
        setBackground(ThemeConstants.BG_DARK);
        setFocusable(true);

        // Top bar
        add(createTopBar(), BorderLayout.NORTH);

        // Board
        board = new SnakeBoard(engine, w, h);
        add(board, BorderLayout.CENTER);

        // Score panel
        add(createScorePanel(), BorderLayout.EAST);

        // Speed: 0=Slow(150ms), 1=Normal(100ms), 2=Fast(60ms)
        int[] delays = {150, 100, 60};
        int delay = delays[config.gameSpeed];

        gameTimer = new Timer(delay, e -> gameTick());
        gameTimer.start();

        setupKeyBindings();

        // Ensure focus
        SwingUtilities.invokeLater(() -> requestFocusInWindow());
    }

    private void gameTick() {
        if (paused || gameEnded) return;

        // AI move
        if (ai != null) {
            for (SnakePlayer p : engine.getPlayers()) {
                if (!p.isHuman && p.alive) {
                    SnakePlayer.Direction d = ai.getNextMove(p, engine.getFood(),
                            engine.getPlayers(), engine.getWidth(), engine.getHeight());
                    if (d != null) p.setDirection(d);
                }
            }
        }

        engine.tick();
        board.repaint();
        repaint();

        if (engine.isGameOver() && !gameEnded) {
            gameEnded = true;
            gameTimer.stop();
            Timer endTimer = new Timer(1500, ev -> showGameOver());
            endTimer.setRepeats(false);
            endTimer.start();
        }
    }

    private void showGameOver() {
        GameResult result = new GameResult();
        result.gameType = GameType.SNAKE;
        result.config = config;
        result.message = engine.getGameOverMessage();

        SnakePlayer best = null;
        for (SnakePlayer p : engine.getPlayers()) {
            if (best == null || p.score > best.score) best = p;
        }
        result.winnerName = best != null ? best.name + " Wins!" : "Nobody";

        for (SnakePlayer p : engine.getPlayers()) {
            result.stats.put(p.name + " Score", String.valueOf(p.score));
            result.stats.put(p.name + " Length", String.valueOf(p.body.size()));
        }
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        result.stats.put("Time", elapsed + "s");

        SoundEngine.playWin();
        GameLauncher.getInstance().showGameOver(result);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(ThemeConstants.BG_LIGHTER);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ThemeConstants.NEON_GREEN);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        bar.setPreferredSize(new Dimension(0, 45));

        JButton backBtn = ThemeConstants.makeStyledButton("\u2190 MENU", ThemeConstants.BG_CARD, ThemeConstants.TEXT_PRIMARY);
        backBtn.setPreferredSize(new Dimension(100, 35));
        backBtn.setFocusable(false);
        backBtn.addActionListener(e -> {
            paused = true;
            if (JOptionPane.showConfirmDialog(this, "Return to menu?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                gameTimer.stop();
                GameLauncher.getInstance().showMainMenu();
            } else {
                paused = false;
                requestFocusInWindow();
            }
        });

        JLabel title = new JLabel("  SNAKE", SwingConstants.CENTER);
        title.setFont(ThemeConstants.FONT_HEADING);
        title.setForeground(ThemeConstants.NEON_GREEN);

        JButton muteBtn = ThemeConstants.makeStyledButton(GameState.soundMuted ? "\uD83D\uDD07" : "\uD83D\uDD0A",
                ThemeConstants.BG_CARD, ThemeConstants.TEXT_PRIMARY);
        muteBtn.setPreferredSize(new Dimension(60, 35));
        muteBtn.setFocusable(false);
        muteBtn.addActionListener(e -> {
            GameState.soundMuted = !GameState.soundMuted;
            muteBtn.setText(GameState.soundMuted ? "\uD83D\uDD07" : "\uD83D\uDD0A");
            requestFocusInWindow();
        });

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        left.setOpaque(false);
        left.add(backBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        right.setOpaque(false);
        right.add(muteBtn);

        bar.add(left, BorderLayout.WEST);
        bar.add(title, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createScorePanel() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                ThemeConstants.enableAntialiasing(g2);
                g2.setColor(ThemeConstants.BG_LIGHTER);
                g2.fillRect(0, 0, getWidth(), getHeight());

                int y = 30;
                for (SnakePlayer p : engine.getPlayers()) {
                    g2.setColor(p.color);
                    g2.setFont(ThemeConstants.FONT_SUBHEAD);
                    g2.drawString(p.name, 15, y);
                    y += 25;
                    g2.setFont(ThemeConstants.FONT_HEADING.deriveFont(28f));
                    g2.drawString(String.valueOf(p.score), 15, y);
                    y += 20;
                    g2.setFont(ThemeConstants.FONT_SMALL);
                    g2.setColor(ThemeConstants.TEXT_DIM);
                    g2.drawString("Length: " + p.body.size(), 15, y);
                    y += 15;
                    g2.drawString(p.alive ? "ALIVE" : "DEAD", 15, y);
                    y += 35;
                }

                y += 20;
                g2.setColor(ThemeConstants.TEXT_DIM);
                g2.setFont(ThemeConstants.FONT_SMALL);
                g2.drawString("CONTROLS", 15, y); y += 18;
                g2.drawString("P1: W A S D", 15, y); y += 15;
                if (config.playerCount > 1) {
                    g2.drawString("P2: Arrows", 15, y); y += 15;
                }
                g2.drawString("P: Pause", 15, y); y += 15;
                g2.drawString("ESC: Menu", 15, y);

                if (paused) {
                    y += 40;
                    g2.setColor(ThemeConstants.NEON_GOLD);
                    g2.setFont(ThemeConstants.FONT_SUBHEAD);
                    g2.drawString("PAUSED", 15, y);
                }
            }
        };
        panel.setPreferredSize(new Dimension(160, 0));
        return panel;
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        // Player 1: WASD
        bindKey(im, am, KeyEvent.VK_W, "p1up", () -> setDir(0, SnakePlayer.Direction.UP));
        bindKey(im, am, KeyEvent.VK_A, "p1left", () -> setDir(0, SnakePlayer.Direction.LEFT));
        bindKey(im, am, KeyEvent.VK_S, "p1down", () -> setDir(0, SnakePlayer.Direction.DOWN));
        bindKey(im, am, KeyEvent.VK_D, "p1right", () -> setDir(0, SnakePlayer.Direction.RIGHT));

        // Player 2: Arrows
        bindKey(im, am, KeyEvent.VK_UP, "p2up", () -> setDir(1, SnakePlayer.Direction.UP));
        bindKey(im, am, KeyEvent.VK_LEFT, "p2left", () -> setDir(1, SnakePlayer.Direction.LEFT));
        bindKey(im, am, KeyEvent.VK_DOWN, "p2down", () -> setDir(1, SnakePlayer.Direction.DOWN));
        bindKey(im, am, KeyEvent.VK_RIGHT, "p2right", () -> setDir(1, SnakePlayer.Direction.RIGHT));

        // Pause
        bindKey(im, am, KeyEvent.VK_P, "pause", () -> { paused = !paused; repaint(); });
        bindKey(im, am, KeyEvent.VK_SPACE, "pause2", () -> { paused = !paused; repaint(); });

        // ESC
        bindKey(im, am, KeyEvent.VK_ESCAPE, "esc", () -> {
            paused = true;
            if (JOptionPane.showConfirmDialog(this, "Return to menu?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                gameTimer.stop();
                GameLauncher.getInstance().showMainMenu();
            } else {
                paused = false;
            }
        });
    }

    private void setDir(int playerIndex, SnakePlayer.Direction dir) {
        java.util.List<SnakePlayer> players = engine.getPlayers();
        if (playerIndex < players.size() && players.get(playerIndex).isHuman) {
            players.get(playerIndex).setDirection(dir);
        }
    }

    private void bindKey(InputMap im, ActionMap am, int keyCode, String name, Runnable action) {
        im.put(KeyStroke.getKeyStroke(keyCode, 0), name);
        am.put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }
}
