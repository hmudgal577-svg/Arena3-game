package arena3.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import arena3.core.GameLauncher;
import arena3.core.GameState;
import arena3.core.GameState.*;

public class GameOverScreen extends JPanel {
    private GameResult result;
    private final java.util.List<Particle> particles = new ArrayList<>();
    private final Timer animTimer;
    private final Random rng = new Random();

    public GameOverScreen() {
        setBackground(ThemeConstants.BG_DARK);
        setLayout(new GridBagLayout());

        animTimer = new Timer(16, e -> {
            for (Particle p : particles) p.update();
            particles.removeIf(p -> p.y > 1000);
            repaint();
        });
    }

    public void setResult(GameResult result) {
        this.result = result;
        removeAll();
        particles.clear();
        spawnConfetti();
        buildUI();
        animTimer.start();
        revalidate();
        repaint();
    }

    private void spawnConfetti() {
        Color[] colors = {ThemeConstants.NEON_GOLD, ThemeConstants.NEON_CYAN,
                ThemeConstants.NEON_GREEN, ThemeConstants.NEON_RED,
                ThemeConstants.NEON_PURPLE, Color.WHITE};
        for (int i = 0; i < 120; i++) {
            particles.add(new Particle(
                    rng.nextInt(1200), -rng.nextInt(400),
                    (rng.nextFloat() - 0.5f) * 6, rng.nextFloat() * 2 + 1,
                    colors[rng.nextInt(colors.length)],
                    rng.nextInt(6) + 4, rng.nextFloat() * 360
            ));
        }
    }

    private void buildUI() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                ThemeConstants.enableAntialiasing(g2);
                g2.setColor(new Color(15, 15, 25, 230));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                ThemeConstants.drawGlow(g2, 0, 0, getWidth(), getHeight(),
                        ThemeConstants.NEON_GOLD, 2);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        card.setPreferredSize(new Dimension(550, 420));

        // Title
        JLabel title = new JLabel("GAME OVER");
        title.setFont(ThemeConstants.FONT_TITLE);
        title.setForeground(ThemeConstants.NEON_GOLD);
        title.setAlignmentX(CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(15));

        // Winner
        if (result != null) {
            JLabel winner = new JLabel("\uD83C\uDFC6 " + result.winnerName);
            winner.setFont(ThemeConstants.FONT_HEADING);
            winner.setForeground(ThemeConstants.TEXT_PRIMARY);
            winner.setAlignmentX(CENTER_ALIGNMENT);
            card.add(winner);
            card.add(Box.createVerticalStrut(5));

            if (!result.message.isEmpty()) {
                JLabel msg = new JLabel(result.message);
                msg.setFont(ThemeConstants.FONT_BODY);
                msg.setForeground(ThemeConstants.TEXT_DIM);
                msg.setAlignmentX(CENTER_ALIGNMENT);
                card.add(msg);
            }
            card.add(Box.createVerticalStrut(20));

            // Stats
            for (Map.Entry<String, String> entry : result.stats.entrySet()) {
                JLabel stat = new JLabel(entry.getKey() + ": " + entry.getValue());
                stat.setFont(ThemeConstants.FONT_BODY);
                stat.setForeground(ThemeConstants.NEON_CYAN);
                stat.setAlignmentX(CENTER_ALIGNMENT);
                card.add(stat);
                card.add(Box.createVerticalStrut(4));
            }
        }

        card.add(Box.createVerticalStrut(25));

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        btnPanel.setOpaque(false);

        JButton playAgain = ThemeConstants.makeStyledButton("PLAY AGAIN", ThemeConstants.NEON_GREEN, ThemeConstants.BG_DARK);
        playAgain.addActionListener(e -> {
            animTimer.stop();
            if (result != null && result.config != null)
                GameLauncher.getInstance().startGame(result.config);
        });

        JButton settings = ThemeConstants.makeStyledButton("SETTINGS", ThemeConstants.NEON_CYAN, ThemeConstants.BG_DARK);
        settings.addActionListener(e -> {
            animTimer.stop();
            if (result != null)
                GameLauncher.getInstance().showPlayerSetup(result.gameType);
        });

        JButton menu = ThemeConstants.makeStyledButton("MAIN MENU", ThemeConstants.NEON_GOLD, ThemeConstants.BG_DARK);
        menu.addActionListener(e -> {
            animTimer.stop();
            GameLauncher.getInstance().showMainMenu();
        });

        btnPanel.add(playAgain);
        btnPanel.add(settings);
        btnPanel.add(menu);
        card.add(btnPanel);

        add(card);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(ThemeConstants.BG_DARK);
        g2.fillRect(0, 0, getWidth(), getHeight());
        ThemeConstants.enableAntialiasing(g2);

        for (Particle p : particles) {
            g2.setColor(ThemeConstants.withAlpha(p.color, 200));
            g2.rotate(Math.toRadians(p.rotation), p.x, p.y);
            g2.fillRect((int) p.x, (int) p.y, p.size, p.size / 2);
            g2.rotate(-Math.toRadians(p.rotation), p.x, p.y);
        }
    }

    private static class Particle {
        float x, y, vx, vy;
        Color color;
        int size;
        float rotation, rotSpeed;

        Particle(float x, float y, float vx, float vy, Color color, int size, float rot) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.color = color; this.size = size;
            this.rotation = rot; this.rotSpeed = (float)(Math.random() - 0.5) * 10;
        }

        void update() {
            x += vx; y += vy; vy += 0.15f;
            rotation += rotSpeed;
        }
    }
}
