package arena3.menu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import arena3.core.GameLauncher;
import arena3.core.GameState.GameType;
import arena3.ui.ThemeConstants;

public class MainMenuScreen extends JPanel {
    private float tick = 0;
    private final Timer animTimer;
    private final GameCard[] cards = new GameCard[3];

    public MainMenuScreen() {
        setLayout(new BorderLayout());
        setBackground(ThemeConstants.BG_DARK);
        setOpaque(true);

        // Title panel
        JPanel titlePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                ThemeConstants.enableAntialiasing(g2);
                g2.setColor(ThemeConstants.BG_DARK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Title with glow
                g2.setFont(ThemeConstants.FONT_TITLE.deriveFont(64f));
                String title = "ARENA 3";
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(title)) / 2;
                int ty = 80;

                // Flicker effect
                float flicker = 0.85f + 0.15f * (float) Math.sin(tick * 0.15);
                Color gold = ThemeConstants.withAlpha(ThemeConstants.NEON_GOLD, (int)(255 * flicker));
                ThemeConstants.drawTextGlow(g2, title, tx, ty, gold);

                // Subtitle
                g2.setFont(ThemeConstants.FONT_SUBHEAD);
                String sub = "CHOOSE YOUR BATTLE";
                fm = g2.getFontMetrics();
                int sx = (getWidth() - fm.stringWidth(sub)) / 2;
                g2.setColor(ThemeConstants.NEON_CYAN);
                g2.drawString(sub, sx, ty + 45);
            }
        };
        titlePanel.setPreferredSize(new Dimension(0, 150));
        titlePanel.setOpaque(false);

        // Cards panel
        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 30, 0));
        cardsPanel.setOpaque(false);
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(20, 60, 20, 60));

        cards[0] = new GameCard("SNAKE", "Eat. Grow. Survive.",
                ThemeConstants.NEON_GREEN, GameType.SNAKE, 0);
        cards[1] = new GameCard("LUDO", "Roll. Race. Conquer.",
                ThemeConstants.NEON_GOLD, GameType.LUDO, 1);
        cards[2] = new GameCard("CHESS", "Think. Plan. Dominate.",
                ThemeConstants.NEON_CYAN, GameType.CHESS, 2);

        for (GameCard c : cards) cardsPanel.add(c);

        // Footer
        JPanel footer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                ThemeConstants.enableAntialiasing(g2);
                g2.setColor(ThemeConstants.BG_DARK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setFont(ThemeConstants.FONT_SMALL);
                g2.setColor(ThemeConstants.TEXT_DIM);
                String txt = "Solo = Play vs AI  |  Multiplayer = Play vs Friends";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(txt, (getWidth() - fm.stringWidth(txt)) / 2, 30);
            }
        };
        footer.setPreferredSize(new Dimension(0, 50));
        footer.setOpaque(false);

        add(titlePanel, BorderLayout.NORTH);
        add(cardsPanel, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        animTimer = new Timer(16, e -> { tick += 1; repaint(); });
    }

    public void onShow() {
        animTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        ThemeConstants.enableAntialiasing(g2);
        g2.setColor(ThemeConstants.BG_DARK);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Animated grid background
        g2.setColor(new Color(30, 30, 50, 40));
        int gridSize = 40;
        float offset = tick % gridSize;
        for (int x = (int) -offset; x < getWidth() + gridSize; x += gridSize) {
            g2.drawLine(x, 0, x, getHeight());
        }
        for (int y = (int) -offset; y < getHeight() + gridSize; y += gridSize) {
            g2.drawLine(0, y, getWidth(), y);
        }
    }

    // --- Inner class: Game Card ---
    private class GameCard extends JPanel {
        private final String name, tagline;
        private final Color accent;
        private final GameType type;
        private final int iconType;
        private boolean hovered = false;

        GameCard(String name, String tagline, Color accent, GameType type, int iconType) {
            this.name = name;
            this.tagline = tagline;
            this.accent = accent;
            this.type = type;
            this.iconType = iconType;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setLayout(new BorderLayout());

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) {
                    animTimer.stop();
                    GameLauncher.getInstance().showPlayerSetup(type);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            ThemeConstants.enableAntialiasing(g2);

            int w = getWidth(), h = getHeight();
            int arc = 20;

            // Background
            g2.setColor(ThemeConstants.BG_CARD);
            g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);

            // Glowing border
            Color borderColor = hovered ? ThemeConstants.brighter(accent, 1.5f) : accent;
            int glowLayers = hovered ? 4 : 2;
            for (int i = glowLayers; i >= 0; i--) {
                g2.setColor(ThemeConstants.withAlpha(borderColor, 30 + (glowLayers - i) * 40));
                g2.setStroke(new BasicStroke(2 + i * 2));
                g2.drawRoundRect(1 + i, 1 + i, w - 2 - i * 2, h - 2 - i * 2, arc, arc);
            }

            // Icon area
            int iconY = 40;
            int iconSize = 80;
            int cx = w / 2;
            g2.setColor(accent);
            drawIcon(g2, cx, iconY, iconSize);

            // Name
            g2.setFont(ThemeConstants.FONT_HEADING);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(ThemeConstants.TEXT_PRIMARY);
            g2.drawString(name, (w - fm.stringWidth(name)) / 2, iconY + iconSize + 40);

            // Tagline
            g2.setFont(ThemeConstants.FONT_SMALL);
            fm = g2.getFontMetrics();
            g2.setColor(ThemeConstants.TEXT_DIM);
            g2.drawString(tagline, (w - fm.stringWidth(tagline)) / 2, iconY + iconSize + 65);

            // SELECT button area
            int btnW = 140, btnH = 40;
            int btnX = (w - btnW) / 2, btnY = h - 70;
            Color btnBg = hovered ? ThemeConstants.brighter(accent, 1.3f) : accent;
            g2.setColor(ThemeConstants.withAlpha(btnBg, 200));
            g2.fillRoundRect(btnX, btnY, btnW, btnH, 10, 10);
            g2.setColor(ThemeConstants.BG_DARK);
            g2.setFont(ThemeConstants.FONT_BODY);
            fm = g2.getFontMetrics();
            String sel = "SELECT";
            g2.drawString(sel, btnX + (btnW - fm.stringWidth(sel)) / 2,
                    btnY + (btnH + fm.getAscent() - fm.getDescent()) / 2);
        }

        private void drawIcon(Graphics2D g2, int cx, int y, int size) {
            g2.setStroke(new BasicStroke(3));
            if (iconType == 0) {
                // Snake: zigzag line
                int sx = cx - size / 2;
                int sy = y + size / 2;
                int seg = size / 5;
                for (int i = 0; i < 5; i++) {
                    int x1 = sx + i * seg, y1 = sy + (i % 2 == 0 ? -15 : 15);
                    int x2 = sx + (i + 1) * seg, y2 = sy + ((i + 1) % 2 == 0 ? -15 : 15);
                    g2.drawLine(x1, y1, x2, y2);
                }
                g2.fillOval(sx + size - 6, sy + 15 - 6, 12, 12);
            } else if (iconType == 1) {
                // Ludo: dice face
                int dx = cx - 25, dy = y + 15;
                g2.drawRoundRect(dx, dy, 50, 50, 8, 8);
                g2.fillOval(dx + 10, dy + 10, 8, 8);
                g2.fillOval(dx + 32, dy + 10, 8, 8);
                g2.fillOval(dx + 21, dy + 21, 8, 8);
                g2.fillOval(dx + 10, dy + 32, 8, 8);
                g2.fillOval(dx + 32, dy + 32, 8, 8);
            } else {
                // Chess: crown shape
                int crx = cx - 25, cry = y + 20;
                int[] px = {crx, crx + 12, crx + 25, crx + 38, crx + 50};
                int[] py = {cry + 40, cry, cry + 20, cry, cry + 40};
                g2.drawPolyline(px, py, 5);
                g2.drawLine(crx, cry + 40, crx + 50, cry + 40);
                g2.drawLine(crx, cry + 46, crx + 50, cry + 46);
            }
        }
    }
}
