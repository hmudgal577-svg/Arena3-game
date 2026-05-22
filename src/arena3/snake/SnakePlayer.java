package arena3.snake;

import java.awt.*;
import java.util.ArrayDeque;

public class SnakePlayer {
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    public ArrayDeque<Point> body = new ArrayDeque<>();
    public Color color;
    public Direction dir = Direction.RIGHT;
    public Direction nextDir = Direction.RIGHT;
    public int score = 0;
    public boolean alive = true;
    public boolean isHuman;
    public String name;

    public SnakePlayer(String name, Color color, boolean isHuman, Point start, int length) {
        this(name, color, isHuman, start, length, Direction.RIGHT);
    }

    public SnakePlayer(String name, Color color, boolean isHuman, Point start, int length, Direction initialDir) {
        this.name = name;
        this.color = color;
        this.isHuman = isHuman;
        this.dir = initialDir;
        this.nextDir = initialDir;
        // Build body: head at start, segments extend OPPOSITE to direction
        int dx = 0, dy = 0;
        switch (initialDir) {
            case RIGHT: dx = -1; break;
            case LEFT:  dx = 1;  break;
            case UP:    dy = 1;  break;
            case DOWN:  dy = -1; break;
        }
        for (int i = 0; i < length; i++) {
            body.addLast(new Point(start.x + i * dx, start.y + i * dy));
        }
    }

    public Point getHead() { return body.peekFirst(); }

    public void setDirection(Direction d) {
        // Prevent reversing
        if (d == Direction.UP && dir == Direction.DOWN) return;
        if (d == Direction.DOWN && dir == Direction.UP) return;
        if (d == Direction.LEFT && dir == Direction.RIGHT) return;
        if (d == Direction.RIGHT && dir == Direction.LEFT) return;
        this.nextDir = d;
    }

    public void applyDirection() { dir = nextDir; }

    public Point nextHead() {
        Point h = getHead();
        switch (dir) {
            case UP: return new Point(h.x, h.y - 1);
            case DOWN: return new Point(h.x, h.y + 1);
            case LEFT: return new Point(h.x - 1, h.y);
            case RIGHT: return new Point(h.x + 1, h.y);
        }
        return h;
    }

    public boolean occupies(Point p) {
        for (Point bp : body) {
            if (bp.equals(p)) return true;
        }
        return false;
    }

    public boolean collidesWithSelf() {
        Point head = getHead();
        boolean first = true;
        for (Point bp : body) {
            if (first) { first = false; continue; }
            if (bp.equals(head)) return true;
        }
        return false;
    }
}
