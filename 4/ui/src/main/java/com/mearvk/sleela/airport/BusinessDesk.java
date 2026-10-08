package com.mearvk.sleela.airport;

import java.util.ArrayList;
import java.util.List;

/**
 * A faithful Java mirror of {@code game/BusinessDesk.sleela} — the Edition 2
 * <b>Business Desk</b>, where the player steps up to create a <i>move</i> or
 * <i>transaction</i> while the tower game runs.
 *
 * <p>The desk starts <b>empty</b>: a player who has only "tried on their
 * genius" has opened no ventures yet. The player can run side games in parallel
 * with flying planes — <b>Casino Management</b> and <b>Investment
 * Management</b> — modelled here as <b>Chemistry</b>: each venture is a seeded
 * reaction that settles a weekly transaction onto the owner's book.
 *
 * <ul>
 *   <li><b>Casino Management</b> is an {@code EXOTHERMIC} reaction — high
 *       variance, a slight house edge, big swings week to week.</li>
 *   <li><b>Investment Management</b> is a {@code TITRATION} reaction — steady
 *       compounding at a modest weekly yield, with the occasional drawdown.</li>
 * </ul>
 *
 * <p><b>Major Level-5 moves.</b> A venture accrues <b>IQ</b> from sustained
 * profitable weeks. When a venture reaches <b>IQ level 5</b> it unlocks the
 * <b>Great Assimilation</b> — an "IQ reordering of the Orient by assimilation
 * of US capitalist interests" that applies a large multiplier to Major-Eastern
 * revenue for a run of weeks: the stretch that <i>rakes in the money</i>. This
 * is the Edition 2 bridge from the Business Desk into the East&nbsp;5.0 /
 * Major-Eastern win economy ({@link LifeEconomy}).
 *
 * <p>Everything is driven by a deterministic, seeded PRNG so a given seed
 * replays identically, exactly like the tower game and the media center.
 */
public final class BusinessDesk {

    // --- Venture kinds -----------------------------------------------------
    public static final int VENTURE_CASINO = 1;      // Casino Management
    public static final int VENTURE_INVESTMENT = 2;  // Investment Management

    // --- Chemistry of each venture ----------------------------------------
    public static final int REACTION_EXOTHERMIC = 1; // volatile, big swings
    public static final int REACTION_TITRATION = 2;  // steady, compounding

    // --- The Major Level-5 move -------------------------------------------
    /** A venture reaches a "major move" at this IQ level. */
    public static final int MAJOR_MOVE_LEVEL = 5;
    /** IQ gained per profitable week; a losing week costs one back. */
    static final int IQ_PER_GOOD_WEEK = 1;
    /** IQ needed per level. One IQ point per level, so level == net IQ. */
    static final int IQ_PER_LEVEL = 1;
    /** How many weeks the Great Assimilation keeps raking money in. */
    static final int ASSIMILATION_WEEKS = 6;
    /** Multiplier (percent) on Major-Eastern revenue while assimilation runs. */
    static final int ASSIMILATION_EAST_PCT = 300; // 3x — "raked in the money"

    /** One venture the player has tried on at the desk. */
    public static final class Venture {
        final int kind;
        final String name;
        final int reaction;
        int stakeCents;      // working capital committed
        int balanceCents;    // current value of the venture
        int iq;              // accrued IQ (sustained-performance score)
        int goodWeeks;       // consecutive profitable weeks
        int lastSettleCents; // last weekly transaction (+/-)
        boolean majorFired;  // has this venture ever fired its Level-5 move?

        Venture(int kind, String name, int reaction, int stakeCents) {
            this.kind = kind;
            this.name = name;
            this.reaction = reaction;
            this.stakeCents = Math.max(0, stakeCents);
            this.balanceCents = this.stakeCents;
            this.iq = 0;
            this.goodWeeks = 0;
            this.lastSettleCents = 0;
            this.majorFired = false;
        }

        /** The venture's current IQ level (0-based buckets of IQ_PER_LEVEL). */
        public int level() {
            return iq / IQ_PER_LEVEL;
        }

        public int kind() {
            return kind;
        }

        public String name() {
            return name;
        }

        public int reaction() {
            return reaction;
        }

        public int balanceCents() {
            return balanceCents;
        }

        public int iq() {
            return iq;
        }

        public int lastSettleCents() {
            return lastSettleCents;
        }

        public boolean majorFired() {
            return majorFired;
        }
    }

    private final List<Venture> ventures = new ArrayList<>();
    private int week;
    private int rngState;
    private int deskProfitCents;        // cumulative profit booked by the desk
    private int assimilationWeeksLeft;  // >0 while the Great Assimilation runs
    private int majorMoves;             // how many Level-5 moves have fired

    public BusinessDesk() {
        this(90210);
    }

    public BusinessDesk(int seed) {
        reset(seed);
    }

    public void reset(int seed) {
        ventures.clear();
        week = 0;
        rngState = seed;
        deskProfitCents = 0;
        assimilationWeeksLeft = 0;
        majorMoves = 0;
    }

    // --- deterministic PRNG, matching the Wrapper and the tower game -------
    private int nextRand() {
        int x = rngState + (int) 2654435761L;
        if (x < 0) {
            x = -x;
        }
        rngState = x;
        return x;
    }

    private int randRange(int lo, int hi) {
        if (hi <= lo) {
            return lo;
        }
        return lo + (nextRand() % (hi - lo));
    }

    // ---------------------------------------------------------------------
    // The player's moves at the desk. The desk is empty until a venture is
    // opened; opening one is the first real "transaction".
    // ---------------------------------------------------------------------

    /** True until the player has tried on a venture — "the desk is empty". */
    public boolean isEmpty() {
        return ventures.isEmpty();
    }

    /** Try on Casino Management (an exothermic Chemistry reaction). */
    public Venture openCasino(int stakeCents) {
        Venture v = new Venture(VENTURE_CASINO, "Casino Management",
                REACTION_EXOTHERMIC, stakeCents);
        ventures.add(v);
        return v;
    }

    /** Try on Investment Management (a titration Chemistry reaction). */
    public Venture openInvestment(int stakeCents) {
        Venture v = new Venture(VENTURE_INVESTMENT, "Investment Management",
                REACTION_TITRATION, stakeCents);
        ventures.add(v);
        return v;
    }

    /**
     * Settle one week across every open venture. Each venture runs its
     * Chemistry reaction, books a weekly transaction, and accrues IQ. When a
     * venture crosses {@link #MAJOR_MOVE_LEVEL} for the first time it fires the
     * Great Assimilation. Returns the net cash the desk booked this week.
     */
    public int settleWeek() {
        week++;
        int weekNet = 0;

        for (Venture v : ventures) {
            int settle = runReaction(v);
            v.lastSettleCents = settle;
            v.balanceCents += settle;
            if (v.balanceCents < 0) {
                v.balanceCents = 0;
            }
            weekNet += settle;

            // Sustained performance drives IQ; a bad week bleeds a point.
            if (settle > 0) {
                v.goodWeeks++;
                v.iq += IQ_PER_GOOD_WEEK;
            } else {
                v.goodWeeks = 0;
                if (v.iq > 0) {
                    v.iq--;
                }
            }

            // The Major Level-5 move — fires once, the first time a venture's
            // sustained genius reaches level 5.
            if (!v.majorFired && v.level() >= MAJOR_MOVE_LEVEL) {
                v.majorFired = true;
                majorMoves++;
                assimilationWeeksLeft = ASSIMILATION_WEEKS;
            }
        }

        if (assimilationWeeksLeft > 0) {
            assimilationWeeksLeft--;
        }

        deskProfitCents += weekNet;
        return weekNet;
    }

    /** Run a venture's Chemistry reaction for one week; returns the P/L cents. */
    private int runReaction(Venture v) {
        if (v.reaction == REACTION_EXOTHERMIC) {
            // Casino: an exothermic reaction — wide swings around a small
            // positive edge for a well-run house. The payout ranges roughly
            // -55%..+75% of a weekly "pot" sized by the stake, so it is
            // volatile week to week but favourable over a long campaign.
            int pot = Math.max(1, v.stakeCents / 10);
            int swing = randRange(-55, 76); // percent of the pot (mean ≈ +10%)
            return (pot * swing) / 100;
        }
        // Investment: a steady titration — a dependable weekly yield on the
        // current balance (about +0.8%..+2.0%), with an occasional, smaller
        // drawdown. Net clearly positive, which is what builds sustained IQ.
        int yieldBp = randRange(80, 200); // basis points of the balance
        int gain = (v.balanceCents * yieldBp) / 10000;
        if (randRange(0, 100) < 15) {
            gain -= (v.balanceCents * randRange(40, 120)) / 10000; // drawdown
        }
        return gain;
    }

    /**
     * The Edition 2 bridge into the win economy: given a month's raw
     * Major-Eastern revenue, return it scaled by the Great Assimilation when it
     * is active. During the assimilation weeks this multiplies Eastern revenue
     * by {@link #ASSIMILATION_EAST_PCT} — the reordering of the Orient that
     * rakes in the money; otherwise it returns the revenue unchanged.
     */
    public int applyAssimilation(int eastRevenueCents) {
        if (assimilationWeeksLeft > 0) {
            return (eastRevenueCents * ASSIMILATION_EAST_PCT) / 100;
        }
        return eastRevenueCents;
    }

    // --- read-side accessors for the UI / tests ---------------------------
    public boolean assimilationActive() {
        return assimilationWeeksLeft > 0;
    }

    public int assimilationWeeksLeft() {
        return assimilationWeeksLeft;
    }

    public int majorMoves() {
        return majorMoves;
    }

    public int week() {
        return week;
    }

    public int deskProfitCents() {
        return deskProfitCents;
    }

    public int ventureCount() {
        return ventures.size();
    }

    public List<Venture> ventures() {
        return ventures;
    }

    /** A compact, pipe-delimited snapshot line, mirroring the Wrapper. */
    public String snapshot() {
        StringBuilder sb = new StringBuilder();
        sb.append("DESK|week=").append(week)
                .append("|empty=").append(ventures.isEmpty() ? "yes" : "no")
                .append("|ventures=").append(ventures.size())
                .append("|deskProfit=").append(deskProfitCents)
                .append("|majorMoves=").append(majorMoves)
                .append("|assimilation=").append(assimilationActive() ? "yes" : "no")
                .append("|assimLeft=").append(assimilationWeeksLeft);
        for (Venture v : ventures) {
            sb.append("\nVEN|name=").append(v.name)
                    .append("|reaction=").append(v.reaction == REACTION_EXOTHERMIC
                            ? "exothermic" : "titration")
                    .append("|balance=").append(v.balanceCents)
                    .append("|iq=").append(v.iq)
                    .append("|level=").append(v.level())
                    .append("|lastSettle=").append(v.lastSettleCents)
                    .append("|major=").append(v.majorFired ? "yes" : "no");
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // Headless self-play demo: open both side games and run a long campaign so
    // a reviewer sees the desk fill, the IQ climb, the Level-5 move fire, and
    // the Great Assimilation rake in money.
    // ---------------------------------------------------------------------
    public static void main(String[] args) {
        BusinessDesk desk = new BusinessDesk();
        System.out.println("Business Desk — empty? " + desk.isEmpty());
        desk.openCasino(400000);       // $4,000 at the tables
        desk.openInvestment(600000);   // $6,000 under management
        System.out.println("Opened ventures — empty? " + desk.isEmpty());

        int firstMajorWeek = -1;
        for (int w = 1; w <= 60; w++) {
            desk.settleWeek();
            if (firstMajorWeek < 0 && desk.majorMoves() > 0) {
                firstMajorWeek = w;
            }
            if (w % 10 == 0 || w == firstMajorWeek) {
                System.out.printf("week %-2d  deskProfit=$%-7d  majorMoves=%d  %s%n",
                        w, desk.deskProfitCents() / 100, desk.majorMoves(),
                        desk.assimilationActive()
                                ? "GREAT ASSIMILATION — raking it in (" + desk.assimilationWeeksLeft() + "w left)"
                                : "");
            }
        }
        System.out.println("First Level-5 move fired on week " + firstMajorWeek);
        System.out.println("Example: $240,000 East becomes $"
                + (desk.applyAssimilation(24_000_000) / 100)
                + " while the assimilation runs.");
    }
}
