package arena3.ludo;

import java.awt.Color;

public class LudoPlayer {
    public final int index; // 0-3
    public final String name;
    public final Color color;
    public final boolean isAI;
    public final LudoToken[] tokens = new LudoToken[4];
    public boolean hasWon = false;
    public int finishOrder = -1; // 1st, 2nd, 3rd, 4th

    public LudoPlayer(int index, String name, Color color, boolean isAI) {
        this.index = index;
        this.name = name;
        this.color = color;
        this.isAI = isAI;
        for (int i = 0; i < 4; i++) {
            tokens[i] = new LudoToken(index, i);
        }
    }

    public boolean allTokensWon() {
        for (LudoToken t : tokens) {
            if (!t.hasWon()) return false;
        }
        return true;
    }

    public int tokensWon() {
        int c = 0;
        for (LudoToken t : tokens) if (t.hasWon()) c++;
        return c;
    }

    public int tokensOnBoard() {
        int c = 0;
        for (LudoToken t : tokens) if (t.isOnBoard() || t.isInHomeColumn()) c++;
        return c;
    }

    public int tokensAtHome() {
        int c = 0;
        for (LudoToken t : tokens) if (t.isHome()) c++;
        return c;
    }
}
