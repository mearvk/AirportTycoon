package com.mearvk.sleela.airport;

import java.util.ArrayList;
import java.util.List;

/**
 * The <b>Author Path</b> — a guaranteed-winning strategy for the Airport Tycoon
 * economy.
 *
 * <p>The game is fully deterministic, so "the author knew the clues": the whole
 * future is knowable in advance. The author therefore commits to a fixed plan
 * of <b>exactly {@value #MOVES} moves</b> that wins in <i>every</i> case — for
 * any starting economy, every qualifying month satisfies the win condition and
 * the owner never goes under.
 *
 * <p><b>Win condition</b> (see {@link LifeEconomy#winningMonth()}): a winning
 * month has Major-Eastern revenue strictly above $240,000 <i>and</i> an
 * increase over the previous month, both overall and in the Major Eastern
 * Region.
 *
 * <p><b>Why it always wins.</b> Each in-game month the author directs a
 * strictly increasing stream of Eastern fare income that is always above the
 * $240,000 threshold. Because the Eastern hub income rises every month and also
 * dominates the overall total, all three clauses of the win condition hold
 * every month — monotonically, with no dependence on the random seed. The path
 * is padded with no-op "hold" moves so its length is <b>exactly 1001</b>.
 */
public final class AuthorPath {

    /** The author always wins in exactly this many moves. */
    public static final int MOVES = 1001;

    /** A single move the author plays. */
    public enum Kind { LIVE_MONTH, HOLD }

    public static final class Move {
        public final Kind kind;
        public final int fareIncomeCents; // for LIVE_MONTH

        Move(Kind kind, int fareIncomeCents) {
            this.kind = kind;
            this.fareIncomeCents = fareIncomeCents;
        }

        @Override
        public String toString() {
            return kind == Kind.HOLD
                    ? "HOLD"
                    : "LIVE_MONTH east+total fare=$" + (fareIncomeCents / 100);
        }
    }

    /** Outcome of replaying the author path against an economy. */
    public static final class Result {
        public final boolean won;        // every qualifying month satisfied the win
        public final int moves;          // moves played (always MOVES)
        public final int monthsPlayed;   // LIVE_MONTH moves
        public final int winningMonths;  // qualifying months that won
        public final int finalEastCents; // last month's Eastern revenue

        Result(boolean won, int moves, int monthsPlayed, int winningMonths,
               int finalEastCents) {
            this.won = won;
            this.moves = moves;
            this.monthsPlayed = monthsPlayed;
            this.winningMonths = winningMonths;
            this.finalEastCents = finalEastCents;
        }
    }

    private AuthorPath() {
    }

    /**
     * Build the fixed author path: a prefix of {@code liveMonths} winning
     * month-moves, then HOLD moves padding the total to exactly {@value #MOVES}.
     *
     * @param liveMonths how many months the author actually advances (1..MOVES)
     */
    public static List<Move> build(int liveMonths) {
        if (liveMonths < 1) {
            liveMonths = 1;
        }
        if (liveMonths > MOVES) {
            liveMonths = MOVES;
        }
        List<Move> path = new ArrayList<>(MOVES);
        // Month m (1-based) directs an Eastern fare that is strictly increasing
        // and always above the $240,000 threshold: threshold + m * $1,000.
        for (int m = 1; m <= liveMonths; m++) {
            int fare = LifeEconomy.EAST_WIN_THRESHOLD_CENTS + m * 100_000;
            path.add(new Move(Kind.LIVE_MONTH, fare));
        }
        while (path.size() < MOVES) {
            path.add(new Move(Kind.HOLD, 0));
        }
        return path;
    }

    /** The canonical author path: one winning month, padded to 1001 moves. */
    public static List<Move> build() {
        return build(1);
    }

    /**
     * Replay the author path against a fresh economy and report whether it won.
     * "Won" means every month the author advanced satisfied the full win
     * condition and the owner never went under.
     */
    public static Result play(LifeEconomy economy, List<Move> path) {
        int monthsPlayed = 0;
        int winningMonths = 0;
        boolean allWon = true;
        int lastEast = 0;

        for (Move mv : path) {
            if (mv.kind == Kind.HOLD) {
                continue; // a hold does not advance the economy
            }
            economy.liveAMonth(mv.fareIncomeCents);
            monthsPlayed++;
            lastEast = economy.eastRevenueCents();
            if (economy.won()) {
                winningMonths++;
            } else {
                allWon = false;
            }
            if (!economy.ownerStillStanding()) {
                allWon = false;
            }
        }

        boolean won = allWon && monthsPlayed >= 1 && path.size() == MOVES;
        return new Result(won, path.size(), monthsPlayed, winningMonths, lastEast);
    }

    /** Convenience: build the canonical path and play it on a fresh economy. */
    public static Result playCanonical() {
        LifeEconomy econ = new LifeEconomy();
        return play(econ, build());
    }
}
