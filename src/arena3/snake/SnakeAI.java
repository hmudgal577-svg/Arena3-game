package arena3.snake;

import java.awt.*;
import java.util.*;

public class SnakeAI {
    private final int difficulty; // 0=Easy, 1=Medium, 2=Hard
    private int tickCounter = 0;
    private SnakePlayer.Direction cachedDir = null;

    public SnakeAI(int difficulty) {
        this.difficulty = difficulty;
    }

    public SnakePlayer.Direction getNextMove(SnakePlayer ai, Point food,
                                              java.util.List<SnakePlayer> allSnakes,
                                              int width, int height) {
        tickCounter++;

        // Easy: recalculate every 5 ticks, Medium: every 3, Hard: every tick
        int recalcInterval = difficulty == 0 ? 5 : difficulty == 1 ? 3 : 1;

        // Medium: 20% random moves
        if (difficulty == 1 && Math.random() < 0.2) {
            SnakePlayer.Direction[] dirs = SnakePlayer.Direction.values();
            SnakePlayer.Direction d = dirs[new Random().nextInt(4)];
            if (isSafe(ai, d, allSnakes, width, height)) return d;
        }

        if (cachedDir != null && tickCounter % recalcInterval != 0) {
            if (isSafe(ai, cachedDir, allSnakes, width, height)) return cachedDir;
        }

        // BFS to food
        Set<Point> obstacles = new HashSet<>();
        for (SnakePlayer s : allSnakes) {
            if (s.alive) {
                for (Point p : s.body) obstacles.add(new Point(p));
            }
        }
        // Remove AI's own head from obstacles
        obstacles.remove(ai.getHead());

        SnakePlayer.Direction bfsResult = bfs(ai.getHead(), food, obstacles, width, height);

        if (bfsResult != null) {
            cachedDir = bfsResult;
            return bfsResult;
        }

        // Survival mode: pick direction with most open space
        cachedDir = survivalMove(ai, allSnakes, width, height);
        return cachedDir;
    }

    private SnakePlayer.Direction bfs(Point start, Point target, Set<Point> obstacles,
                                       int width, int height) {
        if (start.equals(target)) return null;

        Queue<Point> queue = new LinkedList<>();
        Map<Point, Point> parent = new HashMap<>();
        Map<Point, SnakePlayer.Direction> firstDir = new HashMap<>();

        queue.add(start);
        parent.put(start, null);

        int[][] deltas = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        SnakePlayer.Direction[] dirs = {
                SnakePlayer.Direction.UP, SnakePlayer.Direction.DOWN,
                SnakePlayer.Direction.LEFT, SnakePlayer.Direction.RIGHT
        };

        while (!queue.isEmpty()) {
            Point curr = queue.poll();
            for (int i = 0; i < 4; i++) {
                Point next = new Point(curr.x + deltas[i][0], curr.y + deltas[i][1]);
                if (next.x < 0 || next.x >= width || next.y < 0 || next.y >= height) continue;
                if (obstacles.contains(next)) continue;
                if (parent.containsKey(next)) continue;

                parent.put(next, curr);
                if (curr.equals(start)) {
                    firstDir.put(next, dirs[i]);
                } else {
                    firstDir.put(next, firstDir.get(curr));
                }

                if (next.equals(target)) {
                    return firstDir.get(next);
                }
                queue.add(next);
            }
        }
        return null;
    }

    private SnakePlayer.Direction survivalMove(SnakePlayer ai,
                                                java.util.List<SnakePlayer> allSnakes,
                                                int width, int height) {
        SnakePlayer.Direction[] dirs = SnakePlayer.Direction.values();
        SnakePlayer.Direction best = ai.dir;
        int bestSpace = -1;

        Set<Point> obstacles = new HashSet<>();
        for (SnakePlayer s : allSnakes) {
            if (s.alive) for (Point p : s.body) obstacles.add(new Point(p));
        }

        for (SnakePlayer.Direction d : dirs) {
            Point nh = movePoint(ai.getHead(), d);
            if (nh.x < 0 || nh.x >= width || nh.y < 0 || nh.y >= height) continue;
            if (obstacles.contains(nh)) continue;

            // Flood fill to count reachable space
            int space = floodFill(nh, obstacles, width, height);
            if (space > bestSpace) {
                bestSpace = space;
                best = d;
            }
        }
        return best;
    }

    private int floodFill(Point start, Set<Point> obstacles, int width, int height) {
        Set<Point> visited = new HashSet<>();
        Queue<Point> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);
        int count = 0;
        int maxCheck = 200; // Limit for performance

        while (!queue.isEmpty() && count < maxCheck) {
            Point p = queue.poll();
            count++;
            int[][] deltas = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
            for (int[] d : deltas) {
                Point n = new Point(p.x + d[0], p.y + d[1]);
                if (n.x >= 0 && n.x < width && n.y >= 0 && n.y < height
                        && !obstacles.contains(n) && !visited.contains(n)) {
                    visited.add(n);
                    queue.add(n);
                }
            }
        }
        return count;
    }

    private boolean isSafe(SnakePlayer ai, SnakePlayer.Direction d,
                           java.util.List<SnakePlayer> allSnakes, int w, int h) {
        Point nh = movePoint(ai.getHead(), d);
        if (nh.x < 0 || nh.x >= w || nh.y < 0 || nh.y >= h) return false;
        for (SnakePlayer s : allSnakes) {
            if (s.alive && s.occupies(nh)) return false;
        }
        return true;
    }

    private Point movePoint(Point p, SnakePlayer.Direction d) {
        switch (d) {
            case UP: return new Point(p.x, p.y - 1);
            case DOWN: return new Point(p.x, p.y + 1);
            case LEFT: return new Point(p.x - 1, p.y);
            case RIGHT: return new Point(p.x + 1, p.y);
        }
        return p;
    }
}
