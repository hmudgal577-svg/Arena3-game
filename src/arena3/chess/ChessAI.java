package arena3.chess;

import java.awt.Point;
import java.util.*;
import arena3.chess.ChessPiece.*;

public class ChessAI {
    private final int difficulty; // 0=Easy, 1=Medium, 2=Hard
    private final PieceColor aiColor;

    private static final int[] PIECE_VALUES = {100, 320, 330, 500, 900, 20000};

    private static final int[][] PAWN_TABLE = {
        { 0,  0,  0,  0,  0,  0,  0,  0},
        {50, 50, 50, 50, 50, 50, 50, 50},
        {10, 10, 20, 30, 30, 20, 10, 10},
        { 5,  5, 10, 25, 25, 10,  5,  5},
        { 0,  0,  0, 20, 20,  0,  0,  0},
        { 5, -5,-10,  0,  0,-10, -5,  5},
        { 5, 10, 10,-20,-20, 10, 10,  5},
        { 0,  0,  0,  0,  0,  0,  0,  0}
    };

    private static final int[][] KNIGHT_TABLE = {
        {-50,-40,-30,-30,-30,-30,-40,-50},
        {-40,-20,  0,  0,  0,  0,-20,-40},
        {-30,  0, 10, 15, 15, 10,  0,-30},
        {-30,  5, 15, 20, 20, 15,  5,-30},
        {-30,  0, 15, 20, 20, 15,  0,-30},
        {-30,  5, 10, 15, 15, 10,  5,-30},
        {-40,-20,  0,  5,  5,  0,-20,-40},
        {-50,-40,-30,-30,-30,-30,-40,-50}
    };

    private static final int[][] BISHOP_TABLE = {
        {-20,-10,-10,-10,-10,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0, 10, 10, 10, 10,  0,-10},
        {-10,  5,  5, 10, 10,  5,  5,-10},
        {-10,  0,  5, 10, 10,  5,  0,-10},
        {-10, 10, 10, 10, 10, 10, 10,-10},
        {-10,  5,  0,  0,  0,  0,  5,-10},
        {-20,-10,-10,-10,-10,-10,-10,-20}
    };

    private static final int[][] ROOK_TABLE = {
        { 0,  0,  0,  0,  0,  0,  0,  0},
        { 5, 10, 10, 10, 10, 10, 10,  5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        { 0,  0,  0,  5,  5,  0,  0,  0}
    };

    private static final int[][] QUEEN_TABLE = {
        {-20,-10,-10, -5, -5,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5,  5,  5,  5,  0,-10},
        { -5,  0,  5,  5,  5,  5,  0, -5},
        {  0,  0,  5,  5,  5,  5,  0, -5},
        {-10,  5,  5,  5,  5,  5,  0,-10},
        {-10,  0,  5,  0,  0,  0,  0,-10},
        {-20,-10,-10, -5, -5,-10,-10,-20}
    };

    private static final int[][] KING_TABLE = {
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-20,-30,-30,-40,-40,-30,-30,-20},
        {-10,-20,-20,-20,-20,-20,-20,-10},
        { 20, 20,  0,  0,  0,  0, 20, 20},
        { 20, 30, 10,  0,  0, 10, 30, 20}
    };

    private static final int[][][] PST = {PAWN_TABLE, KNIGHT_TABLE, BISHOP_TABLE, ROOK_TABLE, QUEEN_TABLE, KING_TABLE};

    public ChessAI(int difficulty, PieceColor aiColor) {
        this.difficulty = difficulty;
        this.aiColor = aiColor;
    }

    public ChessMove getBestMove(ChessEngine engine) {
        List<ChessMove> moves = engine.getAllLegalMoves(aiColor);
        if (moves.isEmpty()) return null;

        if (difficulty == 0) {
            return moves.get(new Random().nextInt(moves.size()));
        }

        int depth = (difficulty == 1) ? 3 : 4;

        // Move ordering: captures first, promotions, then rest
        moves.sort((a, b) -> moveOrderScore(b) - moveOrderScore(a));

        ChessMove bestMove = moves.get(0);
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE + 1;
        int beta = Integer.MAX_VALUE - 1;

        for (ChessMove move : moves) {
            ChessEngine copy = engine.deepCopy();
            Type promoType = move.isPromotion ? Type.QUEEN : null;
            copy.makeMove(move.from, move.to, promoType);

            // After our move, it's opponent's turn - minimax returns score from AI perspective
            int score = minimax(copy, depth - 1, alpha, beta, opposite(aiColor));

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, score);
        }

        return bestMove;
    }

    private int minimax(ChessEngine engine, int depth, int alpha, int beta, PieceColor color) {
        if (engine.gameOver) {
            if (engine.gameOverMessage.contains("Checkmate")) {
                // The side that just moved delivered checkmate
                // If it was AI's move that caused this, score is very good
                PieceColor justMoved = opposite(engine.currentTurn);
                if (justMoved == aiColor) return 99999 + depth;
                else return -99999 - depth;
            }
            return 0; // stalemate/draw
        }

        if (depth == 0) {
            return evaluate(engine.board);
        }

        List<ChessMove> moves = engine.getAllLegalMoves(color);

        if (moves.isEmpty()) {
            if (engine.isInCheck(color, engine.board)) {
                // Color is in checkmate
                return (color == aiColor) ? -99999 - depth : 99999 + depth;
            }
            return 0; // stalemate
        }

        moves.sort((a, b) -> moveOrderScore(b) - moveOrderScore(a));

        if (color == aiColor) {
            // Maximizing
            int maxEval = Integer.MIN_VALUE + 1;
            for (ChessMove move : moves) {
                ChessEngine copy = engine.deepCopy();
                Type pt = move.isPromotion ? Type.QUEEN : null;
                copy.makeMove(move.from, move.to, pt);
                int eval = minimax(copy, depth - 1, alpha, beta, opposite(color));
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            // Minimizing
            int minEval = Integer.MAX_VALUE - 1;
            for (ChessMove move : moves) {
                ChessEngine copy = engine.deepCopy();
                Type pt = move.isPromotion ? Type.QUEEN : null;
                copy.makeMove(move.from, move.to, pt);
                int eval = minimax(copy, depth - 1, alpha, beta, opposite(color));
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    /** Evaluate board from AI's perspective (positive = good for AI) */
    private int evaluate(ChessPiece[][] board) {
        int score = 0;
        int aiMobility = 0, oppMobility = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                ChessPiece p = board[r][c];
                if (p == null) continue;
                int val = PIECE_VALUES[p.type.ordinal()];
                int pst = getPST(p, r, c);
                int total = val + pst;
                if (p.color == aiColor) {
                    score += total;
                    // Center control bonus for minor pieces
                    if ((p.type == Type.KNIGHT || p.type == Type.BISHOP) &&
                            r >= 2 && r <= 5 && c >= 2 && c <= 5) {
                        score += 15;
                    }
                } else {
                    score -= total;
                    if ((p.type == Type.KNIGHT || p.type == Type.BISHOP) &&
                            r >= 2 && r <= 5 && c >= 2 && c <= 5) {
                        score -= 15;
                    }
                }
            }
        }
        return score;
    }

    private int getPST(ChessPiece piece, int row, int col) {
        int[][] table = PST[piece.type.ordinal()];
        if (piece.color == PieceColor.WHITE) {
            return table[row][col];
        } else {
            return table[7 - row][col];
        }
    }

    private int moveOrderScore(ChessMove m) {
        int score = 0;
        if (m.capturedPiece != null) score += 10 * m.capturedPiece.getValue();
        if (m.isPromotion) score += 900;
        if (m.isCastle) score += 50;
        return score;
    }

    private PieceColor opposite(PieceColor c) {
        return c == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;
    }
}
