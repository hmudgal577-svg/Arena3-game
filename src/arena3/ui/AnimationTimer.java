package arena3.ui;

import javax.swing.Timer;

public class AnimationTimer {
    private Timer timer;
    private boolean running;

    public AnimationTimer(int fps, Runnable onTick) {
        int delay = 1000 / Math.max(1, fps);
        timer = new Timer(delay, e -> onTick.run());
        timer.setCoalesce(true);
        running = false;
    }

    public void start() { timer.start(); running = true; }
    public void stop() { timer.stop(); running = false; }
    public boolean isRunning() { return running; }

    public void setFPS(int fps) {
        int delay = 1000 / Math.max(1, fps);
        timer.setDelay(delay);
    }
}
