package arena3.ludo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import arena3.core.GameLauncher;
import arena3.core.GameState;
import arena3.core.GameState.*;
import arena3.core.SoundEngine;
import arena3.ui.ThemeConstants;

public class LudoGame extends JPanel {
    private final GameConfig config;
    private final LudoEngine engine;
    private final LudoBoard board;
    private JButton rollBtn;
    private final Timer repaintTimer;
    private final JLabel[] infoLabels;
    private boolean processingTurn = false;

    public LudoGame(GameConfig config) {
        this.config = config;
        setLayout(new BorderLayout());
        setBackground(ThemeConstants.BG_DARK);
        setFocusable(true);

        String[] names = {"Red", "Green", "Yellow", "Blue"};
        Color[] colors = {ThemeConstants.LUDO_RED, ThemeConstants.LUDO_GREEN,
                ThemeConstants.LUDO_YELLOW, ThemeConstants.LUDO_BLUE};
        LudoPlayer[] players = new LudoPlayer[4];
        for (int i = 0; i < config.ludoPlayerCount; i++) {
            players[i] = new LudoPlayer(i, names[i], colors[i], config.isAI[i]);
        }

        engine = new LudoEngine(players, config.ludoPlayerCount, config.aiDifficulty);
        board = new LudoBoard(engine);

        add(createTopBar(), BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);

        infoLabels = new JLabel[config.ludoPlayerCount];
        add(createSidePanel(), BorderLayout.EAST);

        board.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                handleBoardClick(e.getX(), e.getY());
            }
        });

        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        am.put("esc", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(LudoGame.this, "Return to menu?",
                        "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    repaintTimer.stop();
                    GameLauncher.getInstance().showMainMenu();
                }
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "roll");
        am.put("roll", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { handleRoll(); }
        });

        repaintTimer = new Timer(50, e -> { updateInfoLabels(); board.repaint(); });
        repaintTimer.start();

        SwingUtilities.invokeLater(() -> {
            requestFocusInWindow();
            startCurrentPlayerTurn();
        });
    }

    /** Central method: starts the current player's turn (human or AI) */
    private void startCurrentPlayerTurn() {
        if (engine.isGameOver() || processingTurn) return;

        LudoPlayer current = engine.getCurrentPlayer();
        if (current.isAI) {
            rollBtn.setEnabled(false);
            scheduleAITurn();
        } else {
            rollBtn.setEnabled(true);
            board.setValidTokens(null);
        }
        board.repaint();
    }

    private void handleRoll() {
        if (engine.isGameOver()) return;
        if (engine.isDiceRolled()) return;
        if (engine.getCurrentPlayer().isAI) return;
        if (engine.getDice().isAnimating()) return;
        if (processingTurn) return;

        rollBtn.setEnabled(false);
        engine.rollDice(() -> {
            if (engine.isDiceRolled()) {
                // Dice rolled, player has valid moves - show them
                List<LudoToken> valid = engine.getValidMoves();
                board.setValidTokens(valid);
                board.repaint();

                if (valid.size() == 1) {
                    // Auto-execute single valid move
                    Timer autoMove = new Timer(400, ev -> {
                        executeMove(valid.get(0));
                    });
                    autoMove.setRepeats(false);
                    autoMove.start();
                }
                // else: wait for player to click a token
            } else {
                // No valid moves, turn was auto-advanced
                board.setValidTokens(null);
                board.repaint();
                // Start next player's turn
                Timer nextDelay = new Timer(500, ev -> startCurrentPlayerTurn());
                nextDelay.setRepeats(false);
                nextDelay.start();
            }
        });
    }

    private void handleBoardClick(int mx, int my) {
        if (engine.isGameOver()) return;
        if (!engine.isDiceRolled()) return;
        if (engine.getCurrentPlayer().isAI) return;
        if (processingTurn) return;

        LudoToken clicked = board.getTokenAt(mx, my);
        if (clicked == null) return;
        if (clicked.playerIndex != engine.getCurrentPlayerIndex()) return;

        List<LudoToken> valid = engine.getValidMoves();
        if (valid.contains(clicked)) {
            executeMove(clicked);
        }
    }

    private void executeMove(LudoToken token) {
        processingTurn = true;
        engine.moveToken(token);
        board.setValidTokens(null);
        board.setSelectedToken(null);
        board.repaint();

        if (engine.isGameOver()) {
            processingTurn = false;
            repaintTimer.stop();
            Timer endDelay = new Timer(1500, e -> showGameOver());
            endDelay.setRepeats(false);
            endDelay.start();
            return;
        }

        // Brief delay then start next turn
        Timer nextDelay = new Timer(400, e -> {
            processingTurn = false;
            startCurrentPlayerTurn();
        });
        nextDelay.setRepeats(false);
        nextDelay.start();
    }

    private void scheduleAITurn() {
        if (engine.isGameOver()) return;
        LudoPlayer aiPlayer = engine.getCurrentPlayer();
        if (!aiPlayer.isAI) { startCurrentPlayerTurn(); return; }

        processingTurn = true;

        // Step 1: Roll dice after delay
        Timer rollDelay = new Timer(600, e -> {
            if (engine.isGameOver()) { processingTurn = false; return; }

            engine.rollDice(() -> {
                board.repaint();

                if (!engine.isDiceRolled()) {
                    // No valid moves, turn auto-advanced
                    Timer nextDelay = new Timer(500, ev -> {
                        processingTurn = false;
                        startCurrentPlayerTurn();
                    });
                    nextDelay.setRepeats(false);
                    nextDelay.start();
                    return;
                }

                // Step 2: AI picks a token to move after delay
                Timer moveDelay = new Timer(500, ev -> {
                    List<LudoToken> valid = engine.getValidMoves();
                    if (!valid.isEmpty()) {
                        LudoToken chosen = engine.aiChooseMove(aiPlayer, valid);
                        if (chosen != null) {
                            engine.moveToken(chosen);
                        }
                    }
                    board.setValidTokens(null);
                    board.repaint();

                    if (engine.isGameOver()) {
                        processingTurn = false;
                        repaintTimer.stop();
                        Timer endDelay = new Timer(1500, ex -> showGameOver());
                        endDelay.setRepeats(false);
                        endDelay.start();
                        return;
                    }

                    // Next turn (could be same AI with extra turn, or different player)
                    Timer nextDelay = new Timer(400, ex -> {
                        processingTurn = false;
                        startCurrentPlayerTurn();
                    });
                    nextDelay.setRepeats(false);
                    nextDelay.start();
                });
                moveDelay.setRepeats(false);
                moveDelay.start();
            });
        });
        rollDelay.setRepeats(false);
        rollDelay.start();
    }

    private void showGameOver() {
        GameResult result = new GameResult();
        result.gameType = GameType.LUDO;
        result.config = config;
        result.message = engine.getGameOverMessage();
        result.winnerName = "Game Over";
        for (LudoPlayer p : engine.getPlayers()) {
            if (p == null) continue;
            if (p.finishOrder == 1) result.winnerName = p.name + " Wins!";
            result.stats.put(p.name + " Tokens Won", p.tokensWon() + "/4");
        }
        SoundEngine.playWin();
        GameLauncher.getInstance().showGameOver(result);
    }

    private void updateInfoLabels() {
        for (int i = 0; i < infoLabels.length; i++) {
            LudoPlayer p = engine.getPlayers()[i];
            if (p != null && infoLabels[i] != null) {
                String marker = (i == engine.getCurrentPlayerIndex()) ? " <<" : "";
                infoLabels[i].setText("  Won:" + p.tokensWon() + " Board:" + p.tokensOnBoard() + marker);
            }
        }
    }

    private JPanel createSidePanel() {
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(ThemeConstants.BG_LIGHTER);
        sidePanel.setPreferredSize(new Dimension(180, 0));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        String[] names = {"Red", "Green", "Yellow", "Blue"};
        Color[] colors = {ThemeConstants.LUDO_RED, ThemeConstants.LUDO_GREEN,
                ThemeConstants.LUDO_YELLOW, ThemeConstants.LUDO_BLUE};

        for (int i = 0; i < config.ludoPlayerCount; i++) {
            LudoPlayer p = engine.getPlayers()[i];
            JLabel lbl = new JLabel(p.name + (p.isAI ? " (AI)" : ""));
            lbl.setFont(ThemeConstants.FONT_BODY);
            lbl.setForeground(p.color);
            lbl.setAlignmentX(LEFT_ALIGNMENT);
            sidePanel.add(lbl);

            infoLabels[i] = new JLabel("  Won:0 Board:0");
            infoLabels[i].setFont(ThemeConstants.FONT_SMALL);
            infoLabels[i].setForeground(ThemeConstants.TEXT_DIM);
            infoLabels[i].setAlignmentX(LEFT_ALIGNMENT);
            sidePanel.add(infoLabels[i]);
            sidePanel.add(Box.createVerticalStrut(8));
        }

        sidePanel.add(Box.createVerticalStrut(15));

        rollBtn = ThemeConstants.makeStyledButton("ROLL DICE", ThemeConstants.NEON_GOLD, ThemeConstants.BG_DARK);
        rollBtn.setMaximumSize(new Dimension(160, 50));
        rollBtn.setAlignmentX(LEFT_ALIGNMENT);
        rollBtn.setFocusable(false);
        rollBtn.addActionListener(e -> handleRoll());
        sidePanel.add(rollBtn);
        sidePanel.add(Box.createVerticalStrut(15));

        JLabel ctrl = new JLabel("<html><small>Click token to move<br>Roll 6 to enter board<br>SPACE = Roll Dice<br>ESC = Menu</small></html>");
        ctrl.setFont(ThemeConstants.FONT_SMALL);
        ctrl.setForeground(ThemeConstants.TEXT_DIM);
        ctrl.setAlignmentX(LEFT_ALIGNMENT);
        sidePanel.add(ctrl);
        sidePanel.add(Box.createVerticalGlue());
        return sidePanel;
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(ThemeConstants.BG_LIGHTER);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ThemeConstants.NEON_GOLD);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        bar.setPreferredSize(new Dimension(0, 45));

        JButton backBtn = ThemeConstants.makeStyledButton("\u2190 MENU", ThemeConstants.BG_CARD, ThemeConstants.TEXT_PRIMARY);
        backBtn.setPreferredSize(new Dimension(100, 35));
        backBtn.setFocusable(false);
        backBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Return to menu?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                repaintTimer.stop();
                GameLauncher.getInstance().showMainMenu();
            }
        });

        JLabel title = new JLabel("LUDO", SwingConstants.CENTER);
        title.setFont(ThemeConstants.FONT_HEADING);
        title.setForeground(ThemeConstants.NEON_GOLD);

        JButton muteBtn = ThemeConstants.makeStyledButton(GameState.soundMuted ? "\uD83D\uDD07" : "\uD83D\uDD0A",
                ThemeConstants.BG_CARD, ThemeConstants.TEXT_PRIMARY);
        muteBtn.setPreferredSize(new Dimension(60, 35));
        muteBtn.setFocusable(false);
        muteBtn.addActionListener(e -> {
            GameState.soundMuted = !GameState.soundMuted;
            muteBtn.setText(GameState.soundMuted ? "\uD83D\uDD07" : "\uD83D\uDD0A");
        });

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        left.setOpaque(false); left.add(backBtn);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        right.setOpaque(false); right.add(muteBtn);
        bar.add(left, BorderLayout.WEST);
        bar.add(title, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }
}
