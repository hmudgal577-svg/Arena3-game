package arena3.ludo;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import arena3.ui.ThemeConstants;

public class LudoBoard extends JPanel {
    private final LudoEngine engine;
    private int cellSize = 40;
    private int offsetX, offsetY;
    private LudoToken selectedToken = null;
    private List<LudoToken> validTokens = null;

    private static final Color[] PLAYER_COLORS = {
        ThemeConstants.LUDO_RED, ThemeConstants.LUDO_GREEN,
        ThemeConstants.LUDO_YELLOW, ThemeConstants.LUDO_BLUE
    };

    public LudoBoard(LudoEngine engine) {
        this.engine = engine;
        setBackground(ThemeConstants.BG_DARK);
        setOpaque(true);
    }

    public void setValidTokens(List<LudoToken> tokens) { this.validTokens = tokens; }
    public void setSelectedToken(LudoToken t) { this.selectedToken = t; }

    public LudoToken getTokenAt(int mx, int my) {
        for (LudoPlayer p : engine.getPlayers()) {
            if (p == null) continue;
            for (LudoToken t : p.tokens) {
                int[] pos = LudoEngine.getScreenPos(t, cellSize, offsetX, offsetY);
                int dx = mx - pos[0], dy = my - pos[1];
                if (dx * dx + dy * dy < (cellSize / 2) * (cellSize / 2)) {
                    return t;
                }
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        ThemeConstants.enableAntialiasing(g2);

        // Calculate sizing
        int boardPixels = 15;
        int avail = Math.min(getWidth() - 20, getHeight() - 100);
        cellSize = avail / boardPixels;
        cellSize = Math.max(cellSize, 20);
        int boardSize = cellSize * 15;
        offsetX = (getWidth() - boardSize) / 2;
        offsetY = (getHeight() - boardSize - 80) / 2 + 10;

        // Board background
        g2.setColor(new Color(245, 240, 230));
        g2.fillRect(offsetX, offsetY, boardSize, boardSize);

        // Draw grid
        g2.setColor(new Color(200, 195, 185));
        for (int i = 0; i <= 15; i++) {
            g2.drawLine(offsetX + i * cellSize, offsetY, offsetX + i * cellSize, offsetY + boardSize);
            g2.drawLine(offsetX, offsetY + i * cellSize, offsetX + boardSize, offsetY + i * cellSize);
        }

        // Home bases (6x6 in each corner)
        drawHomeBase(g2, 0, 0, PLAYER_COLORS[0]);     // Red top-left
        drawHomeBase(g2, 9, 0, PLAYER_COLORS[1]);      // Green top-right
        drawHomeBase(g2, 9, 9, PLAYER_COLORS[2]);      // Yellow bottom-right
        drawHomeBase(g2, 0, 9, PLAYER_COLORS[3]);      // Blue bottom-left

        // Center home (3x3)
        int cx = offsetX + 6 * cellSize;
        int cy = offsetY + 6 * cellSize;
        int cs = 3 * cellSize;
        // 4 triangles
        int mx = cx + cs / 2, my = cy + cs / 2;
        g2.setColor(PLAYER_COLORS[0]);
        g2.fillPolygon(new int[]{cx, cx + cs, mx}, new int[]{cy, cy, my}, 3);
        g2.setColor(PLAYER_COLORS[1]);
        g2.fillPolygon(new int[]{cx + cs, cx + cs, mx}, new int[]{cy, cy + cs, my}, 3);
        g2.setColor(PLAYER_COLORS[2]);
        g2.fillPolygon(new int[]{cx, cx + cs, mx}, new int[]{cy + cs, cy + cs, my}, 3);
        g2.setColor(PLAYER_COLORS[3]);
        g2.fillPolygon(new int[]{cx, cx, mx}, new int[]{cy, cy + cs, my}, 3);
        g2.setColor(new Color(200, 195, 185));
        g2.drawRect(cx, cy, cs, cs);

        // Colored safe columns
        for (int i = 0; i < 4; i++) {
            int[][] homeCol = LudoEngine.HOME_COLUMNS[i];
            for (int[] rc : homeCol) {
                g2.setColor(ThemeConstants.withAlpha(PLAYER_COLORS[i], 100));
                g2.fillRect(offsetX + rc[1] * cellSize, offsetY + rc[0] * cellSize, cellSize, cellSize);
            }
        }

        // Safe squares (stars)
        int[] safeAbs = {0, 8, 13, 21, 26, 34, 39, 47};
        for (int abs : safeAbs) {
            int[] rc = LudoEngine.MAIN_TRACK[abs];
            int sx = offsetX + rc[1] * cellSize + cellSize / 2;
            int sy = offsetY + rc[0] * cellSize + cellSize / 2;
            drawStar(g2, sx, sy, cellSize / 3, new Color(180, 160, 80));
        }

        // Entry colored squares
        for (int i = 0; i < 4; i++) {
            int[] rc = LudoEngine.MAIN_TRACK[LudoEngine.ENTRY_ABS[i]];
            g2.setColor(ThemeConstants.withAlpha(PLAYER_COLORS[i], 120));
            g2.fillRect(offsetX + rc[1] * cellSize, offsetY + rc[0] * cellSize, cellSize, cellSize);
        }

        // Draw tokens
        for (LudoPlayer p : engine.getPlayers()) {
            if (p == null) continue;
            // Count tokens at each position to stack
            java.util.Map<String, Integer> posCount = new java.util.HashMap<>();
            java.util.Map<String, Integer> posIdx = new java.util.HashMap<>();

            for (LudoToken t : p.tokens) {
                String key = p.index + "_" + t.position;
                posCount.merge(key, 1, Integer::sum);
                posIdx.put(key, 0);
            }

            for (LudoToken t : p.tokens) {
                String key = p.index + "_" + t.position;
                int stackIdx = posIdx.get(key);
                posIdx.put(key, stackIdx + 1);
                int total = posCount.get(key);

                int[] pos = LudoEngine.getScreenPos(t, cellSize, offsetX, offsetY);
                int tx = pos[0], ty = pos[1];

                // Stack offset
                if (total > 1) {
                    tx += (stackIdx - total / 2) * 6;
                    ty += (stackIdx % 2) * 6 - 3;
                }

                boolean isValid = validTokens != null && validTokens.contains(t);
                boolean isSel = t == selectedToken;
                drawToken(g2, tx, ty, p.color, t.tokenId + 1, isValid, isSel);
            }
        }

        // Current player indicator glow
        int cpi = engine.getCurrentPlayerIndex();
        g2.setColor(ThemeConstants.withAlpha(PLAYER_COLORS[cpi], 100));
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(offsetX - 3, offsetY - 3, boardSize + 6, boardSize + 6);
        g2.setStroke(new BasicStroke(1));

        // Dice area
        int diceX = offsetX + boardSize + 20;
        int diceY = offsetY + boardSize / 2 - 40;
        if (diceX + 80 > getWidth()) {
            diceX = offsetX + boardSize / 2 - 40;
            diceY = offsetY + boardSize + 10;
        }
        engine.getDice().paint(g2, diceX, diceY, 70);

        // Status text
        g2.setFont(ThemeConstants.FONT_BODY);
        g2.setColor(ThemeConstants.TEXT_PRIMARY);
        String status = engine.getStatusMessage();
        if (status != null && !status.isEmpty()) {
            g2.drawString(status, offsetX, offsetY + boardSize + 30);
        }

        // Turn indicator
        g2.setColor(PLAYER_COLORS[cpi]);
        g2.setFont(ThemeConstants.FONT_SUBHEAD);
        String turnText = engine.getCurrentPlayer().name + "'s Turn";
        if (engine.getCurrentPlayer().isAI && engine.isDiceRolled()) turnText = "AI Thinking...";
        g2.drawString(turnText, offsetX, offsetY + boardSize + 55);
    }

    private void drawHomeBase(Graphics2D g2, int col, int row, Color color) {
        int x = offsetX + col * cellSize;
        int y = offsetY + row * cellSize;
        int size = 6 * cellSize;
        g2.setColor(ThemeConstants.withAlpha(color, 80));
        g2.fillRect(x, y, size, size);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(x, y, size, size);
        // Inner white circle area for tokens at home
        int inner = 4 * cellSize;
        int ix = x + cellSize;
        int iy = y + cellSize;
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(ix, iy, inner, inner, 15, 15);
        g2.setColor(color);
        g2.drawRoundRect(ix, iy, inner, inner, 15, 15);
        g2.setStroke(new BasicStroke(1));
    }

    private void drawToken(Graphics2D g2, int x, int y, Color color, int num,
                           boolean valid, boolean selected) {
        int r = cellSize / 2 - 2;
        if (r < 8) r = 8;

        // --- Animated glow for valid/selected ---
        if (selected) {
            for (int i = 4; i >= 0; i--) {
                g2.setColor(ThemeConstants.withAlpha(ThemeConstants.NEON_GOLD, 15 + i * 18));
                int extra = i * 3;
                g2.fillOval(x - r - extra, y - r - extra, (r + extra) * 2, (r + extra) * 2);
            }
        } else if (valid) {
            for (int i = 3; i >= 0; i--) {
                g2.setColor(ThemeConstants.withAlpha(ThemeConstants.NEON_CYAN, 15 + i * 20));
                int extra = i * 3;
                g2.fillOval(x - r - extra, y - r - extra, (r + extra) * 2, (r + extra) * 2);
            }
        }

        // --- Drop shadow ---
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillOval(x - r + 3, y - r + 3, r * 2, r * 2);

        // --- Base ring (slightly larger, darker) ---
        Color dark = color.darker().darker();
        g2.setColor(dark);
        g2.fillOval(x - r, y - r, r * 2, r * 2);

        // --- Main body with radial gradient ---
        int bodyR = r - 2;
        Color bright = brighter(color, 40);
        GradientPaint gp = new GradientPaint(
                x - bodyR / 2, y - bodyR / 2, bright,
                x + bodyR / 2, y + bodyR / 2, color.darker());
        g2.setPaint(gp);
        g2.fillOval(x - bodyR, y - bodyR, bodyR * 2, bodyR * 2);

        // --- Inner circle (lighter) ---
        int innerR = (int)(bodyR * 0.65);
        GradientPaint gp2 = new GradientPaint(
                x - innerR, y - innerR, brighter(color, 80),
                x + innerR, y + innerR, color);
        g2.setPaint(gp2);
        g2.fillOval(x - innerR, y - innerR, innerR * 2, innerR * 2);

        // --- Specular highlight (top-left shine) ---
        int hlR = (int)(bodyR * 0.4);
        int hlX = x - bodyR / 3;
        int hlY = y - bodyR / 3;
        GradientPaint hlGrad = new GradientPaint(
                hlX, hlY, new Color(255, 255, 255, 180),
                hlX + hlR, hlY + hlR, new Color(255, 255, 255, 0));
        g2.setPaint(hlGrad);
        g2.fillOval(hlX - hlR / 2, hlY - hlR / 2, hlR, hlR);

        // --- Border ring ---
        g2.setColor(new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(x - bodyR, y - bodyR, bodyR * 2, bodyR * 2);
        g2.setStroke(new BasicStroke(1));

        // --- Number text ---
        g2.setColor(Color.WHITE);
        Font numFont = new Font("Arial", Font.BOLD, Math.max(10, (int)(r * 0.9)));
        g2.setFont(numFont);
        FontMetrics fm = g2.getFontMetrics();
        String s = String.valueOf(num);
        int tx = x - fm.stringWidth(s) / 2;
        int ty = y + fm.getAscent() / 2 - 1;
        // Text shadow
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawString(s, tx + 1, ty + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(s, tx, ty);
    }

    private Color brighter(Color c, int amount) {
        return new Color(
            Math.min(255, c.getRed() + amount),
            Math.min(255, c.getGreen() + amount),
            Math.min(255, c.getBlue() + amount));
    }

    private void drawStar(Graphics2D g2, int cx, int cy, int r, Color color) {
        g2.setColor(color);
        int points = 5;
        int[] px = new int[points * 2];
        int[] py = new int[points * 2];
        for (int i = 0; i < points * 2; i++) {
            double angle = Math.PI * i / points - Math.PI / 2;
            int rad = (i % 2 == 0) ? r : r / 2;
            px[i] = cx + (int)(Math.cos(angle) * rad);
            py[i] = cy + (int)(Math.sin(angle) * rad);
        }
        g2.fillPolygon(px, py, points * 2);
    }
}
