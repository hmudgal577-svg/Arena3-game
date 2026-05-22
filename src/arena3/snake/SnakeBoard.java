package arena3.snake;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import arena3.ui.ThemeConstants;

public class SnakeBoard extends JPanel {
    private final SnakeEngine engine;
    private final int gridW, gridH;
    private int cellSize = 20;
    private float foodPulse = 0;

    public SnakeBoard(SnakeEngine engine, int gridW, int gridH) {
        this.engine = engine;
        this.gridW = gridW;
        this.gridH = gridH;
        setBackground(ThemeConstants.BG_DARK);
        setOpaque(true);
        new Timer(16, e -> { foodPulse += 0.08f; repaint(); }).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        ThemeConstants.enableAntialiasing(g2);

        int availW = getWidth() - 20;
        int availH = getHeight() - 20;
        cellSize = Math.min(availW / gridW, availH / gridH);
        cellSize = Math.max(cellSize, 4);
        int offsetX = (getWidth() - gridW * cellSize) / 2;
        int offsetY = (getHeight() - gridH * cellSize) / 2;

        // Background
        g2.setColor(ThemeConstants.BG_DARK);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Subtle grid
        g2.setColor(new Color(30, 35, 55, 40));
        for (int x = 0; x <= gridW; x++)
            g2.drawLine(offsetX + x * cellSize, offsetY, offsetX + x * cellSize, offsetY + gridH * cellSize);
        for (int y = 0; y <= gridH; y++)
            g2.drawLine(offsetX, offsetY + y * cellSize, offsetX + gridW * cellSize, offsetY + y * cellSize);

        // Checkerboard pattern
        for (int gy = 0; gy < gridH; gy++) {
            for (int gx = 0; gx < gridW; gx++) {
                if ((gx + gy) % 2 == 0) {
                    g2.setColor(new Color(18, 22, 38));
                } else {
                    g2.setColor(new Color(14, 18, 32));
                }
                g2.fillRect(offsetX + gx * cellSize, offsetY + gy * cellSize, cellSize, cellSize);
            }
        }

        // Board border glow
        for (int i = 3; i >= 0; i--) {
            g2.setColor(ThemeConstants.withAlpha(ThemeConstants.NEON_GREEN, 12 + (3 - i) * 12));
            g2.setStroke(new BasicStroke(1 + i));
            g2.drawRect(offsetX - i, offsetY - i, gridW * cellSize + i * 2, gridH * cellSize + i * 2);
        }
        g2.setStroke(new BasicStroke(1));

        // Food with pulsing glow
        Point food = engine.getFood();
        if (food != null) {
            float pulse = 0.6f + 0.4f * (float) Math.sin(foodPulse);
            int fx = offsetX + food.x * cellSize;
            int fy = offsetY + food.y * cellSize;
            int pad = (int)(cellSize * 0.12f);

            // Outer glow
            for (int i = 4; i >= 0; i--) {
                g2.setColor(ThemeConstants.withAlpha(ThemeConstants.NEON_RED, (int)(12 * pulse + i * 10)));
                int extra = i * 3;
                g2.fillOval(fx + pad - extra, fy + pad - extra,
                        cellSize - pad * 2 + extra * 2, cellSize - pad * 2 + extra * 2);
            }
            // Apple body
            GradientPaint foodGrad = new GradientPaint(fx, fy, new Color(255, 60, 60), fx + cellSize, fy + cellSize, new Color(180, 20, 20));
            g2.setPaint(foodGrad);
            g2.fillOval(fx + pad, fy + pad, cellSize - pad * 2, cellSize - pad * 2);
            // Shine
            g2.setColor(new Color(255, 255, 255, 100));
            int shR = cellSize / 5;
            g2.fillOval(fx + pad + 2, fy + pad + 2, shR, shR);
        }

        // Snakes
        for (SnakePlayer player : engine.getPlayers()) {
            Point[] bodyArr = player.body.toArray(new Point[0]);
            int len = bodyArr.length;
            if (len == 0) continue;

            // Draw connected body segments (tail to head so head is on top)
            for (int i = len - 1; i >= 0; i--) {
                Point bp = bodyArr[i];
                float bx = offsetX + bp.x * cellSize;
                float by = offsetY + bp.y * cellSize;

                float ratio = 1.0f - (float) i / Math.max(len, 1);
                int alpha = player.alive ? (int)(120 + 135 * ratio) : 60;

                // Segment thickness: thicker at head, thinner at tail
                float thickness = player.alive ? (0.55f + 0.4f * ratio) : 0.5f;
                float inset = cellSize * (1f - thickness) / 2f;

                // Draw connection to previous segment (toward head)
                if (i > 0) {
                    Point prev = bodyArr[i - 1];
                    float px = offsetX + prev.x * cellSize;
                    float py = offsetY + prev.y * cellSize;

                    Color segCol = ThemeConstants.withAlpha(player.color, alpha);
                    g2.setColor(segCol);

                    // Fill gap between segments
                    float connX = Math.min(bx, px);
                    float connY = Math.min(by, py);
                    float connW = Math.abs(bx - px) + cellSize;
                    float connH = Math.abs(by - py) + cellSize;
                    float cInset = inset;
                    g2.fill(new RoundRectangle2D.Float(
                            connX + cInset, connY + cInset,
                            connW - cInset * 2, connH - cInset * 2,
                            cellSize * 0.3f, cellSize * 0.3f));
                }

                // Draw segment body
                Color segColor = ThemeConstants.withAlpha(player.color, alpha);
                Color segBright = ThemeConstants.withAlpha(
                        brighten(player.color, (int)(60 * ratio)), alpha);

                GradientPaint segGrad = new GradientPaint(
                        bx, by, segBright, bx + cellSize, by + cellSize, segColor);
                g2.setPaint(segGrad);
                g2.fill(new RoundRectangle2D.Float(
                        bx + inset, by + inset,
                        cellSize - inset * 2, cellSize - inset * 2,
                        cellSize * 0.4f, cellSize * 0.4f));

                // Head special rendering
                if (i == 0 && player.alive) {
                    // Head glow
                    for (int gl = 3; gl >= 0; gl--) {
                        g2.setColor(ThemeConstants.withAlpha(player.color, 12 + gl * 15));
                        float extra = gl * 2;
                        g2.fill(new RoundRectangle2D.Float(
                                bx + inset - extra, by + inset - extra,
                                cellSize - inset * 2 + extra * 2, cellSize - inset * 2 + extra * 2,
                                cellSize * 0.45f, cellSize * 0.45f));
                    }

                    // Head body (brighter)
                    GradientPaint headGrad = new GradientPaint(
                            bx, by, brighten(player.color, 80),
                            bx + cellSize, by + cellSize, player.color);
                    g2.setPaint(headGrad);
                    g2.fill(new RoundRectangle2D.Float(
                            bx + inset, by + inset,
                            cellSize - inset * 2, cellSize - inset * 2,
                            cellSize * 0.45f, cellSize * 0.45f));

                    // Eyes
                    drawSnakeEyes(g2, bx, by, cellSize, player.dir);

                    // Highlight shine
                    g2.setColor(new Color(255, 255, 255, 50));
                    float shSize = cellSize * 0.25f;
                    g2.fill(new Ellipse2D.Float(bx + cellSize * 0.2f, by + cellSize * 0.15f, shSize, shSize));
                }

                // Stripe pattern on body (every 3rd segment)
                if (i > 0 && i % 3 == 0 && player.alive) {
                    g2.setColor(new Color(255, 255, 255, 20));
                    g2.fill(new RoundRectangle2D.Float(
                            bx + inset + 2, by + inset + 2,
                            cellSize - inset * 2 - 4, cellSize - inset * 2 - 4,
                            cellSize * 0.3f, cellSize * 0.3f));
                }
            }

            // Dead snake X on head
            if (!player.alive && len > 0) {
                Point head = bodyArr[0];
                float hx = offsetX + head.x * cellSize + cellSize / 2f;
                float hy = offsetY + head.y * cellSize + cellSize / 2f;
                g2.setColor(ThemeConstants.NEON_RED);
                g2.setStroke(new BasicStroke(2.5f));
                float sz = cellSize * 0.25f;
                g2.draw(new Line2D.Float(hx - sz, hy - sz, hx + sz, hy + sz));
                g2.draw(new Line2D.Float(hx + sz, hy - sz, hx - sz, hy + sz));
                g2.setStroke(new BasicStroke(1));
            }
        }

        // Particles
        for (float[] p : engine.particles) {
            int alpha = (int)(p[4] * 200);
            if (alpha <= 0) continue;
            g2.setColor(new Color((int) p[5], (int) p[6], (int) p[7], alpha));
            int px = offsetX + (int)(p[0] * cellSize) + cellSize / 2;
            int py = offsetY + (int)(p[1] * cellSize) + cellSize / 2;
            int ps = Math.max(2, (int)(cellSize * 0.3f * p[4]));
            g2.fillOval(px - ps / 2, py - ps / 2, ps, ps);
        }
    }

    private void drawSnakeEyes(Graphics2D g2, float bx, float by, int cs, SnakePlayer.Direction dir) {
        int eyeSize = Math.max(3, cs / 4);
        int pupilSize = Math.max(1, eyeSize / 2);
        float ex1, ey1, ex2, ey2;

        switch (dir) {
            case UP:
                ex1 = bx + cs * 0.28f; ex2 = bx + cs * 0.72f - eyeSize;
                ey1 = ey2 = by + cs * 0.2f;
                break;
            case DOWN:
                ex1 = bx + cs * 0.28f; ex2 = bx + cs * 0.72f - eyeSize;
                ey1 = ey2 = by + cs * 0.6f;
                break;
            case LEFT:
                ex1 = ex2 = bx + cs * 0.15f;
                ey1 = by + cs * 0.22f; ey2 = by + cs * 0.62f;
                break;
            default: // RIGHT
                ex1 = ex2 = bx + cs * 0.65f;
                ey1 = by + cs * 0.22f; ey2 = by + cs * 0.62f;
                break;
        }

        // Eye whites
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Float(ex1, ey1, eyeSize, eyeSize));
        g2.fill(new Ellipse2D.Float(ex2, ey2, eyeSize, eyeSize));

        // Pupils (offset toward direction)
        float pdx = 0, pdy = 0;
        switch (dir) {
            case UP: pdy = -1; break;
            case DOWN: pdy = 1; break;
            case LEFT: pdx = -1; break;
            case RIGHT: pdx = 1; break;
        }
        g2.setColor(new Color(20, 20, 30));
        float pOff = (eyeSize - pupilSize) / 2f;
        g2.fill(new Ellipse2D.Float(ex1 + pOff + pdx, ey1 + pOff + pdy, pupilSize, pupilSize));
        g2.fill(new Ellipse2D.Float(ex2 + pOff + pdx, ey2 + pOff + pdy, pupilSize, pupilSize));
    }

    private Color brighten(Color c, int amount) {
        return new Color(
            Math.min(255, c.getRed() + amount),
            Math.min(255, c.getGreen() + amount),
            Math.min(255, c.getBlue() + amount));
    }
}
