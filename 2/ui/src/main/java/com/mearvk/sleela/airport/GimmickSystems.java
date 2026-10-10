package com.mearvk.sleela.airport;

/**
 * GimmickSystems &mdash; the concrete, deterministic subsystems behind the
 * {@link EditionGimmicks} ladder.
 *
 * <p>{@link EditionGimmicks} says <em>which</em> systems a given edition lights
 * up and names the mastery theme; the small models here are the <em>working
 * parts</em> each new edition bolts on. They are intentionally compact,
 * seed-deterministic, and free of any UI dependency so the whole ladder is
 * reproducible and unit-testable, and so later editions can stack them on top
 * of the Edition 1 tower loop without rewriting it.
 *
 * <p>Each model keeps the "decision-support" contract: it exposes a condition,
 * a recommended action, and a mutator the player (or an Edition 7 policy)
 * drives. Nothing here acts irreversibly on its own.
 */
public final class GimmickSystems {

    private GimmickSystems() {
    }

    /** A tiny deterministic PRNG shared by the subsystems (xorshift-ish). */
    static int nextRand(int[] state) {
        int x = state[0];
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        if (x == 0) {
            x = 0x9e3779b9;
        }
        state[0] = x;
        return x & 0x7fffffff;
    }

    // =====================================================================
    // Edition 3 — WEATHER & RUNWAY CONDITIONS ("Read the environment")
    // =====================================================================
    /**
     * A weather band that drifts across the field and ices runways. Landings
     * into an iced runway are unsafe until the player de-ices it; the front
     * passes on its own after a while, so the skill is timing the gap.
     */
    public static final class Weather {
        public static final int CLEAR = 0;
        public static final int FRONT = 1;   // reduced visibility, risk rising
        public static final int STORM = 2;   // runways ice up

        private final int[] rng;
        private int phase = CLEAR;
        private int phaseTicks = 0;
        private final boolean[] iced;         // per-runway ice flag

        public Weather(int seed, int runways) {
            this.rng = new int[]{seed == 0 ? 1 : seed};
            this.iced = new boolean[Math.max(1, runways)];
        }

        public int phase() {
            return phase;
        }

        public boolean iced(int runwayIndex1Based) {
            int i = runwayIndex1Based - 1;
            return i >= 0 && i < iced.length && iced[i];
        }

        /** Player action: de-ice one runway. Returns true if it was iced. */
        public boolean deIce(int runwayIndex1Based) {
            int i = runwayIndex1Based - 1;
            if (i >= 0 && i < iced.length && iced[i]) {
                iced[i] = false;
                return true;
            }
            return false;
        }

        /** Safe to land on this runway now? (open + not iced). */
        public boolean safeToLand(int runwayIndex1Based) {
            return !iced(runwayIndex1Based);
        }

        public void tick() {
            phaseTicks++;
            // Advance the front on a deterministic cadence.
            if (phase == CLEAR && phaseTicks > 40 && (nextRand(rng) % 100) < 18) {
                phase = FRONT;
                phaseTicks = 0;
            } else if (phase == FRONT && phaseTicks > 20) {
                phase = STORM;
                phaseTicks = 0;
                for (int i = 0; i < iced.length; i++) {
                    iced[i] = true;         // storm ices every runway
                }
            } else if (phase == STORM && phaseTicks > 30) {
                phase = CLEAR;
                phaseTicks = 0;             // front passes; ice remains until de-iced
            }
        }

        public String recommend() {
            if (phase == STORM) {
                return "STORM: de-ice a runway, then land into the gap.";
            }
            if (phase == FRONT) {
                return "FRONT inbound: clear the backlog before the ice hits.";
            }
            return "CLEAR skies: good window to push departures.";
        }
    }

    // =====================================================================
    // Edition 4 — FLEET HEALTH & MAINTENANCE ("Sustain the machine")
    // =====================================================================
    /**
     * A fleet-wide use-rating that decays as planes cycle and recovers when the
     * player schedules maintenance. Below {@link #REVIEW_THRESHOLD} the
     * Maintenance Engineer flags the fleet; a flagged fleet earns a reduced
     * fare until serviced, so upkeep is a lever on revenue.
     */
    public static final class Fleet {
        public static final int REVIEW_THRESHOLD = 85;   // below => engineer review
        public static final int MAX_RATING = 100;

        private int useRating;
        private int cyclesSinceService = 0;

        public Fleet(int initialRating) {
            this.useRating = clamp(initialRating);
        }

        public int useRating() {
            return useRating;
        }

        public boolean needsReview() {
            return useRating < REVIEW_THRESHOLD;
        }

        /** Each completed departure wears the fleet a little. */
        public void onDeparture() {
            cyclesSinceService++;
            if (cyclesSinceService % 3 == 0) {
                useRating = clamp(useRating - 1);
            }
        }

        /** Player action: schedule maintenance; restores rating for a cost. */
        public int scheduleMaintenance() {
            int restore = MAX_RATING - useRating;
            useRating = MAX_RATING;
            cyclesSinceService = 0;
            return 40 + restore * 12;        // cash cost grows with neglect
        }

        /** A flagged fleet collects fares at this fraction until serviced. */
        public double fareFactor() {
            return needsReview() ? 0.75 : 1.0;
        }

        public String recommend() {
            if (needsReview()) {
                return "Use-rating " + useRating + " (<85): schedule a Maintenance "
                        + "Engineer review; fares are reduced until you do.";
            }
            return "Use-rating " + useRating + ": healthy. Keep cycling.";
        }

        private static int clamp(int r) {
            if (r < 0) {
                return 0;
            }
            return Math.min(r, MAX_RATING);
        }
    }

    // =====================================================================
    // Edition 5 — ROUTE NETWORK & RESERVATIONS ("Plan ahead")
    // =====================================================================
    /**
     * A forward book of reservations. The player books ahead; a healthy book
     * smooths arrivals and pays a steady network bonus, while an empty book
     * starves the gates. Reservations are consumed as planes depart.
     */
    public static final class Network {
        public static final int TARGET_BOOK = 20;      // the "full book" target
        private int reservations = 0;
        private int fulfilled = 0;

        public int reservations() {
            return reservations;
        }

        public int fulfilled() {
            return fulfilled;
        }

        /** Player action: book ahead. */
        public void book(int n) {
            if (n > 0) {
                reservations += n;
            }
        }

        /** A departure draws down the book and counts as fulfilled. */
        public void onDeparture() {
            if (reservations > 0) {
                reservations--;
                fulfilled++;
            }
        }

        /** Book health in [0,1] against the full-book target. */
        public double bookHealth() {
            double h = (double) reservations / TARGET_BOOK;
            return h > 1 ? 1 : h;
        }

        /** Steady per-departure network bonus when the book is well-stocked. */
        public int departureBonus() {
            return bookHealth() >= 0.5 ? 30 : 0;
        }

        public String recommend() {
            if (reservations == 0) {
                return "Book is empty: reserve ahead or the gates will starve.";
            }
            if (bookHealth() < 0.5) {
                return "Book thin (" + reservations + "): top up toward "
                        + TARGET_BOOK + " for the steady bonus.";
            }
            return "Book healthy (" + reservations + "): steady arrivals, bonus active.";
        }
    }

    // =====================================================================
    // Edition 6 — MULTI-TERMINAL EXPANSION ("Scale out")
    // =====================================================================
    /**
     * A set of continental terminals the player unlocks and balances. Each
     * terminal carries its own load; the hub score rewards keeping them level
     * rather than overloading one.
     */
    public static final class Terminals {
        private final int[] load;        // current load per terminal
        private int open;                // how many terminals are unlocked

        public Terminals(int totalTerminals, int openAtStart) {
            this.load = new int[Math.max(1, totalTerminals)];
            this.open = Math.max(1, Math.min(openAtStart, load.length));
        }

        public int open() {
            return open;
        }

        public int capacity() {
            return load.length;
        }

        /** Player action: unlock the next terminal. */
        public boolean unlockNext() {
            if (open < load.length) {
                open++;
                return true;
            }
            return false;
        }

        /** Route a departure to the least-loaded open terminal (balancing). */
        public int routeDeparture() {
            int best = 0;
            for (int i = 1; i < open; i++) {
                if (load[i] < load[best]) {
                    best = i;
                }
            }
            load[best]++;
            return best;
        }

        public int loadOf(int terminal) {
            return (terminal >= 0 && terminal < load.length) ? load[terminal] : 0;
        }

        /** Imbalance = max load - min load across open terminals (lower better). */
        public int imbalance() {
            int min = Integer.MAX_VALUE;
            int max = 0;
            for (int i = 0; i < open; i++) {
                min = Math.min(min, load[i]);
                max = Math.max(max, load[i]);
            }
            return (open == 0) ? 0 : (max - min);
        }

        public String recommend() {
            if (open < load.length && imbalance() <= 1) {
                return "Terminals level (" + open + "/" + load.length
                        + "): room to unlock another and scale out.";
            }
            if (imbalance() > 2) {
                return "Imbalance " + imbalance() + ": spread load before unlocking more.";
            }
            return "Running " + open + "/" + load.length + " terminals, balanced.";
        }
    }

    // =====================================================================
    // Edition 7 — AUTOMATION & POLICY ("Delegate control")
    // =====================================================================
    /**
     * A programmable policy the player configures once and the tower then
     * follows on its own: which systems to auto-manage. This is the AUTO button
     * turned into a rule set &mdash; delegation rather than per-plane clicking.
     */
    public static final class Automation {
        private boolean autoLand = true;
        private boolean autoDeIce = false;
        private boolean autoMaintain = false;
        private boolean autoBook = false;

        public boolean autoLand() {
            return autoLand;
        }

        public boolean autoDeIce() {
            return autoDeIce;
        }

        public boolean autoMaintain() {
            return autoMaintain;
        }

        public boolean autoBook() {
            return autoBook;
        }

        public void setAutoLand(boolean v) {
            autoLand = v;
        }

        public void setAutoDeIce(boolean v) {
            autoDeIce = v;
        }

        public void setAutoMaintain(boolean v) {
            autoMaintain = v;
        }

        public void setAutoBook(boolean v) {
            autoBook = v;
        }

        /** How many of the sub-systems are currently delegated. */
        public int delegatedCount() {
            int n = 0;
            if (autoLand) {
                n++;
            }
            if (autoDeIce) {
                n++;
            }
            if (autoMaintain) {
                n++;
            }
            if (autoBook) {
                n++;
            }
            return n;
        }

        public String policyLine() {
            return "POLICY|land=" + autoLand + "|deice=" + autoDeIce
                    + "|maintain=" + autoMaintain + "|book=" + autoBook;
        }

        public String recommend() {
            return "Delegated " + delegatedCount() + "/4 systems. Program the policy, "
                    + "then supervise exceptions instead of every plane.";
        }
    }

    // =====================================================================
    // Edition 8 — LIVE COMPETITIVE ECONOMY ("Compete")
    // =====================================================================
    /**
     * The read-only live score published to a server. It is derived from the
     * authoritative tower state plus the bonuses each lower system contributes,
     * so one stable competitive number reflects the whole stack running at
     * once. Mirrors {@code liveScore()} in {@code AirportTycoon.sleela}.
     */
    public static final class LiveEconomyScore {
        public static int score(int cash, int reputation, int served, int streak) {
            return cash * 10 + reputation * 100 + served * 50 + streak * 25;
        }

        public static String line(String serverId, String region, boolean online,
                                  int players, int score) {
            return "LIVE|server=" + serverId + "|region=" + region
                    + "|online=" + (online ? "yes" : "no")
                    + "|players=" + players + "|score=" + score;
        }
    }
}
