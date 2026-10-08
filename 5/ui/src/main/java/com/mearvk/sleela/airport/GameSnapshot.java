package com.mearvk.sleela.airport;

import java.util.ArrayList;
import java.util.List;

/**
 * An immutable frame of game state, as produced by the SLeeLa Wrapper's
 * {@code snapshot()} and consumed by the JavaFX renderer.
 *
 * <p>The wire format mirrors {@code game/AirportTycoon.sleela}'s
 * {@code snapshot()}: a pipe-delimited header line followed by one
 * {@code structPack}-style JSON object per live plane. This class owns parsing
 * that form so both the process-backed runtime and the local model emit the
 * same thing.
 */
public final class GameSnapshot {

    /** Plane lifecycle states, matching the PLANE_* constants in the Wrapper. */
    public static final int INBOUND = 1;
    public static final int LANDING = 2;
    public static final int TAXI_IN = 3;
    public static final int AT_GATE = 4;
    public static final int DEPARTING = 6;

    public final int tick;
    public final int cash;
    public final int reputation;
    public final int served;
    public final int lost;
    public final int openGates;
    public final int openRunways;
    public final int planeCount;
    public final boolean gameOver;
    public final List<PlaneView> planes;

    // Edition 2 feedback-loop readouts (default to neutral when a frame
    // predates the fields, so older snapshots still parse).
    public final int streak;            // consecutive on-time departures
    public final int servicePremiumPct; // reputation-driven fare multiplier (%)

    // Edition 3 Prosperity Contract readouts.
    public final int contractTargetSize;
    public final int contractProgress;
    public final int contractGoal;
    public final int contractTicksLeft;
    public final int contractsCompleted;
    public final int contractEarlyWindow;
    public final int contractEarlyBonus;
    public final int contractChain;
    public final int contractLadderStep;
    public final int contractLadderCap;
    public final int reserveFloor;
    public final int reserveTicks;
    public final int reserveGoal;
    public final int reserveBonus;
    public final int reserveAwards;

    /** Edition 2 constructor carrying the feedback-loop readouts. */
    public GameSnapshot(int tick, int cash, int reputation, int served, int lost,
                        int openGates, int openRunways, int planeCount,
                        boolean gameOver, List<PlaneView> planes,
                        int streak, int servicePremiumPct,
                        int contractTargetSize, int contractProgress,
                        int contractGoal, int contractTicksLeft,
                        int contractsCompleted, int contractEarlyWindow,
                        int contractEarlyBonus, int contractChain,
                        int contractLadderStep, int contractLadderCap,
                        int reserveFloor, int reserveTicks, int reserveGoal,
                        int reserveBonus, int reserveAwards) {
        this.tick = tick;
        this.cash = cash;
        this.reputation = reputation;
        this.served = served;
        this.lost = lost;
        this.openGates = openGates;
        this.openRunways = openRunways;
        this.planeCount = planeCount;
        this.gameOver = gameOver;
        this.planes = planes;
        this.streak = streak;
        this.servicePremiumPct = servicePremiumPct;
        this.contractTargetSize = contractTargetSize;
        this.contractProgress = contractProgress;
        this.contractGoal = contractGoal;
        this.contractTicksLeft = contractTicksLeft;
        this.contractsCompleted = contractsCompleted;
        this.contractEarlyWindow = contractEarlyWindow;
        this.contractEarlyBonus = contractEarlyBonus;
        this.contractChain = contractChain;
        this.contractLadderStep = contractLadderStep;
        this.contractLadderCap = contractLadderCap;
        this.reserveFloor = reserveFloor;
        this.reserveTicks = reserveTicks;
        this.reserveGoal = reserveGoal;
        this.reserveBonus = reserveBonus;
        this.reserveAwards = reserveAwards;
    }

    /** Edition 1-compatible constructor; neutral premium (100%), no streak. */
    public GameSnapshot(int tick, int cash, int reputation, int served, int lost,
                        int openGates, int openRunways, int planeCount,
                        boolean gameOver, List<PlaneView> planes) {
        this(tick, cash, reputation, served, lost, openGates, openRunways,
                planeCount, gameOver, planes, 0, 100, 0, 0, 5, 0, 0, 120, 250, 0, 100, 4, 3000, 0, 180, 750, 0);
    }

    /** A single plane as the UI needs it for animation. */
    public static final class PlaneView {
        public final int id;
        public final String flight;
        public final int state;
        public final int sizeClass;
        public final int patience;
        public final int maxPatience;
        public final int serviceLeft;
        public final double posX;
        public final double posY;

        public PlaneView(int id, String flight, int state, int sizeClass,
                         int patience, int maxPatience, int serviceLeft,
                         double posX, double posY) {
            this.id = id;
            this.flight = flight;
            this.state = state;
            this.sizeClass = sizeClass;
            this.patience = patience;
            this.maxPatience = maxPatience;
            this.serviceLeft = serviceLeft;
            this.posX = posX;
            this.posY = posY;
        }

        public double patienceFraction() {
            if (maxPatience <= 0) {
                return 1.0;
            }
            double f = (double) patience / (double) maxPatience;
            if (f < 0) {
                return 0;
            }
            if (f > 1) {
                return 1;
            }
            return f;
        }
    }

    /**
     * Parse the Wrapper's snapshot wire format. Tolerant of missing fields so
     * the UI never crashes on a partially written frame.
     */
    public static GameSnapshot parse(String text) {
        int tick = 0, cash = 0, rep = 0, served = 0, lost = 0;
        int gates = 0, runways = 0, planeCount = 0;
        int streak = 0, premium = 100;
        int contractTarget = 0, contractProgress = 0, contractGoal = 5;
        int contractLeft = 0, contracts = 0, contractEarlyWindow = 120, contractEarlyBonus = 250;
        int contractChain = 0, contractLadderStep = 100, contractLadderCap = 4;
        int reserveFloor = 3000, reserveTicks = 0, reserveGoal = 180, reserveBonus = 750, reserveAwards = 0;
        boolean over = false;
        List<PlaneView> planes = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return new GameSnapshot(0, 0, 0, 0, 0, 0, 0, 0, false, planes);
        }

        String[] lines = text.split("\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("ATC|")) {
                for (String kv : line.substring(4).split("\\|")) {
                    int eq = kv.indexOf('=');
                    if (eq < 0) {
                        continue;
                    }
                    String k = kv.substring(0, eq);
                    String v = kv.substring(eq + 1);
                    switch (k) {
                        case "tick" -> tick = parseInt(v);
                        case "cash" -> cash = parseInt(v);
                        case "rep" -> rep = parseInt(v);
                        case "served" -> served = parseInt(v);
                        case "lost" -> lost = parseInt(v);
                        case "gates" -> gates = parseInt(v);
                        case "runways" -> runways = parseInt(v);
                        case "planes" -> planeCount = parseInt(v);
                        case "over" -> over = Boolean.parseBoolean(v);
                        case "streak" -> streak = parseInt(v);
                        case "premium" -> premium = parseInt(v);
                        case "contractTarget" -> contractTarget = parseInt(v);
                        case "contractProgress" -> contractProgress = parseInt(v);
                        case "contractGoal" -> contractGoal = parseInt(v);
                        case "contractLeft" -> contractLeft = parseInt(v);
                        case "contracts" -> contracts = parseInt(v);
                        case "contractEarlyWindow" -> contractEarlyWindow = parseInt(v);
                        case "contractEarlyBonus" -> contractEarlyBonus = parseInt(v);
                        case "contractChain" -> contractChain = parseInt(v);
                        case "contractLadderStep" -> contractLadderStep = parseInt(v);
                        case "contractLadderCap" -> contractLadderCap = parseInt(v);
                        case "reserveFloor" -> reserveFloor = parseInt(v);
                        case "reserveTicks" -> reserveTicks = parseInt(v);
                        case "reserveGoal" -> reserveGoal = parseInt(v);
                        case "reserveBonus" -> reserveBonus = parseInt(v);
                        case "reserveAwards" -> reserveAwards = parseInt(v);
                        default -> { /* ignore unknown keys */ }
                    }
                }
            } else if (line.startsWith("{")) {
                PlaneView p = parsePlane(line);
                if (p != null) {
                    planes.add(p);
                }
            }
        }
        return new GameSnapshot(tick, cash, rep, served, lost, gates, runways,
                planeCount, over, planes, streak, premium,
                contractTarget, contractProgress, contractGoal,
                contractLeft, contracts, contractEarlyWindow, contractEarlyBonus,
                contractChain, contractLadderStep, contractLadderCap,
                reserveFloor, reserveTicks, reserveGoal, reserveBonus, reserveAwards);
    }

    /** Minimal, dependency-free parse of a flat structPack JSON object. */
    private static PlaneView parsePlane(String json) {
        String body = json.trim();
        if (body.startsWith("{")) {
            body = body.substring(1);
        }
        if (body.endsWith("}")) {
            body = body.substring(0, body.length() - 1);
        }
        int id = 0, state = 0, sizeClass = 0, patience = 0, maxP = 0, svc = 0;
        double x = 0, y = 0;
        String flight = "";

        for (String field : splitTopLevel(body)) {
            int colon = field.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = unquote(field.substring(0, colon).trim());
            String val = field.substring(colon + 1).trim();
            switch (key) {
                case "id" -> id = parseInt(val);
                case "flight" -> flight = unquote(val);
                case "state" -> state = parseInt(val);
                case "sizeClass" -> sizeClass = parseInt(val);
                case "patience" -> patience = parseInt(val);
                case "maxPatience" -> maxP = parseInt(val);
                case "serviceLeft" -> svc = parseInt(val);
                case "posX" -> x = parseDouble(val);
                case "posY" -> y = parseDouble(val);
                default -> { /* __type and links ignored */ }
            }
        }
        if (id == 0) {
            return null;
        }
        return new PlaneView(id, flight, state, sizeClass, patience, maxP, svc, x, y);
    }

    private static List<String> splitTopLevel(String body) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        boolean inStr = false;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"' && (i == 0 || body.charAt(i - 1) != '\\')) {
                inStr = !inStr;
            }
            if (!inStr && (c == '{' || c == '[')) {
                depth++;
            }
            if (!inStr && (c == '}' || c == ']')) {
                depth--;
            }
            if (!inStr && c == ',' && depth == 0) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }

    private static String unquote(String s) {
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
