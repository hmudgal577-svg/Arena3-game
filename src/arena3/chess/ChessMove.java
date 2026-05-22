package arena3.chess;

import java.awt.Point;

public class ChessMove {
    public final Point from, to;
    public final ChessPiece movedPiece;
    public ChessPiece capturedPiece;
    public boolean isCastle = false;
    public boolean isEnPassant = false;
    public boolean isPromotion = false;
    public ChessPiece.Type promotionType = null;
    public String algebraic = "";

    public ChessMove(Point from, Point to, ChessPiece moved) {
        this.from = new Point(from);
        this.to = new Point(to);
        this.movedPiece = moved;
    }

    public ChessMove(int fx, int fy, int tx, int ty, ChessPiece moved) {
        this(new Point(fx, fy), new Point(tx, ty), moved);
    }

    @Override
    public String toString() {
        return algebraic.isEmpty() ?
            ("" + (char)('a' + from.x) + (8 - from.y) + (char)('a' + to.x) + (8 - to.y)) :
            algebraic;
    }
}
