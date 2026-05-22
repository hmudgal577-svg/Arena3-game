package arena3.ludo;

import java.util.*;

public class LudoAI {
    private final int difficulty; // 0=Easy, 1=Hard

    public LudoAI(int difficulty) {
        this.difficulty = difficulty;
    }

    public LudoToken chooseMoveToken(LudoPlayer player, int diceValue,
                                      List<LudoToken> validTokens, LudoPlayer[] allPlayers) {
        if (validTokens.isEmpty()) return null;
        if (validTokens.size() == 1) return validTokens.get(0);

        if (difficulty == 0) {
            // Easy: random
            return validTokens.get(new Random().nextInt(validTokens.size()));
        }

        // Hard AI priority
        // 1. Capture opponent if possible
        for (LudoToken t : validTokens) {
            if (t.isOnBoard()) {
                int newPos = t.position + diceValue;
                if (newPos < 52) {
                    int newAbs = (newPos + getStartOffset(player.index)) % 52;
                    for (LudoPlayer opp : allPlayers) {
                        if (opp == null || opp.index == player.index) continue;
                        for (LudoToken ot : opp.tokens) {
                            if (ot.isOnBoard() && ot.getAbsolutePosition() == newAbs && !isSafe(newAbs)) {
                                return t;
                            }
                        }
                    }
                }
            }
        }

        // 2. Enter board if dice=6 and have tokens at home
        if (diceValue == 6) {
            for (LudoToken t : validTokens) {
                if (t.isHome()) return t;
            }
        }

        // 3. Move token into home column or finish
        for (LudoToken t : validTokens) {
            if (t.isOnBoard()) {
                int newPos = t.position + diceValue;
                if (newPos >= 52) return t; // enters home column or wins
            }
            if (t.isInHomeColumn()) {
                int newPos = t.position + diceValue;
                if (newPos == LudoToken.WON) return t; // wins
            }
        }

        // 4. Move to safe square if threatened
        for (LudoToken t : validTokens) {
            if (t.isOnBoard()) {
                int newPos = t.position + diceValue;
                if (newPos < 52) {
                    int newAbs = (newPos + getStartOffset(player.index)) % 52;
                    if (isSafe(newAbs) && isThreatenedByOpponent(t, allPlayers, player.index)) {
                        return t;
                    }
                }
            }
        }

        // 5. Move farthest token (closest to winning)
        LudoToken farthest = null;
        int maxPos = -2;
        for (LudoToken t : validTokens) {
            if (t.position > maxPos) {
                maxPos = t.position;
                farthest = t;
            }
        }
        return farthest != null ? farthest : validTokens.get(0);
    }

    private boolean isThreatenedByOpponent(LudoToken token, LudoPlayer[] players, int myIndex) {
        if (!token.isOnBoard()) return false;
        int abs = token.getAbsolutePosition();
        if (isSafe(abs)) return false;
        for (LudoPlayer opp : players) {
            if (opp == null || opp.index == myIndex) continue;
            for (LudoToken ot : opp.tokens) {
                if (ot.isOnBoard()) {
                    int oppAbs = ot.getAbsolutePosition();
                    // Check if opponent is within 6 squares behind
                    for (int d = 1; d <= 6; d++) {
                        if ((oppAbs + d) % 52 == abs) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isSafe(int absPos) {
        int[] safeSquares = {0, 8, 13, 21, 26, 34, 39, 47};
        for (int s : safeSquares) if (absPos == s) return true;
        return false;
    }

    private int getStartOffset(int playerIndex) {
        int[] offsets = {0, 13, 26, 39};
        return offsets[playerIndex];
    }
}
