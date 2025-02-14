package com.engine.player;

import com.engine.Alliance;
import com.engine.board.Board;
import com.engine.board.Move;
import com.engine.pieces.King;
import com.engine.pieces.Piece;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class Player {
    protected final Board board;
    final King playerKing;
    private final Collection<Move> legalMoves;
    private final boolean isInCheck;

    Player(final Board board,
           final Collection<Move> legalMoves,
           final Collection<Move> opponentMoves) {
        this.board = board;
        this.playerKing = establishKing();
        this.legalMoves = ImmutableList.copyOf(Iterables.concat(legalMoves, calculateKingCastles(legalMoves, opponentMoves)));
        this.isInCheck = !calculateAttackOnTile(this.playerKing.getPiecePosition(), opponentMoves).isEmpty();
    }

    static Collection<Move> calculateAttackOnTile(final int piecePosition, final Collection<Move> moves) {
        final List<Move> attackTileMoves = new ArrayList<>();
        for(final Move candidateTileMove : moves) {
            if(piecePosition == candidateTileMove.getDestinationCoordinate()) {
                attackTileMoves.add(candidateTileMove);
            }
        }
        return ImmutableList.copyOf(attackTileMoves);
    }

    private King getPlayerKing() {
        return playerKing;
    }

    public Collection<Move> getLegalMoves() {
        return this.legalMoves;
    }

    private King establishKing() {
        for(final Piece piece : getActivePieces()) {
            if(piece.getPieceType().isKing()) {
                return (King) piece;
            }
        }
        throw new RuntimeException("Should not reach here! Not a valid board");
    }

    private boolean isMoveLegal(final Move candidateMove) {
        return this.legalMoves.contains(candidateMove);
    }

    public boolean isInCheck() {
        return this.isInCheck;
    }

    public boolean isInCheckMate() {
        return this.isInCheck && hasNoEscapeMoves();
    }

    public boolean isInStaleMate() {
        return !this.isInCheck && hasNoEscapeMoves();
    }

    private boolean hasNoEscapeMoves() {
        for(final Move move : this.legalMoves) {
            final MoveTransition transition = makeMove(move);
            if(transition.getMoveStatus().isDone()) {
                return false;
            }
        }
        return true;
    }

    /**
     * the function checks if currentPlayer can make the candidateMove so it will not lead to an illegal move or to checking himself
     *
     * the function checks if the move is not legal so it creates a moveTransition with status that is ILLEGAL_MOVE else
     * if the move is legal it checks if the move will lead to an attack on currentPlayer king(checking himself)
     *
     * @param candidateMove is for checking if currentPlayer can make candidateMove
     * @return a MoveTransition with the candidateMove and the status of it
     */

    public MoveTransition makeMove(final Move candidateMove) {
        if(!isMoveLegal(candidateMove)) {
            return new MoveTransition(this.board, candidateMove, MoveStatus.ILLEGAL_MOVE);
        }
        final Board transitionBoard = candidateMove.execute();
        final Collection<Move> kingAttacks = calculateAttackOnTile(transitionBoard.getCurrentPlayer().getOpponent().getPlayerKing().getPiecePosition(),
                transitionBoard.getCurrentPlayer().getLegalMoves());
        if(!kingAttacks.isEmpty()) {
            return new MoveTransition(this.board, candidateMove, MoveStatus.LEAVES_PLAYER_IN_CHECK);
        }
        return new MoveTransition(transitionBoard, candidateMove, MoveStatus.DONE);
    }

    public abstract Collection<Piece> getActivePieces();

    public abstract Alliance getAlliance();

    public abstract Player getOpponent();

    protected abstract Collection<Move> calculateKingCastles(final Collection<Move> playerLegals,
                                                             final Collection<Move> opponentLegals);
}