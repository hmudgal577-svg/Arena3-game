package arena3.chess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import arena3.chess.ChessPiece.*;
import arena3.core.GameLauncher;
import arena3.core.GameState;
import arena3.core.GameState.*;
import arena3.core.SoundEngine;
import arena3.ui.ThemeConstants;

public class ChessGame extends JPanel {
    private final GameConfig config;
    private final ChessEngine engine;
    private final ChessBoard board;
    private final ChessAI ai;
    private final boolean vsAI;
    private final PieceColor aiColor;

    // Timer
    private int whiteTimeSeconds, blackTimeSeconds;
    private final boolean timerEnabled;
    private Timer clockTimer;

    // UI components
    private JLabel statusLabel;
    private JLabel whiteClockLabel, blackClockLabel;
    private DefaultListModel<String> moveListModel;
    private JPanel capturedWhitePanel, capturedBlackPanel;

    public ChessGame(GameConfig config) {
        this.config = config;
        engine = new ChessEngine();
        engine.initBoard();

        vsAI = config.playerCount == 1;
        if (vsAI) {
            aiColor = config.playAsWhite ? PieceColor.BLACK : PieceColor.WHITE;
            ai = new ChessAI(config.aiDifficulty, aiColor);
        } else {
            aiColor = null;
            ai = null;
        }

        boolean flip = !config.playAsWhite;
        board = new ChessBoard(engine, flip, config.showHints);
        board.setOnMoveCallback(this::onMoveMade);

        timerEnabled = config.timerMinutes > 0;
        whiteTimeSeconds = config.timerMinutes * 60;
        blackTimeSeconds = config.timerMinutes * 60;

        setLayout(new BorderLayout());
        setBackground(ThemeConstants.BG_DARK);
        setFocusable(true);

        add(createTopBar(), BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);
        add(createSidePanel(), BorderLayout.EAST);

        // Key bindings
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        am.put("esc", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(ChessGame.this, "Return to menu?",
                        "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    stopTimers();
                    GameLauncher.getInstance().showMainMenu();
                }
            }
        });

        // Start clock if enabled
        if (timerEnabled) {
            clockTimer = new Timer(1000, e -> tickClock());
            clockTimer.start();
        }

        // If AI plays first (player chose black)
        if (vsAI && aiColor == PieceColor.WHITE) {
            SwingUtilities.invokeLater(this::scheduleAIMove);
        }

        updateStatus();
        SwingUtilities.invokeLater(() -> requestFocusInWindow());
    }

    private void onMoveMade() {
        // Determine what happened in the last move
        if (!engine.moveHistory.isEmpty()) {
            ChessMove lastMove = engine.moveHistory.get(engine.moveHistory.size() - 1);

            if (lastMove.capturedPiece != null) {
                SoundEngine.playChessCapture();
            } else {
                SoundEngine.playChessMove();
            }

            if (engine.inCheck) {
                SoundEngine.playCheck();
            }

            // Update move list
            String notation = lastMove.algebraic;
            int moveNum = (engine.moveHistory.size() + 1) / 2;
            if (engine.moveHistory.size() % 2 == 1) {
                moveListModel.addElement(moveNum + ". " + notation);
            } else {
                int lastIdx = moveListModel.size() - 1;
                if (lastIdx >= 0) {
                    moveListModel.set(lastIdx, moveListModel.get(lastIdx) + "  " + notation);
                }
            }
        }

        updateStatus();
        repaint();

        if (engine.gameOver) {
            stopTimers();
            Timer endDelay = new Timer(1500, e -> showGameOver());
            endDelay.setRepeats(false);
            endDelay.start();
            return;
        }

        // Schedule AI move
        if (vsAI && engine.currentTurn == aiColor) {
            board.clearSelection();
            scheduleAIMove();
        }
    }

    private void scheduleAIMove() {
        statusLabel.setText("AI is thinking...");
        statusLabel.setForeground(ThemeConstants.NEON_GOLD);

        SwingWorker<ChessMove, Void> worker = new SwingWorker<>() {
            @Override
            protected ChessMove doInBackground() {
                // Add delay for feel
                try {
                    int delay = config.aiDifficulty == 0 ? 300 : config.aiDifficulty == 1 ? 500 : 800;
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
                return ai.getBestMove(engine);
            }

            @Override
            protected void done() {
                try {
                    ChessMove move = get();
                    if (move != null && !engine.gameOver) {
                        Type promoType = move.isPromotion ? Type.QUEEN : null;
                        engine.makeMove(move.from, move.to, promoType);
                        board.clearSelection();
                        board.repaint();
                        onMoveMade();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void tickClock() {
        if (engine.gameOver) return;
        if (engine.currentTurn == PieceColor.WHITE) {
            whiteTimeSeconds--;
            if (whiteTimeSeconds <= 0) {
                whiteTimeSeconds = 0;
                engine.gameOver = true;
                engine.gameOverMessage = "White's time ran out! Black wins!";
                stopTimers();
                showGameOver();
            }
        } else {
            blackTimeSeconds--;
            if (blackTimeSeconds <= 0) {
                blackTimeSeconds = 0;
                engine.gameOver = true;
                engine.gameOverMessage = "Black's time ran out! White wins!";
                stopTimers();
                showGameOver();
            }
        }
        updateClockDisplay();
    }

    private void stopTimers() {
        if (clockTimer != null) clockTimer.stop();
    }

    private void updateStatus() {
        if (engine.gameOver) {
            statusLabel.setText(engine.gameOverMessage);
            statusLabel.setForeground(ThemeConstants.NEON_GOLD);
        } else if (engine.inCheck) {
            statusLabel.setText((engine.currentTurn == PieceColor.WHITE ? "White" : "Black") + " is in CHECK!");
            statusLabel.setForeground(ThemeConstants.NEON_RED);
        } else {
            statusLabel.setText((engine.currentTurn == PieceColor.WHITE ? "White" : "Black") + " to move");
            statusLabel.setForeground(ThemeConstants.TEXT_PRIMARY);
        }
        updateClockDisplay();
        updateCapturedPieces();
    }

    private void updateClockDisplay() {
        if (!timerEnabled) return;
        whiteClockLabel.setText("W " + formatTime(whiteTimeSeconds));
        blackClockLabel.setText("B " + formatTime(blackTimeSeconds));

        whiteClockLabel.setForeground(engine.currentTurn == PieceColor.WHITE ?
                (whiteTimeSeconds < 30 ? ThemeConstants.NEON_RED : ThemeConstants.TEXT_PRIMARY) :
                ThemeConstants.TEXT_DIM);
        blackClockLabel.setForeground(engine.currentTurn == PieceColor.BLACK ?
                (blackTimeSeconds < 30 ? ThemeConstants.NEON_RED : ThemeConstants.TEXT_PRIMARY) :
                ThemeConstants.TEXT_DIM);
    }

    private String formatTime(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private void updateCapturedPieces() {
        // Handled in paint of side panel
        if (capturedWhitePanel != null) capturedWhitePanel.repaint();
        if (capturedBlackPanel != null) capturedBlackPanel.repaint();
    }

    private void showGameOver() {
        GameResult result = new GameResult();
        result.gameType = GameType.CHESS;
        result.config = config;
        result.message = engine.gameOverMessage;
        result.winnerName = engine.gameOverMessage;

        int adv = engine.getMaterialAdvantage();
        result.stats.put("Material", adv > 0 ? "White +" + adv : adv < 0 ? "Black +" + (-adv) : "Equal");
        result.stats.put("Moves", String.valueOf(engine.moveHistory.size()));
        if (timerEnabled) {
            result.stats.put("White Time", formatTime(whiteTimeSeconds));
            result.stats.put("Black Time", formatTime(blackTimeSeconds));
        }

        SoundEngine.playWin();
        GameLauncher.getInstance().showGameOver(result);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(ThemeConstants.BG_LIGHTER);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ThemeConstants.NEON_CYAN);
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
                stopTimers();
                GameLauncher.getInstance().showMainMenu();
            }
        });

        JLabel title = new JLabel("CHESS", SwingConstants.CENTER);
        title.setFont(ThemeConstants.FONT_HEADING);
        title.setForeground(ThemeConstants.NEON_CYAN);

        JButton muteBtn = ThemeConstants.makeStyledButton(GameState.soundMuted ? "\uD83D\uDD07" : "\uD83D\uDD0A",
                ThemeConstants.BG_CARD, ThemeConstants.TEXT_PRIMARY);
        muteBtn.setPreferredSize(new Dimension(60, 35));
        muteBtn.setFocusable(false);
        muteBtn.addActionListener(e -> {
            GameState.soundMuted = !GameState.soundMuted;
            muteBtn.setText(GameState.soundMuted ? "\uD83D\uDD07" : "\uD83D\uDD0A");
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

    private JPanel createSidePanel() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(ThemeConstants.BG_LIGHTER);
        side.setPreferredSize(new Dimension(220, 0));
        side.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Status
        statusLabel = new JLabel("White to move");
        statusLabel.setFont(ThemeConstants.FONT_SUBHEAD);
        statusLabel.setForeground(ThemeConstants.TEXT_PRIMARY);
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        side.add(statusLabel);
        side.add(Box.createVerticalStrut(10));

        // Clocks
        if (timerEnabled) {
            whiteClockLabel = new JLabel("W " + formatTime(whiteTimeSeconds));
            whiteClockLabel.setFont(new Font("Monospaced", Font.BOLD, 20));
            whiteClockLabel.setForeground(ThemeConstants.TEXT_PRIMARY);
            blackClockLabel = new JLabel("B " + formatTime(blackTimeSeconds));
            blackClockLabel.setFont(new Font("Monospaced", Font.BOLD, 20));
            blackClockLabel.setForeground(ThemeConstants.TEXT_DIM);
            side.add(whiteClockLabel);
            side.add(blackClockLabel);
            side.add(Box.createVerticalStrut(10));
        } else {
            whiteClockLabel = new JLabel();
            blackClockLabel = new JLabel();
        }

        // Captured pieces
        JLabel capLabel = new JLabel("Captured");
        capLabel.setFont(ThemeConstants.FONT_SMALL);
        capLabel.setForeground(ThemeConstants.TEXT_DIM);
        side.add(capLabel);

        capturedWhitePanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                ThemeConstants.enableAntialiasing(g2);
                g2.setFont(new Font("Serif", Font.PLAIN, 18));
                g2.setColor(new Color(200, 200, 200));
                StringBuilder sb = new StringBuilder();
                for (ChessPiece p : engine.blackCaptured) sb.append(p.getSymbol());
                g2.drawString(sb.toString(), 2, 18);
            }
        };
        capturedWhitePanel.setMaximumSize(new Dimension(210, 25));
        capturedWhitePanel.setPreferredSize(new Dimension(210, 25));
        capturedWhitePanel.setOpaque(false);

        capturedBlackPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                ThemeConstants.enableAntialiasing(g2);
                g2.setFont(new Font("Serif", Font.PLAIN, 18));
                g2.setColor(new Color(100, 100, 100));
                StringBuilder sb = new StringBuilder();
                for (ChessPiece p : engine.whiteCaptured) sb.append(p.getSymbol());
                g2.drawString(sb.toString(), 2, 18);
            }
        };
        capturedBlackPanel.setMaximumSize(new Dimension(210, 25));
        capturedBlackPanel.setPreferredSize(new Dimension(210, 25));
        capturedBlackPanel.setOpaque(false);

        side.add(capturedWhitePanel);
        side.add(capturedBlackPanel);

        // Material advantage
        JLabel matLabel = new JLabel() {
            @Override public String getText() {
                int adv = engine.getMaterialAdvantage();
                if (adv > 0) return "White +" + adv;
                if (adv < 0) return "Black +" + (-adv);
                return "Equal material";
            }
        };
        matLabel.setFont(ThemeConstants.FONT_SMALL);
        matLabel.setForeground(ThemeConstants.NEON_CYAN);
        side.add(matLabel);
        side.add(Box.createVerticalStrut(10));

        // Move history
        JLabel movesLabel = new JLabel("Move History");
        movesLabel.setFont(ThemeConstants.FONT_SMALL);
        movesLabel.setForeground(ThemeConstants.TEXT_DIM);
        side.add(movesLabel);

        moveListModel = new DefaultListModel<>();
        JList<String> moveList = new JList<>(moveListModel);
        moveList.setFont(new Font("Monospaced", Font.PLAIN, 13));
        moveList.setBackground(ThemeConstants.BG_DARK);
        moveList.setForeground(ThemeConstants.TEXT_PRIMARY);
        JScrollPane scroll = new JScrollPane(moveList);
        scroll.setPreferredSize(new Dimension(200, 300));
        scroll.setMaximumSize(new Dimension(200, 2000));
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeConstants.BG_LIGHTER));
        side.add(scroll);

        side.add(Box.createVerticalGlue());
        return side;
    }
}
