package arena3.ludo;

import java.awt.*;

public class LudoToken {
    public static final int HOME_BASE = -1;
    public static final int WON = 58;

    public int position; // -1=base, 0-51=main track, 52-57=home column, 58=won
    public final int playerIndex; // 0=Red, 1=Green, 2=Yellow, 3=Blue
    public final int tokenId; // 0-3

    // Animation state
    public float animX = -1, animY = -1;
    public boolean animating = false;

    public LudoToken(int playerIndex, int tokenId) {
        this.playerIndex = playerIndex;
        this.tokenId = tokenId;
        this.position = HOME_BASE;
    }

    public boolean isHome() { return position == HOME_BASE; }
    public boolean hasWon() { return position == WON; }
    public boolean isOnBoard() { return position >= 0 && position < 52; }
    public boolean isInHomeColumn() { return position >= 52 && position < 58; }

    /** Get absolute board position from player-relative position */
    public int getAbsolutePosition() {
        if (!isOnBoard()) return -1;
        int[] startOffsets = {0, 13, 26, 39};
        return (position + startOffsets[playerIndex]) % 52;
    }

    /** Check if token is on a safe square */
    public boolean isOnSafeSquare() {
        if (!isOnBoard()) return false;
        int abs = getAbsolutePosition();
        // Safe squares at entries and star positions
        int[] safeSquares = {0, 8, 13, 21, 26, 34, 39, 47};
        for (int s : safeSquares) {
            if (abs == s) return true;
        }
        return false;
    }

    public LudoToken copy() {
        LudoToken t = new LudoToken(playerIndex, tokenId);
        t.position = this.position;
        return t;
    }
}
