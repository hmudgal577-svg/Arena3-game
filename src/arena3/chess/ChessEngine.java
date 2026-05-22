package arena3.chess;

import java.awt.Point;
import java.util.*;
import arena3.chess.ChessPiece.*;

public class ChessEngine {
    public ChessPiece[][] board = new ChessPiece[8][8];
    public PieceColor currentTurn = PieceColor.WHITE;
    public Point enPassantTarget = null; // square where en passant capture is possible
    public List<ChessMove> moveHistory = new ArrayList<>();
    public List<String> algebraicHistory = new ArrayList<>();
    public List<ChessPiece> whiteCaptured = new ArrayList<>();
    public List<ChessPiece> blackCaptured = new ArrayList<>();

    public boolean gameOver = false;
    public String gameOverMessage = "";
    public boolean inCheck = false;
    public int halfMoveClock = 0; // for 50-move rule

    public void initBoard() {
        board = new ChessPiece[8][8];
        Type[] backRank = {Type.ROOK, Type.KNIGHT, Type.BISHOP, Type.QUEEN, Type.KING, Type.BISHOP, Type.KNIGHT, Type.ROOK};
        for (int c = 0; c < 8; c++) {
            board[0][c] = new ChessPiece(backRank[c], PieceColor.BLACK);
            board[1][c] = new ChessPiece(Type.PAWN, PieceColor.BLACK);
            board[6][c] = new ChessPiece(Type.PAWN, PieceColor.WHITE);
            board[7][c] = new ChessPiece(backRank[c], PieceColor.WHITE);
        }
        currentTurn = PieceColor.WHITE;
        enPassantTarget = null;
        moveHistory.clear();
        algebraicHistory.clear();
        whiteCaptured.clear();
        blackCaptured.clear();
        gameOver = false;
        gameOverMessage = "";
        inCheck = false;
        halfMoveClock = 0;
    }

    public ChessEngine deepCopy() {
        ChessEngine copy = new ChessEngine();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                copy.board[r][c] = board[r][c] != null ? board[r][c].copy() : null;
        copy.currentTurn = currentTurn;
        copy.enPassantTarget = enPassantTarget != null ? new Point(enPassantTarget) : null;
        copy.moveHistory = new ArrayList<>(moveHistory);
        copy.algebraicHistory = new ArrayList<>(algebraicHistory);
        copy.whiteCaptured = new ArrayList<>(whiteCaptured);
        copy.blackCaptured = new ArrayList<>(blackCaptured);
        copy.gameOver = gameOver;
        copy.gameOverMessage = gameOverMessage;
        copy.inCheck = inCheck;
        copy.halfMoveClock = halfMoveClock;
        return copy;
    }

    // --- Pseudo-legal move generation ---
    public List<Point> getPseudoLegalMoves(int row, int col) {
        List<Point> moves = new ArrayList<>();
        ChessPiece piece = board[row][col];
        if (piece == null) return moves;

        switch (piece.type) {
            case PAWN: addPawnMoves(row, col, piece, moves); break;
            case KNIGHT: addKnightMoves(row, col, piece, moves); break;
            case BISHOP: addBishopMoves(row, col, piece, moves); break;
            case ROOK: addRookMoves(row, col, piece, moves); break;
            case QUEEN: addBishopMoves(row, col, piece, moves); addRookMoves(row, col, piece, moves); break;
            case KING: addKingMoves(row, col, piece, moves); break;
        }
        return moves;
    }

    private void addPawnMoves(int r, int c, ChessPiece p, List<Point> moves) {
        int dir = (p.color == PieceColor.WHITE) ? -1 : 1;
        int startRow = (p.color == PieceColor.WHITE) ? 6 : 1;

        // Forward 1
        if (inBounds(r + dir, c) && board[r + dir][c] == null) {
            moves.add(new Point(c, r + dir));
            // Forward 2 from start
            if (r == startRow && board[r + 2 * dir][c] == null) {
                moves.add(new Point(c, r + 2 * dir));
            }
        }
        // Diagonal captures
        for (int dc : new int[]{-1, 1}) {
            int nr = r + dir, nc = c + dc;
            if (!inBounds(nr, nc)) continue;
            if (board[nr][nc] != null && board[nr][nc].color != p.color) {
                moves.add(new Point(nc, nr));
            }
            // En passant
            if (enPassantTarget != null && enPassantTarget.x == nc && enPassantTarget.y == nr) {
                moves.add(new Point(nc, nr));
            }
        }
    }

    private void addKnightMoves(int r, int c, ChessPiece p, List<Point> moves) {
        int[][] offsets = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] o : offsets) {
            int nr = r + o[0], nc = c + o[1];
            if (inBounds(nr, nc) && (board[nr][nc] == null || board[nr][nc].color != p.color)) {
                moves.add(new Point(nc, nr));
            }
        }
    }

    private void addBishopMoves(int r, int c, ChessPiece p, List<Point> moves) {
        int[][] dirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
        addRayMoves(r, c, p, moves, dirs);
    }

    private void addRookMoves(int r, int c, ChessPiece p, List<Point> moves) {
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        addRayMoves(r, c, p, moves, dirs);
    }

    private void addRayMoves(int r, int c, ChessPiece p, List<Point> moves, int[][] dirs) {
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (inBounds(nr, nc)) {
                if (board[nr][nc] == null) {
                    moves.add(new Point(nc, nr));
                } else {
                    if (board[nr][nc].color != p.color) moves.add(new Point(nc, nr));
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }
    }

    private void addKingMoves(int r, int c, ChessPiece p, List<Point> moves) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (inBounds(nr, nc) && (board[nr][nc] == null || board[nr][nc].color != p.color)) {
                    moves.add(new Point(nc, nr));
                }
            }
        }
        // Castling
        if (!p.hasMoved && !isInCheck(p.color, board)) {
            // Kingside
            if (canCastleKingside(r, c, p.color)) {
                moves.add(new Point(c + 2, r));
            }
            // Queenside
            if (canCastleQueenside(r, c, p.color)) {
                moves.add(new Point(c - 2, r));
            }
        }
    }

    private boolean canCastleKingside(int r, int c, PieceColor color) {
        // Rook at (r, 7) must not have moved
        ChessPiece rook = board[r][7];
        if (rook == null || rook.type != Type.ROOK || rook.color != color || rook.hasMoved) return false;
        // Path clear
        if (board[r][c+1] != null || board[r][c+2] != null) return false;
        // Path not attacked
        if (isSquareAttacked(r, c+1, color, board) || isSquareAttacked(r, c+2, color, board)) return false;
        return true;
    }

    private boolean canCastleQueenside(int r, int c, PieceColor color) {
        ChessPiece rook = board[r][0];
        if (rook == null || rook.type != Type.ROOK || rook.color != color || rook.hasMoved) return false;
        if (board[r][c-1] != null || board[r][c-2] != null || board[r][c-3] != null) return false;
        if (isSquareAttacked(r, c-1, color, board) || isSquareAttacked(r, c-2, color, board)) return false;
        return true;
    }

    // --- Legal move generation (filters out moves leaving king in check) ---
    public List<Point> getLegalMoves(int row, int col) {
        ChessPiece piece = board[row][col];
        if (piece == null || piece.color != currentTurn) return new ArrayList<>();

        List<Point> pseudo = getPseudoLegalMoves(row, col);
        List<Point> legal = new ArrayList<>();

        for (Point to : pseudo) {
            ChessEngine testEngine = deepCopy();
            testEngine.applyMoveRaw(new Point(col, row), to);
            if (!testEngine.isInCheck(piece.color, testEngine.board)) {
                legal.add(to);
            }
        }
        return legal;
    }

    public List<ChessMove> getAllLegalMoves(PieceColor color) {
        List<ChessMove> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] != null && board[r][c].color == color) {
                    List<Point> targets = getLegalMoves(r, c);
                    for (Point t : targets) {
                        ChessMove m = new ChessMove(c, r, t.x, t.y, board[r][c]);
                        m.capturedPiece = board[t.y][t.x];
                        // Check for pawn promotion
                        if (board[r][c].type == Type.PAWN) {
                            int promoRow = (color == PieceColor.WHITE) ? 0 : 7;
                            if (t.y == promoRow) m.isPromotion = true;
                        }
                        moves.add(m);
                    }
                }
            }
        }
        return moves;
    }

    // --- Move execution ---
    private void applyMoveRaw(Point from, Point to) {
        ChessPiece piece = board[from.y][from.x];
        if (piece == null) return;

        // En passant capture
        if (piece.type == Type.PAWN && enPassantTarget != null &&
                to.x == enPassantTarget.x && to.y == enPassantTarget.y) {
            int capturedRow = from.y; // pawn being captured is on same row as moving pawn
            board[capturedRow][to.x] = null;
        }

        // Castling - move rook
        if (piece.type == Type.KING && Math.abs(to.x - from.x) == 2) {
            if (to.x > from.x) { // Kingside
                board[from.y][from.x + 1] = board[from.y][7];
                board[from.y][7] = null;
                if (board[from.y][from.x + 1] != null) board[from.y][from.x + 1].hasMoved = true;
            } else { // Queenside
                board[from.y][from.x - 1] = board[from.y][0];
                board[from.y][0] = null;
                if (board[from.y][from.x - 1] != null) board[from.y][from.x - 1].hasMoved = true;
            }
        }

        board[to.y][to.x] = piece;
        board[from.y][from.x] = null;
        piece.hasMoved = true;

        // Update en passant target
        if (piece.type == Type.PAWN && Math.abs(to.y - from.y) == 2) {
            enPassantTarget = new Point(to.x, (from.y + to.y) / 2);
        } else {
            enPassantTarget = null;
        }
    }

    public void makeMove(Point from, Point to, ChessPiece.Type promoType) {
        ChessPiece piece = board[from.y][from.x];
        if (piece == null) return;

        ChessMove move = new ChessMove(from, to, piece);
        move.capturedPiece = board[to.y][to.x];

        // En passant
        if (piece.type == Type.PAWN && enPassantTarget != null &&
                to.x == enPassantTarget.x && to.y == enPassantTarget.y) {
            move.isEnPassant = true;
            move.capturedPiece = board[from.y][to.x];
        }

        // Castling
        if (piece.type == Type.KING && Math.abs(to.x - from.x) == 2) {
            move.isCastle = true;
        }

        // Track captured pieces
        if (move.capturedPiece != null) {
            if (move.capturedPiece.color == PieceColor.WHITE)
                whiteCaptured.add(move.capturedPiece);
            else
                blackCaptured.add(move.capturedPiece);
            halfMoveClock = 0;
        } else if (piece.type == Type.PAWN) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }

        applyMoveRaw(from, to);

        // Pawn promotion
        int promoRow = (piece.color == PieceColor.WHITE) ? 0 : 7;
        if (piece.type == Type.PAWN && to.y == promoRow) {
            move.isPromotion = true;
            ChessPiece.Type pt = promoType != null ? promoType : Type.QUEEN;
            move.promotionType = pt;
            board[to.y][to.x] = new ChessPiece(pt, piece.color);
            board[to.y][to.x].hasMoved = true;
        }

        // Generate algebraic notation
        move.algebraic = generateAlgebraic(move);

        moveHistory.add(move);
        algebraicHistory.add(move.algebraic);

        // Switch turn
        currentTurn = (currentTurn == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;

        // Check game state
        updateGameState();
    }

    private void updateGameState() {
        inCheck = isInCheck(currentTurn, board);
        List<ChessMove> legalMoves = getAllLegalMoves(currentTurn);

        if (legalMoves.isEmpty()) {
            gameOver = true;
            if (inCheck) {
                PieceColor winner = (currentTurn == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
                gameOverMessage = "Checkmate! " + winner.name() + " wins!";
                // Add checkmate symbol
                if (!algebraicHistory.isEmpty()) {
                    int last = algebraicHistory.size() - 1;
                    algebraicHistory.set(last, algebraicHistory.get(last) + "#");
                }
            } else {
                gameOverMessage = "Stalemate! Draw!";
            }
        } else if (inCheck) {
            // Add check symbol
            if (!algebraicHistory.isEmpty()) {
                int last = algebraicHistory.size() - 1;
                String s = algebraicHistory.get(last);
                if (!s.endsWith("+")) algebraicHistory.set(last, s + "+");
            }
        }

        // 50-move rule
        if (halfMoveClock >= 100) { // 50 moves = 100 half-moves
            gameOver = true;
            gameOverMessage = "Draw by 50-move rule!";
        }
    }

    private String generateAlgebraic(ChessMove move) {
        if (move.isCastle) {
            return (move.to.x > move.from.x) ? "O-O" : "O-O-O";
        }

        StringBuilder sb = new StringBuilder();
        if (move.movedPiece.type != Type.PAWN) {
            sb.append(move.movedPiece.getNotationChar());
        }

        // Disambiguation for non-pawns
        if (move.movedPiece.type != Type.PAWN && move.movedPiece.type != Type.KING) {
            // Check if another piece of same type can move to same square
            boolean needFile = false, needRank = false;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (r == move.from.y && c == move.from.x) continue;
                    ChessPiece other = board[r][c];
                    // Note: board state is AFTER move, so we check moveHistory context
                    // Simplified: skip disambiguation for now as it's edge case
                }
            }
        }

        // Capture
        if (move.capturedPiece != null) {
            if (move.movedPiece.type == Type.PAWN) {
                sb.append((char)('a' + move.from.x));
            }
            sb.append('x');
        }

        // Destination
        sb.append((char)('a' + move.to.x));
        sb.append(8 - move.to.y);

        // Promotion
        if (move.isPromotion && move.promotionType != null) {
            sb.append('=');
            sb.append(new ChessPiece(move.promotionType, PieceColor.WHITE).getNotationChar());
        }

        // En passant
        if (move.isEnPassant) {
            sb.append(" e.p.");
        }

        return sb.toString();
    }

    // --- Check detection ---
    public boolean isInCheck(PieceColor color, ChessPiece[][] testBoard) {
        Point kingPos = findKing(color, testBoard);
        if (kingPos == null) return false;
        return isSquareAttacked(kingPos.y, kingPos.x, color, testBoard);
    }

    public boolean isSquareAttacked(int row, int col, PieceColor defenderColor, ChessPiece[][] testBoard) {
        PieceColor attacker = (defenderColor == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;

        // Knight attacks
        int[][] knightOffsets = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] o : knightOffsets) {
            int nr = row + o[0], nc = col + o[1];
            if (inBounds(nr, nc) && testBoard[nr][nc] != null &&
                    testBoard[nr][nc].color == attacker && testBoard[nr][nc].type == Type.KNIGHT) {
                return true;
            }
        }

        // Pawn attacks
        int pawnDir = (attacker == PieceColor.WHITE) ? 1 : -1; // direction pawns attack FROM
        for (int dc : new int[]{-1, 1}) {
            int pr = row + pawnDir, pc = col + dc;
            if (inBounds(pr, pc) && testBoard[pr][pc] != null &&
                    testBoard[pr][pc].color == attacker && testBoard[pr][pc].type == Type.PAWN) {
                return true;
            }
        }

        // King attacks (adjacent)
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int kr = row + dr, kc = col + dc;
                if (inBounds(kr, kc) && testBoard[kr][kc] != null &&
                        testBoard[kr][kc].color == attacker && testBoard[kr][kc].type == Type.KING) {
                    return true;
                }
            }
        }

        // Rook/Queen (straight lines)
        int[][] straightDirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : straightDirs) {
            int nr = row + d[0], nc = col + d[1];
            while (inBounds(nr, nc)) {
                if (testBoard[nr][nc] != null) {
                    if (testBoard[nr][nc].color == attacker &&
                            (testBoard[nr][nc].type == Type.ROOK || testBoard[nr][nc].type == Type.QUEEN)) {
                        return true;
                    }
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }

        // Bishop/Queen (diagonals)
        int[][] diagDirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : diagDirs) {
            int nr = row + d[0], nc = col + d[1];
            while (inBounds(nr, nc)) {
                if (testBoard[nr][nc] != null) {
                    if (testBoard[nr][nc].color == attacker &&
                            (testBoard[nr][nc].type == Type.BISHOP || testBoard[nr][nc].type == Type.QUEEN)) {
                        return true;
                    }
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }

        return false;
    }

    public Point findKing(PieceColor color, ChessPiece[][] testBoard) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (testBoard[r][c] != null && testBoard[r][c].type == Type.KING && testBoard[r][c].color == color)
                    return new Point(c, r);
        return null;
    }

    public int getMaterialAdvantage() {
        int white = 0, black = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] != null && board[r][c].type != Type.KING) {
                    if (board[r][c].color == PieceColor.WHITE) white += board[r][c].getValue();
                    else black += board[r][c].getValue();
                }
            }
        }
        return white - black;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }
}
