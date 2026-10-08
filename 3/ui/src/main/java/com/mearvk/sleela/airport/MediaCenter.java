package com.mearvk.sleela.airport;

import java.util.ArrayList;
import java.util.List;

/**
 * A faithful Java mirror of the media / events layer —
 * {@code 1/media/EventsCenter.sleela} (the 300-rated center) and
 * {@code 1/media/Reporter.sleela} (the 220-rated Boss who fuses the Author
 * viewpoint with East 5.0).
 *
 * <p>First Edition focus: Asia routes (the Great area) and Euro-American routes
 * (Northern Europe + Chinese-influence hubs near the UK and France, feeding the
 * Americas). Prices run on six real tiers 1.0&ndash;6.0 (6 the most luxurious),
 * flights are mainly on time but measured and contained by each country's three
 * national theories, and a soft economic pad smooths the figures.
 */
public final class MediaCenter {

    public static final int CENTER_IQ = 300;
    public static final int REPORTER_IQ = 220;
    public static final int EAST_VERSION_X10 = 50; // East 5.0

    static final int TIER_MIN_X10 = 10; // 1.0
    static final int TIER_MAX_X10 = 60; // 6.0
    static final int SOFT_PAD_PERCENT = 35;

    /** A country, contained by exactly three national theories. */
    public static final class Country {
        final int id;
        final String name;
        final String region;      // "ASIA" or "EUAM"
        final String theory1;     // On-Time Doctrine
        final int onTimeCeiling;
        final String theory2;     // Containment Theory
        final int delayDamping;
        final String theory3;     // Market Theory
        int baseTierX10;

        Country(int id, String name, String region, String t1, int ceiling,
                String t2, int damping, String t3, int baseTierX10) {
            this.id = id;
            this.name = name;
            this.region = region;
            this.theory1 = t1;
            this.onTimeCeiling = ceiling;
            this.theory2 = t2;
            this.delayDamping = damping;
            this.theory3 = t3;
            this.baseTierX10 = clampTier(baseTierX10);
        }

        public String[] theories() {
            return new String[]{theory1, theory2, theory3};
        }
    }

    /** A focus route between two country hubs. */
    public static final class Route {
        final int id;
        final String label;
        final int fromId;
        final int toId;
        final String region;
        int tierX10;
        int onTimePct;
        int demand;

        Route(int id, String label, int fromId, int toId, String region) {
            this.id = id;
            this.label = label;
            this.fromId = fromId;
            this.toId = toId;
            this.region = region;
            this.onTimePct = 90;
            this.demand = 50;
        }

        public String tierLabel() {
            return (tierX10 / 10) + "." + (tierX10 % 10);
        }
    }

    private final List<Country> countries = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();
    private int cycle;
    private int rngState;
    private boolean padInit;
    private int prevFareIndex;

    // Reporter published figures.
    private int pubAsiaOnTime;
    private int pubEuamOnTime;
    private int pubFareIndex;
    private int confidence;
    private String headline = "";

    public MediaCenter() {
        build();
    }

    public void build() {
        cycle = 0;
        rngState = 700300;
        padInit = false;
        prevFareIndex = 0;
        countries.clear();
        routes.clear();

        // Asia — the Great area the game focuses on.
        countries.add(new Country(1, "China", "ASIA",
                "High-Speed Punctuality Doctrine", 96,
                "State Flow Containment", 80,
                "Belt Luxury Market", 40));
        countries.add(new Country(2, "Japan", "ASIA",
                "Teiji Un'ei (on-time operations)", 98,
                "Kaizen Delay Containment", 88,
                "Omotenashi Premium Market", 50));
        // Euro-American focus.
        countries.add(new Country(3, "United Kingdom", "EUAM",
                "Slot Discipline Doctrine", 90,
                "Heathrow Flow Containment", 72,
                "City Premium Market", 45));
        countries.add(new Country(4, "France", "EUAM",
                "Ponctualite Nationale", 91,
                "ATC Containment Nationale", 74,
                "Luxe Market", 55));
        countries.add(new Country(5, "Chile (Americas gateway)", "EUAM",
                "Puntualidad Andina", 88,
                "Pacific Flow Containment", 68,
                "Americas Access Market", 30));

        addRoute(1, "Shanghai -> Tokyo", 1, 2, "ASIA");
        addRoute(2, "Tokyo -> Shanghai", 2, 1, "ASIA");
        addRoute(3, "London -> Santiago (AM)", 3, 5, "EUAM");
        addRoute(4, "Paris -> Santiago (AM)", 4, 5, "EUAM");
        addRoute(5, "Shanghai -> London", 1, 3, "EUAM");
        addRoute(6, "Shanghai -> Paris", 1, 4, "EUAM");
    }

    private void addRoute(int id, String label, int fromId, int toId, String region) {
        Route r = new Route(id, label, fromId, toId, region);
        Country a = countryById(fromId);
        Country b = countryById(toId);
        r.tierX10 = clampTier(a != null && b != null
                ? (a.baseTierX10 + b.baseTierX10) / 2 : 40);
        routes.add(r);
    }

    private Country countryById(int id) {
        for (Country c : countries) {
            if (c.id == id) {
                return c;
            }
        }
        return null;
    }

    static int clampTier(int x10) {
        if (x10 < TIER_MIN_X10) {
            return TIER_MIN_X10;
        }
        return Math.min(x10, TIER_MAX_X10);
    }

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

    /** Fare for a real luxury tier 1.0..6.0; tier 6 is the most luxurious. */
    public static int fareForTierX10(int tierX10) {
        int t = tierX10 / 10;
        return switch (t <= 1 ? 1 : Math.min(t, 6)) {
            case 1 -> 120;
            case 2 -> 200;
            case 3 -> 320;
            case 4 -> 520;
            case 5 -> 840;
            default -> 1360; // tier 6
        };
    }

    /** The soft economic pad: blend current toward target, gently. */
    static int softPad(int current, int target) {
        return current + (target - current) * (100 - SOFT_PAD_PERCENT) / 100;
    }

    static int clampPct(int v) {
        if (v < 0) {
            return 0;
        }
        return Math.min(v, 100);
    }

    /** Advance one events cycle: flights mainly on time, contained by theories. */
    public void tick() {
        cycle++;
        for (Route r : routes) {
            Country origin = countryById(r.fromId);
            if (origin == null) {
                continue;
            }
            int raw = 92 + randRange(0, 8);
            if (randRange(0, 100) < 12) {
                raw -= randRange(10, 30); // a background delay event
            }
            if (raw > origin.onTimeCeiling) {
                raw = origin.onTimeCeiling; // Theory 1: ceiling
            }
            int gap = origin.onTimeCeiling - raw;
            r.onTimePct = clampPct(raw + gap * origin.delayDamping / 100); // Theory 2
            int target = (origin.baseTierX10 + r.tierX10) / 2;             // Theory 3
            r.tierX10 = clampTier(softPad(r.tierX10, target));
            int demandTarget = Math.max(10, 30 + (r.onTimePct - 70));
            r.demand = softPad(r.demand, demandTarget);
        }
    }

    public int regionOnTime(String region) {
        int sum = 0, n = 0;
        for (Route r : routes) {
            if (r.region.equals(region)) {
                sum += r.onTimePct;
                n++;
            }
        }
        return n == 0 ? 0 : sum / n;
    }

    public int regionFareIndex(String region) {
        int sum = 0, n = 0;
        for (Route r : routes) {
            if (r.region.equals(region)) {
                sum += fareForTierX10(r.tierX10);
                n++;
            }
        }
        if (n == 0) {
            return 0;
        }
        int raw = sum / n;
        if (!padInit) {
            prevFareIndex = raw;
            padInit = true;
        }
        prevFareIndex = softPad(prevFareIndex, raw);
        return prevFareIndex;
    }

    // --- Reporter (the 220 Boss): fuse Author viewpoint with East 5.0 ---

    public void report() {
        int asiaOnTime = regionOnTime("ASIA");
        int euamOnTime = regionOnTime("EUAM");
        int asiaFare = regionFareIndex("ASIA");
        int euamFare = regionFareIndex("EUAM");

        int eastAsia = clampPct(asiaOnTime + 2);   // East 5.0 reads Asia with a lift
        pubAsiaOnTime = blend(eastAsia, 70, asiaOnTime, 30);   // East leads Asia
        pubEuamOnTime = blend(euamOnTime, 70, euamOnTime, 30); // Author leads Euro-Am
        pubFareIndex = (asiaFare + euamFare) / 2;

        int agree = 100 - Math.abs(pubAsiaOnTime - pubEuamOnTime);
        int quality = (pubAsiaOnTime + pubEuamOnTime) / 2;
        confidence = clampPct((agree + quality) / 2);

        headline = (pubAsiaOnTime >= pubEuamOnTime
                ? "Asia leads: East 5.0 reads strong punctuality on the focus routes"
                : "Euro-American routes lead on the Author's disciplined read")
                + " — Asia " + pubAsiaOnTime + "% on time, Euro-Am "
                + pubEuamOnTime + "% on time, fare index $" + pubFareIndex
                + " (confidence " + confidence + ").";
    }

    static int blend(int a, int wa, int b, int wb) {
        int total = wa + wb;
        return total <= 0 ? (a + b) / 2 : clampPct((a * wa + b * wb) / total);
    }

    public static String eastVersion() {
        return "East " + (EAST_VERSION_X10 / 10) + "." + (EAST_VERSION_X10 % 10);
    }

    // --- read-side for the UI / tests ---
    public int cycle() {
        return cycle;
    }

    public int publishedAsiaOnTime() {
        return pubAsiaOnTime;
    }

    public int publishedEuamOnTime() {
        return pubEuamOnTime;
    }

    public int publishedFareIndex() {
        return pubFareIndex;
    }

    public int confidence() {
        return confidence;
    }

    public String headline() {
        return headline;
    }

    public List<Route> routes() {
        return routes;
    }

    public List<Country> countries() {
        return countries;
    }
}
