package arena3.snake;

import java.awt.*;
import java.util.*;
import java.util.List;
import arena3.core.SoundEngine;

public class SnakeEngine {
    private final int width, height;
    private final List<SnakePlayer> players = new ArrayList<>();
    private Point food;
    private final Random rng = new Random();
    private boolean gameOver = false;
    private String gameOverMessage = "";
    private int tickCount = 0;

    // Particle effects
    public final List<float[]> particles = new ArrayList<>(); // x,y,vx,vy,life,r,g,b

    public SnakeEngine(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void addPlayer(SnakePlayer p) { players.add(p); }
    public List<SnakePlayer> getPlayers() { return players; }
    public Point getFood() { return food; }
    public boolean isGameOver() { return gameOver; }
    public String getGameOverMessage() { return gameOverMessage; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void init() {
        spawnFood();
    }

    public void spawnFood() {
        Set<Point> occupied = new HashSet<>();
        for (SnakePlayer p : players) {
            if (p.alive) for (Point bp : p.body) occupied.add(bp);
        }
        int attempts = 0;
        do {
            food = new Point(rng.nextInt(width), rng.nextInt(height));
            attempts++;
        } while (occupied.contains(food) && attempts < 1000);
    }

    public void tick() {
        if (gameOver) return;
        tickCount++;

        // Apply directions
        for (SnakePlayer p : players) {
            if (p.alive) p.applyDirection();
        }

        // Move all snakes
        for (SnakePlayer p : players) {
            if (!p.alive) continue;
            Point newHead = p.nextHead();

            // Wall collision
            if (newHead.x < 0 || newHead.x >= width || newHead.y < 0 || newHead.y >= height) {
                killPlayer(p, p.name + " hit a wall!");
                continue;
            }

            // Move head
            p.body.addFirst(new Point(newHead.x, newHead.y));

            // Food check
            if (newHead.equals(food)) {
                p.score += 10;
                SoundEngine.playEat();
                spawnParticles(newHead);
                spawnFood();
            } else {
                p.body.removeLast();
            }
        }

        // Self collision check
        for (SnakePlayer p : players) {
            if (!p.alive) continue;
            if (p.collidesWithSelf()) {
                killPlayer(p, p.name + " ate itself!");
            }
        }

        // Snake vs snake collision
        for (int i = 0; i < players.size(); i++) {
            SnakePlayer a = players.get(i);
            if (!a.alive) continue;
            for (int j = 0; j < players.size(); j++) {
                if (i == j) continue;
                SnakePlayer b = players.get(j);
                if (!b.alive) continue;
                // Check if a's head hit b's body
                Point ahead = a.getHead();
                boolean first = true;
                for (Point bp : b.body) {
                    if (bp.equals(ahead)) {
                        if (first && a.getHead().equals(b.getHead())) {
                            // Head-on collision: both die or smaller dies
                            if (a.body.size() <= b.body.size()) killPlayer(a, a.name + " collided!");
                            if (b.body.size() <= a.body.size()) killPlayer(b, b.name + " collided!");
                        } else {
                            killPlayer(a, a.name + " hit " + b.name + "!");
                        }
                        break;
                    }
                    first = false;
                }
            }
        }

        // Update particles
        Iterator<float[]> it = particles.iterator();
        while (it.hasNext()) {
            float[] p = it.next();
            p[0] += p[2]; p[1] += p[3]; p[4] -= 0.03f;
            if (p[4] <= 0) it.remove();
        }

        // Check game over
        int alive = 0;
        SnakePlayer lastAlive = null;
        for (SnakePlayer p : players) {
            if (p.alive) { alive++; lastAlive = p; }
        }

        if (alive == 0) {
            gameOver = true;
            gameOverMessage = "Draw!";
        } else if (players.size() > 1 && alive == 1) {
            gameOver = true;
            gameOverMessage = lastAlive.name + " wins!";
        } else if (players.size() == 1 && alive == 0) {
            gameOver = true;
            gameOverMessage = "Game Over!";
        }
    }

    private void killPlayer(SnakePlayer p, String msg) {
        if (!p.alive) return;
        p.alive = false;
        gameOverMessage = msg;
        SoundEngine.playDeath();
    }

    private void spawnParticles(Point at) {
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            float vx = (float)(Math.cos(angle) * 3);
            float vy = (float)(Math.sin(angle) * 3);
            particles.add(new float[]{at.x, at.y, vx, vy, 1.0f, 255, 50, 50});
        }
    }
}
