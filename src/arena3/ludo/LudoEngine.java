package arena3.ludo;

import java.util.*;
import arena3.core.SoundEngine;

public class LudoEngine {
    public static final int TRACK_SIZE = 52;

    // Board coordinates for the 52 main track positions (row, col on 15x15 grid)
    public static final int[][] MAIN_TRACK = {
        {6,1},{6,2},{6,3},{6,4},{6,5}, // 0-4: Red start stretch (top-left going right)
        {5,6},{4,6},{3,6},{2,6},{1,6},{0,6}, // 5-10: going up
        {0,7},{0,8}, // 11-12: top-right corner
        {1,8},{2,8},{3,8},{4,8},{5,8}, // 13-17: going down
        {6,9},{6,10},{6,11},{6,12},{6,13},{6,14}, // 18-23: going right
        {7,14},{8,14}, // 24-25: bottom-right corner
        {8,13},{8,12},{8,11},{8,10},{8,9}, // 26-30: going left
        {9,8},{10,8},{11,8},{12,8},{13,8},{14,8}, // 31-36: going down
        {14,7},{14,6}, // 37-38: bottom-left corner
        {13,6},{12,6},{11,6},{10,6},{9,6}, // 39-43: going up
        {8,5},{8,4},{8,3},{8,2},{8,1},{8,0}, // 44-49: going left
        {7,0},{6,0} // 50-51: top-left corner back to start
    };

    // Home column paths (6 cells each, leading to center)
    public static final int[][] HOME_COL_RED    = {{7,1},{7,2},{7,3},{7,4},{7,5},{7,6}};
    public static final int[][] HOME_COL_GREEN  = {{1,7},{2,7},{3,7},{4,7},{5,7},{6,7}};
    public static final int[][] HOME_COL_YELLOW = {{7,13},{7,12},{7,11},{7,10},{7,9},{7,8}};
    public static final int[][] HOME_COL_BLUE   = {{13,7},{12,7},{11,7},{10,7},{9,7},{8,7}};

    public static final int[][][] HOME_COLUMNS = {HOME_COL_RED, HOME_COL_GREEN, HOME_COL_YELLOW, HOME_COL_BLUE};

    // Entry positions (absolute track index where each player enters)
    public static final int[] ENTRY_ABS = {0, 13, 26, 39};
    // Position before entering home column (player-relative)
    public static final int HOME_ENTRY_REL = 51;

    private final LudoPlayer[] players;
    private final int playerCount;
    private final LudoDice dice = new LudoDice();
    private final LudoAI ai;

    private int currentPlayerIndex;
    private int diceValue;
    private boolean diceRolled;
    private boolean gameOver;
    private String gameOverMessage = "";
    private int finishCount = 0;
    private boolean extraTurn = false;
    private String statusMessage = "";

    public LudoEngine(LudoPlayer[] players, int playerCount, int aiDifficulty) {
        this.players = players;
        this.playerCount = playerCount;
        this.ai = new LudoAI(aiDifficulty);
        this.currentPlayerIndex = 0;
        this.diceRolled = false;
        this.gameOver = false;
    }

    public LudoPlayer[] getPlayers() { return players; }
    public LudoPlayer getCurrentPlayer() { return players[currentPlayerIndex]; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public LudoDice getDice() { return dice; }
    public int getDiceValue() { return diceValue; }
    public boolean isDiceRolled() { return diceRolled; }
    public boolean isGameOver() { return gameOver; }
    public String getGameOverMessage() { return gameOverMessage; }
    public String getStatusMessage() { return statusMessage; }

    public void rollDice(Runnable onRolled) {
        if (diceRolled || dice.isAnimating()) return;
        dice.roll(() -> {
            diceValue = dice.getValue();
            diceRolled = true;

            List<LudoToken> valid = getValidMoves();
            if (valid.isEmpty()) {
                statusMessage = getCurrentPlayer().name + " has no valid moves";
                // Auto next turn after delay
                javax.swing.Timer t = new javax.swing.Timer(800, e -> {
                    nextTurn();
                    if (onRolled != null) onRolled.run();
                });
                t.setRepeats(false);
                t.start();
            } else {
                statusMessage = getCurrentPlayer().name + " rolled " + diceValue;
                if (onRolled != null) onRolled.run();
            }
        });
    }

    public List<LudoToken> getValidMoves() {
        List<LudoToken> valid = new ArrayList<>();
        LudoPlayer player = getCurrentPlayer();
        if (player.hasWon) return valid;

        for (LudoToken t : player.tokens) {
            if (t.hasWon()) continue;
            if (t.isHome()) {
                if (diceValue == 6) valid.add(t);
            } else if (t.isOnBoard()) {
                int newPos = t.position + diceValue;
                if (newPos <= LudoToken.WON) {
                    // Check blocking by own tokens
                    if (!isBlockedByOwn(player, newPos, t)) {
                        valid.add(t);
                    }
                }
            } else if (t.isInHomeColumn()) {
                int newPos = t.position + diceValue;
                if (newPos <= LudoToken.WON) {
                    valid.add(t);
                }
            }
        }
        return valid;
    }

    private boolean isBlockedByOwn(LudoPlayer player, int newPos, LudoToken moving) {
        // Cannot land on own token that forms a block (2 tokens on same square)
        if (newPos >= 52) return false; // Home column, no blocking
        for (LudoToken t : player.tokens) {
            if (t == moving || t.hasWon() || t.isHome()) continue;
            if (t.position == newPos) return false; // Can stack with own, just checking
        }
        return false;
    }

    public boolean moveToken(LudoToken token) {
        if (!diceRolled) return false;
        List<LudoToken> valid = getValidMoves();
        if (!valid.contains(token)) return false;

        extraTurn = false;
        LudoPlayer player = getCurrentPlayer();

        if (token.isHome() && diceValue == 6) {
            // Enter board at position 0 (player-relative)
            token.position = 0;
            SoundEngine.playTokenMove();
            // Check capture at entry
            checkCapture(token, player);
            extraTurn = true; // 6 grants extra turn
        } else {
            int oldPos = token.position;
            token.position += diceValue;

            if (token.position == LudoToken.WON) {
                // Token reaches home!
                SoundEngine.playWin();
                statusMessage = player.name + " token reached home!";
            } else if (token.isOnBoard()) {
                SoundEngine.playTokenMove();
                checkCapture(token, player);
            } else {
                SoundEngine.playTokenMove();
            }
        }

        // Check if player has won
        if (player.allTokensWon()) {
            finishCount++;
            player.hasWon = true;
            player.finishOrder = finishCount;

            // Check if game over (only 1 player left)
            int playersLeft = 0;
            for (int i = 0; i < playerCount; i++) {
                if (!players[i].hasWon) playersLeft++;
            }
            if (playersLeft <= 1) {
                gameOver = true;
                gameOverMessage = player.name + " wins!";
                return true;
            }
        }

        if (diceValue == 6) extraTurn = true;

        diceRolled = false;
        if (!extraTurn) {
            nextTurn();
        } else {
            statusMessage = player.name + " gets another turn!";
        }
        return true;
    }

    private void checkCapture(LudoToken token, LudoPlayer player) {
        if (!token.isOnBoard()) return;
        int absPos = token.getAbsolutePosition();
        if (token.isOnSafeSquare()) return;

        for (int i = 0; i < playerCount; i++) {
            if (i == player.index) continue;
            for (LudoToken ot : players[i].tokens) {
                if (ot.isOnBoard() && ot.getAbsolutePosition() == absPos) {
                    ot.position = LudoToken.HOME_BASE;
                    SoundEngine.playCapture();
                    statusMessage = player.name + " captured " + players[i].name + "'s token!";
                    extraTurn = true;
                }
            }
        }
    }

    public void nextTurn() {
        diceRolled = false;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % playerCount;
        } while (players[currentPlayerIndex].hasWon && !gameOver);
        statusMessage = players[currentPlayerIndex].name + "'s turn";
    }

    public LudoToken aiChooseMove(LudoPlayer player, List<LudoToken> validTokens) {
        return ai.chooseMoveToken(player, diceValue, validTokens, players);
    }

    // Get screen position for a token
    public static int[] getScreenPos(LudoToken token, int cellSize, int offsetX, int offsetY) {
        if (token.isHome()) {
            return getHomeBasePos(token.playerIndex, token.tokenId, cellSize, offsetX, offsetY);
        }
        if (token.hasWon()) {
            // Center area
            int cx = offsetX + 7 * cellSize + cellSize / 2;
            int cy = offsetY + 7 * cellSize + cellSize / 2;
            return new int[]{cx + (token.tokenId % 2) * 15 - 7, cy + (token.tokenId / 2) * 15 - 7};
        }
        int[] rc;
        if (token.isOnBoard()) {
            int abs = token.getAbsolutePosition();
            rc = MAIN_TRACK[abs];
        } else {
            // Home column
            int colIdx = token.position - 52;
            rc = HOME_COLUMNS[token.playerIndex][colIdx];
        }
        return new int[]{offsetX + rc[1] * cellSize + cellSize / 2,
                         offsetY + rc[0] * cellSize + cellSize / 2};
    }

    private static int[] getHomeBasePos(int playerIdx, int tokenId, int cellSize, int ox, int oy) {
        // Each home base is 6x6 cells. Inner white area is 4x4 cells starting at offset (1,1).
        // Home base top-left corners (col, row) on the 15x15 grid:
        //   Red=0: (0,0), Green=1: (9,0), Yellow=2: (9,9), Blue=3: (0,9)
        int[][] baseTopLeft = {{0, 0}, {9, 0}, {9, 9}, {0, 9}};
        int baseCol = baseTopLeft[playerIdx][0];
        int baseRow = baseTopLeft[playerIdx][1];

        // Inner white area center (in pixels)
        // Inner area starts at (baseCol+1, baseRow+1) and is 4 cells wide/tall
        // Center is at (baseCol+3, baseRow+3) in grid coords
        int centerX = ox + (baseCol + 3) * cellSize;
        int centerY = oy + (baseRow + 3) * cellSize;

        // Place 4 tokens in a 2x2 grid with spacing of 1.4 cells
        int tr = tokenId / 2;  // 0 or 1
        int tc = tokenId % 2;  // 0 or 1
        int spacing = (int)(cellSize * 1.4);

        int x = centerX + (tc == 0 ? -spacing / 2 : spacing / 2);
        int y = centerY + (tr == 0 ? -spacing / 2 : spacing / 2);

        return new int[]{x, y};
    }
}
