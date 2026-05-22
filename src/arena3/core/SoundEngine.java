package arena3.core;

import javax.sound.sampled.*;
import arena3.core.GameState;

public class SoundEngine {
    private static final int SAMPLE_RATE = 44100;

    private static void playTone(float freq, int durationMs, float volume) {
        if (GameState.soundMuted) return;
        new Thread(() -> {
            try {
                int samples = SAMPLE_RATE * durationMs / 1000;
                byte[] buf = new byte[samples];
                for (int i = 0; i < samples; i++) {
                    double angle = 2.0 * Math.PI * i / (SAMPLE_RATE / freq);
                    double envelope = 1.0;
                    if (i > samples * 0.7) envelope = (samples - i) / (double)(samples * 0.3);
                    buf[i] = (byte)(Math.sin(angle) * 127 * volume * envelope);
                }
                AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(fmt);
                line.start();
                line.write(buf, 0, buf.length);
                line.drain();
                line.close();
            } catch (Exception ignored) {}
        }).start();
    }

    private static void playTones(float[] freqs, int[] durations, float volume) {
        if (GameState.soundMuted) return;
        new Thread(() -> {
            for (int t = 0; t < freqs.length; t++) {
                try {
                    int samples = SAMPLE_RATE * durations[t] / 1000;
                    byte[] buf = new byte[samples];
                    for (int i = 0; i < samples; i++) {
                        double angle = 2.0 * Math.PI * i / (SAMPLE_RATE / freqs[t]);
                        double env = 1.0;
                        if (i > samples * 0.7) env = (samples - i) / (double)(samples * 0.3);
                        buf[i] = (byte)(Math.sin(angle) * 127 * volume * env);
                    }
                    AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
                    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(fmt);
                    line.start();
                    line.write(buf, 0, buf.length);
                    line.drain();
                    line.close();
                } catch (Exception ignored) {}
            }
        }).start();
    }

    // Snake sounds
    public static void playEat() {
        playTones(new float[]{800, 1200}, new int[]{80, 80}, 0.4f);
    }

    public static void playDeath() {
        playTones(new float[]{400, 200}, new int[]{200, 300}, 0.5f);
    }

    // Ludo sounds
    public static void playDiceRoll() {
        if (GameState.soundMuted) return;
        new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                float f = 300 + (float)(Math.random() * 500);
                try {
                    int samples = SAMPLE_RATE * 50 / 1000;
                    byte[] buf = new byte[samples];
                    for (int j = 0; j < samples; j++) {
                        double angle = 2.0 * Math.PI * j / (SAMPLE_RATE / f);
                        buf[j] = (byte)(Math.sin(angle) * 80);
                    }
                    AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
                    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(fmt);
                    line.start();
                    line.write(buf, 0, buf.length);
                    line.drain();
                    line.close();
                } catch (Exception ignored) {}
            }
        }).start();
    }

    public static void playTokenMove() {
        playTone(600, 60, 0.3f);
    }

    public static void playCapture() {
        playTones(new float[]{300, 150}, new int[]{150, 200}, 0.5f);
    }

    // Chess sounds
    public static void playChessMove() {
        playTone(700, 80, 0.3f);
    }

    public static void playChessCapture() {
        playTones(new float[]{400, 250}, new int[]{100, 150}, 0.4f);
    }

    public static void playCheck() {
        playTones(new float[]{900, 900}, new int[]{100, 100}, 0.5f);
    }

    public static void playWin() {
        playTones(new float[]{523, 659, 784, 1046}, new int[]{150, 150, 150, 300}, 0.5f);
    }
}
