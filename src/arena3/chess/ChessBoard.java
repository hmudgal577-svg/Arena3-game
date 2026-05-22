package arena3.chess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import arena3.chess.ChessPiece.*;
import arena3.ui.ThemeConstants;

public class ChessBoard extends JPanel {
    private final ChessEngine engine;
    private int cellSize = 75;
    private int offsetX, offsetY;
    private boolean flipped = false;

    private Point selectedSquare = null;
    private List<Point> validMoves = null;
    private Point lastMoveFrom = null, lastMoveTo = null;
    private boolean showHints = true;

    private Runnable onMoveCallback;

    public ChessBoard(ChessEngine engine, boolean flipped, boolean showHints) {
        this.engine = engine;
        this.flipped = flipped;
        this.showHints = showHints;
        setBackground(ThemeConstants.BG_DARK);
        setOpaque(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    public void setOnMoveCallback(Runnable cb) { this.onMoveCallback = cb; }
    public void setShowHints(boolean show) { this.showHints = show; }
    public void clearSelection() { selectedSquare = null; validMoves = null; }

    private int toBoard(int screen, boolean isCol) {
        int offset = isCol ? offsetX : offsetY;
        int idx = (screen - offset) / cellSize;
        if (flipped) idx = 7 - idx;
        return idx;
    }

    private void handleClick(int mx, int my) {
        if (engine.gameOver) return;

        int col = toBoard(mx, true);
        int row = toBoard(my, false);

        if (col < 0 || col > 7 || row < 0 || row > 7) return;

        Point clicked = new Point(col, row);

        if (selectedSquare == null) {
            // Select piece
            ChessPiece piece = engine.board[row][col];
            if (piece != null && piece.color == engine.currentTurn) {
                selectedSquare = clicked;
                validMoves = engine.getLegalMoves(row, col);
                repaint();
            }
        } else {
            // Try to move
            if (validMoves != null && validMoves.contains(clicked)) {
                // Check promotion
                ChessPiece piece = engine.board[selectedSquare.y][selectedSquare.x];
                Type promoType = null;
                if (piece != null && piece.type == Type.PAWN) {
                    int promoRow = (piece.color == PieceColor.WHITE) ? 0 : 7;
                    if (clicked.y == promoRow) {
                        promoType = askPromotion();
                    }
                }

                lastMoveFrom = new Point(selectedSquare);
                lastMoveTo = new Point(clicked);
                engine.makeMove(selectedSquare, clicked, promoType);
                selectedSquare = null;
                validMoves = null;
                repaint();
                if (onMoveCallback != null) onMoveCallback.run();
            } else {
                // Reselect or deselect
                ChessPiece piece = engine.board[row][col];
                if (piece != null && piece.color == engine.currentTurn) {
                    selectedSquare = clicked;
                    validMoves = engine.getLegalMoves(row, col);
                } else {
                    selectedSquare = null;
                    validMoves = null;
                }
                repaint();
            }
        }
    }

    private Type askPromotion() {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(this, "Promote pawn to:", "Pawn Promotion",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        switch (choice) {
            case 1: return Type.ROOK;
            case 2: return Type.BISHOP;
            case 3: return Type.KNIGHT;
            default: return Type.QUEEN;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        ThemeConstants.enableAntialiasing(g2);

        // Calculate sizing
        int avail = Math.min(getWidth() - 40, getHeight() - 40);
        cellSize = avail / 8;
        cellSize = Math.max(cellSize, 40);
        offsetX = (getWidth() - cellSize * 8) / 2;
        offsetY = (getHeight() - cellSize * 8) / 2;

        // Draw board
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int dr = flipped ? 7 - r : r;
                int dc = flipped ? 7 - c : c;
                int x = offsetX + c * cellSize;
                int y = offsetY + r * cellSize;

                // Square color
                boolean light = (dr + dc) % 2 == 0;
                g2.setColor(light ? ThemeConstants.BOARD_LIGHT : ThemeConstants.BOARD_DARK);
                g2.fillRect(x, y, cellSize, cellSize);

                // Last move highlight
                if (lastMoveFrom != null && lastMoveTo != null) {
                    if ((dr == lastMoveFrom.y && dc == lastMoveFrom.x) ||
                        (dr == lastMoveTo.y && dc == lastMoveTo.x)) {
                        g2.setColor(new Color(255, 255, 0, 60));
                        g2.fillRect(x, y, cellSize, cellSize);
                    }
                }

                // Selected square
                if (selectedSquare != null && dr == selectedSquare.y && dc == selectedSquare.x) {
                    g2.setColor(new Color(0, 255, 255, 80));
                    g2.fillRect(x, y, cellSize, cellSize);
                }

                // Check highlight
                if (engine.inCheck) {
                    ChessPiece king = engine.board[dr][dc];
                    if (king != null && king.type == Type.KING && king.color == engine.currentTurn) {
                        g2.setColor(new Color(255, 0, 0, 100));
                        g2.fillRect(x, y, cellSize, cellSize);
                        // Red glow
                        for (int gl = 3; gl >= 0; gl--) {
                            g2.setColor(new Color(255, 0, 0, 20 + gl * 15));
                            g2.fillRect(x - gl, y - gl, cellSize + gl * 2, cellSize + gl * 2);
                        }
                    }
                }

                // Valid move indicators
                if (showHints && validMoves != null) {
                    for (Point vm : validMoves) {
                        if (vm.x == dc && vm.y == dr) {
                            if (engine.board[dr][dc] != null) {
                                // Capture indicator
                                g2.setColor(new Color(255, 34, 68, 80));
                                g2.fillRect(x, y, cellSize, cellSize);
                                g2.setColor(new Color(255, 34, 68, 150));
                                g2.setStroke(new BasicStroke(3));
                                g2.drawRect(x + 2, y + 2, cellSize - 4, cellSize - 4);
                                g2.setStroke(new BasicStroke(1));
                            } else {
                                // Move dot
                                g2.setColor(new Color(0, 0, 0, 60));
                                int dotR = cellSize / 6;
                                g2.fillOval(x + cellSize / 2 - dotR, y + cellSize / 2 - dotR,
                                        dotR * 2, dotR * 2);
                            }
                        }
                    }
                }

                // Draw piece
                ChessPiece piece = engine.board[dr][dc];
                if (piece != null) {
                    drawPiece(g2, piece, x, y);
                }
            }
        }

        // Board border
        g2.setColor(new Color(80, 70, 60));
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(offsetX - 1, offsetY - 1, cellSize * 8 + 2, cellSize * 8 + 2);
        g2.setStroke(new BasicStroke(1));

        // Rank and file labels
        g2.setFont(ThemeConstants.FONT_TINY);
        for (int i = 0; i < 8; i++) {
            int di = flipped ? 7 - i : i;
            // File labels (a-h)
            String file = String.valueOf((char)('a' + di));
            g2.setColor(ThemeConstants.TEXT_DIM);
            g2.drawString(file, offsetX + i * cellSize + cellSize / 2 - 3, offsetY + cellSize * 8 + 14);

            // Rank labels (1-8)
            String rank = String.valueOf(8 - di);
            g2.drawString(rank, offsetX - 14, offsetY + i * cellSize + cellSize / 2 + 4);
        }
    }

    private void drawPiece(Graphics2D g2, ChessPiece piece, int x, int y) {
        String symbol = piece.getSymbol();
        int fontSize = (int)(cellSize * 0.75);
        g2.setFont(new Font("Serif", Font.PLAIN, fontSize));
        FontMetrics fm = g2.getFontMetrics();

        int tx = x + (cellSize - fm.stringWidth(symbol)) / 2;
        int ty = y + (cellSize + fm.getAscent() - fm.getDescent()) / 2;

        // Outline (draw in black offset by ±1)
        g2.setColor(Color.BLACK);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                g2.drawString(symbol, tx + dx, ty + dy);
            }
        }

        // Piece color
        if (piece.color == PieceColor.WHITE) {
            g2.setColor(new Color(255, 255, 250));
        } else {
            g2.setColor(new Color(50, 50, 50));
        }
        g2.drawString(symbol, tx, ty);
    }
}
