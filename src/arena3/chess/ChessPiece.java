package arena3.chess;

public class ChessPiece {
    public enum Type { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }
    public enum PieceColor { WHITE, BLACK }

    public final Type type;
    public final PieceColor color;
    public boolean hasMoved = false;

    public ChessPiece(Type type, PieceColor color) {
        this.type = type;
        this.color = color;
    }

    public int getValue() {
        switch (type) {
            case PAWN: return 1;
            case KNIGHT: return 3;
            case BISHOP: return 3;
            case ROOK: return 5;
            case QUEEN: return 9;
            case KING: return 1000;
        }
        return 0;
    }

    public ChessPiece copy() {
        ChessPiece p = new ChessPiece(type, color);
        p.hasMoved = this.hasMoved;
        return p;
    }

    public String getSymbol() {
        if (color == PieceColor.WHITE) {
            switch (type) {
                case KING: return "\u2654";
                case QUEEN: return "\u2655";
                case ROOK: return "\u2656";
                case BISHOP: return "\u2657";
                case KNIGHT: return "\u2658";
                case PAWN: return "\u2659";
            }
        } else {
            switch (type) {
                case KING: return "\u265A";
                case QUEEN: return "\u265B";
                case ROOK: return "\u265C";
                case BISHOP: return "\u265D";
                case KNIGHT: return "\u265E";
                case PAWN: return "\u265F";
            }
        }
        return "?";
    }

    public char getNotationChar() {
        switch (type) {
            case KING: return 'K';
            case QUEEN: return 'Q';
            case ROOK: return 'R';
            case BISHOP: return 'B';
            case KNIGHT: return 'N';
            default: return ' ';
        }
    }

    @Override
    public String toString() {
        return color.name().charAt(0) + "" + type.name().charAt(0);
    }
}
