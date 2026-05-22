package arena3.core;

import java.awt.Color;
import arena3.ui.ThemeConstants;

public class GameState {
    public enum GameType { SNAKE, LUDO, CHESS }

    public static class GameConfig {
        public GameType gameType;
        public int playerCount = 1;
        public boolean[] isAI = {false, true, true, true};
        public int aiDifficulty = 1; // 0=Easy, 1=Medium, 2=Hard
        public Color[] playerColors = {
            ThemeConstants.NEON_GREEN, ThemeConstants.NEON_CYAN,
            ThemeConstants.LUDO_YELLOW, ThemeConstants.LUDO_BLUE
        };
        // Snake
        public int boardSize = 30; // 20, 30, 40
        public int gameSpeed = 1;  // 0=Slow, 1=Normal, 2=Fast

        // Chess
        public boolean playAsWhite = true;
        public boolean showHints = true;
        public int timerMinutes = 0; // 0=none, 5, 10

        // Ludo
        public int ludoPlayerCount = 4;

        public GameConfig copy() {
            GameConfig c = new GameConfig();
            c.gameType = gameType;
            c.playerCount = playerCount;
            c.isAI = isAI.clone();
            c.aiDifficulty = aiDifficulty;
            c.playerColors = playerColors.clone();
            c.boardSize = boardSize;
            c.gameSpeed = gameSpeed;
            c.playAsWhite = playAsWhite;
            c.showHints = showHints;
            c.timerMinutes = timerMinutes;
            c.ludoPlayerCount = ludoPlayerCount;
            return c;
        }
    }

    public static class GameResult {
        public String winnerName = "";
        public String message = "";
        public GameType gameType;
        public GameConfig config;
        public java.util.Map<String, String> stats = new java.util.LinkedHashMap<>();
    }

    public static GameConfig currentConfig = new GameConfig();
    public static GameType currentGame = null;
    public static boolean soundMuted = false;
}
