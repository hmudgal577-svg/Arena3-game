package arena3.ui;

import java.awt.*;

public final class ThemeConstants {
    private ThemeConstants() {}

    // Core colors
    public static final Color BG_DARK       = new Color(10, 10, 15);
    public static final Color BG_PANEL      = new Color(20, 20, 30, 200);
    public static final Color BG_CARD       = new Color(25, 25, 40, 220);
    public static final Color BG_LIGHTER    = new Color(30, 30, 50);

    // Neon accents
    public static final Color NEON_GOLD     = new Color(255, 215, 0);
    public static final Color NEON_CYAN     = new Color(0, 255, 255);
    public static final Color NEON_RED      = new Color(255, 34, 68);
    public static final Color NEON_GREEN    = new Color(0, 255, 100);
    public static final Color NEON_BLUE     = new Color(60, 120, 255);
    public static final Color NEON_PURPLE   = new Color(180, 60, 255);

    // Text
    public static final Color TEXT_PRIMARY  = Color.WHITE;
    public static final Color TEXT_DIM      = new Color(150, 150, 180);
    public static final Color TEXT_MUTED    = new Color(80, 80, 110);

    // Chess board
    public static final Color BOARD_LIGHT   = new Color(240, 217, 181);
    public static final Color BOARD_DARK    = new Color(181, 136, 107);

    // Ludo colors
    public static final Color LUDO_RED      = new Color(220, 50, 50);
    public static final Color LUDO_GREEN    = new Color(50, 180, 50);
    public static final Color LUDO_YELLOW   = new Color(230, 200, 40);
    public static final Color LUDO_BLUE     = new Color(50, 100, 220);

    // Fonts
    public static final Font FONT_TITLE     = new Font("Monospaced", Font.BOLD, 48);
    public static final Font FONT_HEADING   = new Font("Monospaced", Font.BOLD, 24);
    public static final Font FONT_SUBHEAD   = new Font("Monospaced", Font.BOLD, 18);
    public static final Font FONT_BODY      = new Font("Monospaced", Font.PLAIN, 16);
    public static final Font FONT_SMALL     = new Font("Monospaced", Font.PLAIN, 12);
    public static final Font FONT_TINY      = new Font("Monospaced", Font.PLAIN, 10);

    // Window
    public static final int WINDOW_WIDTH    = 1200;
    public static final int WINDOW_HEIGHT   = 800;

    // Helpers
    public static void enableAntialiasing(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    public static Color brighter(Color c, float factor) {
        int r = Math.min(255, (int)(c.getRed() * factor));
        int g = Math.min(255, (int)(c.getGreen() * factor));
        int b = Math.min(255, (int)(c.getBlue() * factor));
        return new Color(r, g, b, c.getAlpha());
    }

    public static void drawGlow(Graphics2D g2d, int x, int y, int w, int h, Color color, int layers) {
        for (int i = layers; i >= 0; i--) {
            int alpha = 20 + (255 - 20) * (layers - i) / layers;
            g2d.setColor(withAlpha(color, alpha));
            int pad = i * 3;
            g2d.fillRoundRect(x - pad, y - pad, w + pad * 2, h + pad * 2, 12 + pad, 12 + pad);
        }
    }

    public static void drawTextGlow(Graphics2D g2d, String text, int x, int y, Color color) {
        for (int i = 3; i >= 0; i--) {
            g2d.setColor(withAlpha(color, 30 + i * 20));
            for (int dx = -i; dx <= i; dx++) {
                for (int dy = -i; dy <= i; dy++) {
                    if (dx * dx + dy * dy <= i * i + 1)
                        g2d.drawString(text, x + dx, y + dy);
                }
            }
        }
        g2d.setColor(color);
        g2d.drawString(text, x, y);
    }

    public static javax.swing.JButton makeStyledButton(String text, Color bg, Color fg) {
        javax.swing.JButton btn = new javax.swing.JButton(text);
        btn.setFont(FONT_BODY);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setPreferredSize(new Dimension(180, 45));

        Color hoverBg = brighter(bg, 1.3f);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(hoverBg);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }
}
