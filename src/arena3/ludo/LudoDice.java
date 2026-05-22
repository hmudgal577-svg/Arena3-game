package arena3.ludo;

import javax.swing.*;
import java.awt.*;
import java.util.Random;
import arena3.ui.ThemeConstants;

public class LudoDice {
    private int currentValue = 1;
    private boolean animating = false;
    private final Random rng = new Random();
    private Timer animTimer;
    private int animCount = 0;

    public int getValue() { return currentValue; }
    public boolean isAnimating() { return animating; }

    public void roll(Runnable onComplete) {
        animating = true;
        animCount = 0;
        arena3.core.SoundEngine.playDiceRoll();

        animTimer = new Timer(60, e -> {
            currentValue = rng.nextInt(6) + 1;
            animCount++;
            if (animCount >= 10) {
                animTimer.stop();
                animating = false;
                if (onComplete != null) onComplete.run();
            }
        });
        animTimer.start();
    }

    public void paint(Graphics2D g2, int x, int y, int size) {
        ThemeConstants.enableAntialiasing(g2);

        // Dice body
        if (animating) {
            // Slight shake
            x += (int)(Math.random() * 4 - 2);
            y += (int)(Math.random() * 4 - 2);
        }

        // Shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(x + 3, y + 3, size, size, 12, 12);

        // Dice face
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x, y, size, size, 12, 12);
        g2.setColor(new Color(200, 200, 200));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, size, size, 12, 12);

        // Dots
        g2.setColor(new Color(30, 30, 30));
        int dotR = size / 8;
        int cx = x + size / 2;
        int cy = y + size / 2;
        int off = size / 4;

        switch (currentValue) {
            case 1:
                drawDot(g2, cx, cy, dotR);
                break;
            case 2:
                drawDot(g2, cx - off, cy - off, dotR);
                drawDot(g2, cx + off, cy + off, dotR);
                break;
            case 3:
                drawDot(g2, cx - off, cy - off, dotR);
                drawDot(g2, cx, cy, dotR);
                drawDot(g2, cx + off, cy + off, dotR);
                break;
            case 4:
                drawDot(g2, cx - off, cy - off, dotR);
                drawDot(g2, cx + off, cy - off, dotR);
                drawDot(g2, cx - off, cy + off, dotR);
                drawDot(g2, cx + off, cy + off, dotR);
                break;
            case 5:
                drawDot(g2, cx - off, cy - off, dotR);
                drawDot(g2, cx + off, cy - off, dotR);
                drawDot(g2, cx, cy, dotR);
                drawDot(g2, cx - off, cy + off, dotR);
                drawDot(g2, cx + off, cy + off, dotR);
                break;
            case 6:
                drawDot(g2, cx - off, cy - off, dotR);
                drawDot(g2, cx + off, cy - off, dotR);
                drawDot(g2, cx - off, cy, dotR);
                drawDot(g2, cx + off, cy, dotR);
                drawDot(g2, cx - off, cy + off, dotR);
                drawDot(g2, cx + off, cy + off, dotR);
                break;
        }
        g2.setStroke(new BasicStroke(1));
    }

    private void drawDot(Graphics2D g2, int x, int y, int r) {
        g2.fillOval(x - r, y - r, r * 2, r * 2);
    }
}
