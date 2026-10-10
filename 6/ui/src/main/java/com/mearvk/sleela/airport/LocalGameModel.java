package com.mearvk.sleela.airport;

import java.util.ArrayList;
import java.util.List;

/**
 * A faithful Java mirror of {@code game/AirportTycoon.sleela}.
 *
 * <p>This exists so the Sleela UI and its animation run on any machine, even
 * when the SLeeLa compiler ({@code Sleelvac}) is not installed. The rules,
 * constants, deterministic PRNG, fares, patience model, and difficulty ramp are
 * kept byte-for-byte faithful to the Wrapper so a developer reviewing both sees
 * the same game. When the SLeeLa toolchain is present, prefer
 * {@link SleelaProcessRuntime}, which runs the real Wrapper.
 */
public final class LocalGameModel implements SleelaRuntime {

    // The edition (1..8) this model is playing. Determines which systems of
    // the ordered mastery ladder are live; see EditionGimmicks. Edition 8 (the
    // default) runs every system at once.
    private final EditionGimmicks gimmicks;

    // The cumulative gimmick subsystems. Each is non-null only when its edition
    // (or later) is being played, so Edition N runs systems 1..N.
    private GimmickSystems.Weather weather;      // Ed. 3+
    private GimmickSystems.Fleet fleet;          // Ed. 4+
    private GimmickSystems.Network network;      // Ed. 5+
    private GimmickSystems.Terminals terminals;  // Ed. 6+
    private GimmickSystems.Automation automation;// Ed. 7+

    static final int TOTAL_TERMINALS = 4;        // Ed. 6 continental terminals

    public LocalGameModel() {
        this(EditionConfig.EDITION);
    }

    public LocalGameModel(int edition) {
        this.gimmicks = new EditionGimmicks(edition);
        reset();
    }

    public EditionGimmicks gimmicks() {
        return gimmicks;
    }

    // Mirrors AirportTycoon.sleela constants.
    static final int MAX_GATES = 8;
    static final int MAX_RUNWAYS = 3;

    // --- Edition 2: economic feedback loop (service premium + on-time combo) ---
    // Fares are scaled by a "service premium" tied to reputation: a well-run,
    // high-reputation airport earns tips on every departure, while a struggling
    // one must discount to keep flying. The premium is expressed in percent and
    // clamped to [MIN, MAX]. At the baseline reputation (REF) the premium is
    // exactly 100% (no change), so Edition 1's fares are the neutral midpoint.
    static final int PREMIUM_REF_REP = 60;   // reputation giving a neutral 100%
    static final int PREMIUM_MIN_PCT = 70;   // worst-case discount floor
    static final int PREMIUM_MAX_PCT = 150;  // best-case tip ceiling

    // On-time combo: every CsomboStep consecutive on-time departures (no plane
    // has left angry since the last reset) pays a flat combo tip, rewarding
    // sustained, clean tower management. A single abandonment breaks the combo.
    static final int COMBO_STEP = 5;         // departures per combo milestone
    static final int COMBO_TIP = 150;        // cash tip at each milestone

    static final int INBOUND = 1;
    static final int LANDING = 2;
    static final int TAXI_IN = 3;
    static final int AT_GATE = 4;
    static final int DEPARTING = 6;
    static final int DONE = 7;
    static final int ANGRY = 8;

    private static final class Plane {
        int id;
        String flight;
        int state;
        int sizeClass;
        int patience;
        int maxPatience;
        int gateIdx;    // 1-based, 0 = none
        int runwayIdx;  // 1-based, 0 = none
        int serviceLeft;
        double posX;
        double posY;
    }

    private static final class Gate {
        int index;
        int occupant; // plane id, 0 = free
        boolean open;
    }

    private static final class Runway {
        int index;
        int occupant;
        int cooldown;
        boolean open;
    }

    private final List<Plane> planes = new ArrayList<>();
    private final List<Gate> gates = new ArrayList<>();
    private final List<Runway> runways = new ArrayList<>();

    private int cash;
    private int reputation;
    private int served;
    private int lost;
    private int tick;
    private int rngState;
    private int spawnTimer;
    private int spawnEvery;
    private int openGates;
    private int openRunways;
    private int nextPlaneId;
    private boolean gameOver;

    // Edition 2 feedback-loop state.
    private int streak;        // consecutive on-time departures (resets on abandon)
    private int comboTips;     // cash earned from combo milestones this shift

    @Override
    public void reset() {
        planes.clear();
        gates.clear();
        runways.clear();

        cash = 2000;
        reputation = 100;
        served = 0;
        lost = 0;
        tick = 0;
        rngState = 123457;
        spawnTimer = 20;
        spawnEvery = 20;
        openGates = 2;
        openRunways = 1;
        nextPlaneId = 1;
        gameOver = false;
        streak = 0;
        comboTips = 0;

        for (int i = 1; i <= MAX_GATES; i++) {
            Gate g = new Gate();
            g.index = i;
            g.occupant = 0;
            g.open = i <= openGates;
            gates.add(g);
        }
        for (int i = 1; i <= MAX_RUNWAYS; i++) {
            Runway r = new Runway();
            r.index = i;
            r.occupant = 0;
            r.cooldown = 0;
            r.open = i <= openRunways;
            runways.add(r);
        }

        // --- Light the ordered mastery ladder for this edition --------------
        // Each subsystem is created only from the edition that introduces it,
        // so Edition N runs systems 1..N (see EditionGimmicks / GimmickSystems).
        weather = gimmicks.has(EditionGimmicks.System.WEATHER)
                ? new GimmickSystems.Weather(rngState ^ 0x5EED, MAX_RUNWAYS) : null;
        fleet = gimmicks.has(EditionGimmicks.System.FLEET)
                ? new GimmickSystems.Fleet(92) : null;
        network = gimmicks.has(EditionGimmicks.System.NETWORK)
                ? new GimmickSystems.Network() : null;
        terminals = gimmicks.has(EditionGimmicks.System.TERMINALS)
                ? new GimmickSystems.Terminals(TOTAL_TERMINALS, 1) : null;
        automation = gimmicks.has(EditionGimmicks.System.AUTOMATION)
                ? new GimmickSystems.Automation() : null;
    }

    // --- Accessors for the cumulative subsystems (null when not in edition) --
    GimmickSystems.Weather weather() {
        return weather;
    }

    GimmickSystems.Fleet fleet() {
        return fleet;
    }

    GimmickSystems.Network network() {
        return network;
    }

    GimmickSystems.Terminals terminals() {
        return terminals;
    }

    GimmickSystems.Automation automation() {
        return automation;
    }

    int liveScore() {
        return GimmickSystems.LiveEconomyScore.score(cash, reputation, served, streak);
    }

    // --- deterministic PRNG, matching the Wrapper ---
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

    private Gate gateAt(int index) {
        for (Gate g : gates) {
            if (g.index == index) {
                return g;
            }
        }
        return null;
    }

    private Runway runwayAt(int index) {
        for (Runway r : runways) {
            if (r.index == index) {
                return r;
            }
        }
        return null;
    }

    private Gate freeGate() {
        for (Gate g : gates) {
            if (g.open && g.occupant == 0) {
                return g;
            }
        }
        return null;
    }

    private Runway freeRunway() {
        // Prefer a runway that is also safe to land on (Edition 3+ weather);
        // fall back to any free runway so the de-ice path still has a target.
        Runway anyFree = null;
        for (Runway r : runways) {
            if (r.open && r.occupant == 0 && r.cooldown == 0) {
                if (weather == null || weather.safeToLand(r.index)) {
                    return r;
                }
                if (anyFree == null) {
                    anyFree = r;
                }
            }
        }
        return anyFree;
    }

    private Plane planeById(int id) {
        for (Plane p : planes) {
            if (p.id == id) {
                return p;
            }
        }
        return null;
    }

    /** Edition 1 base fare for a size class (the neutral, pre-premium value). */
    private int baseFareFor(int sizeClass) {
        if (sizeClass == 0) {
            return 120;
        }
        if (sizeClass == 1) {
            return 260;
        }
        return 540;
    }

    /**
     * Edition 2 service premium (percent): scales linearly with reputation
     * around a neutral 100% at {@link #PREMIUM_REF_REP}, clamped to
     * [{@link #PREMIUM_MIN_PCT}, {@link #PREMIUM_MAX_PCT}]. One point of
     * reputation moves the premium by one percent, so reputation now has a
     * direct, legible effect on the bottom line.
     */
    int servicePremiumPct() {
        int pct = 100 + (reputation - PREMIUM_REF_REP);
        if (pct < PREMIUM_MIN_PCT) {
            pct = PREMIUM_MIN_PCT;
        }
        if (pct > PREMIUM_MAX_PCT) {
            pct = PREMIUM_MAX_PCT;
        }
        return pct;
    }

    /**
     * The actual fare paid on departure: Edition 1 base fare, scaled by the
     * Edition 2 service premium, then (Edition 4+) by the fleet-health fare
     * factor &mdash; a neglected fleet collects reduced fares until serviced.
     */
    private int fareFor(int sizeClass) {
        int fare = (baseFareFor(sizeClass) * servicePremiumPct()) / 100;
        if (fleet != null) {
            fare = (int) Math.round(fare * fleet.fareFactor());
        }
        return fare;
    }

    private void spawnPlane() {
        if (planes.size() >= 24) {
            return;
        }
        int sizeClass = randRange(0, 3);
        int patience = 90 - sizeClass * 10 + randRange(0, 20);

        Plane p = new Plane();
        p.id = nextPlaneId;
        p.flight = "SL" + nextPlaneId;
        p.state = INBOUND;
        p.sizeClass = sizeClass;
        p.patience = patience;
        p.maxPatience = patience;
        p.gateIdx = 0;
        p.runwayIdx = 0;
        p.serviceLeft = 10 + sizeClass * 6;
        p.posX = 0.05;
        p.posY = 0.15 + (nextPlaneId % 5) * 0.12;
        planes.add(0, p);

        nextPlaneId++;
    }

    @Override
    public boolean clearToLand(int planeId) {
        Plane p = planeById(planeId);
        if (p == null || p.state != INBOUND) {
            return false;
        }
        Runway rw = freeRunway();
        if (rw == null) {
            return false;
        }
        // Edition 3+: refuse to clear a landing onto an iced runway. The player
        // must de-ice first (or wait for a safe one), which is the point of the
        // weather system.
        if (weather != null && !weather.safeToLand(rw.index)) {
            return false;
        }
        p.state = LANDING;
        p.runwayIdx = rw.index;
        rw.occupant = planeId;
        rw.cooldown = 3;
        return true;
    }

    /** Edition 3+ player action: de-ice a runway so planes can land on it. */
    boolean deIce(int runwayIndex) {
        return weather != null && weather.deIce(runwayIndex);
    }

    /** Edition 4+ player action: schedule fleet maintenance. Returns true if paid. */
    boolean scheduleMaintenance() {
        if (fleet == null) {
            return false;
        }
        int cost = fleet.scheduleMaintenance();
        cash = Math.max(0, cash - cost);
        return true;
    }

    /** Edition 5+ player action: book reservations ahead. */
    boolean book(int n) {
        if (network == null) {
            return false;
        }
        network.book(n);
        return true;
    }

    /** Edition 6+ player action: unlock the next continental terminal. */
    boolean unlockTerminal() {
        return terminals != null && terminals.unlockNext();
    }

    @Override
    public boolean assignGate(int planeId, int gateIndex) {
        Plane p = planeById(planeId);
        if (p == null || (p.state != TAXI_IN && p.state != LANDING)) {
            return false;
        }
        Gate g = gateIndex == 0 ? freeGate() : gateAt(gateIndex);
        if (g == null || !g.open || g.occupant != 0) {
            return false;
        }
        p.gateIdx = g.index;
        p.state = TAXI_IN;
        g.occupant = planeId;
        return true;
    }

    @Override
    public boolean clearForTakeoff(int planeId) {
        Plane p = planeById(planeId);
        if (p == null || p.state != AT_GATE || p.serviceLeft > 0) {
            return false;
        }
        Runway rw = freeRunway();
        if (rw == null) {
            return false;
        }
        if (p.gateIdx != 0) {
            Gate g = gateAt(p.gateIdx);
            if (g != null) {
                g.occupant = 0;
            }
        }
        p.gateIdx = 0;
        p.state = DEPARTING;
        p.runwayIdx = rw.index;
        rw.occupant = planeId;
        rw.cooldown = 3;
        return true;
    }

    @Override
    public boolean buyGate() {
        int cost = 600 + openGates * 150;
        if (cash < cost || openGates >= MAX_GATES) {
            return false;
        }
        cash -= cost;
        Gate g = gateAt(openGates + 1);
        if (g != null) {
            g.open = true;
        }
        openGates++;
        return true;
    }

    @Override
    public boolean buyRunway() {
        int cost = 1500 + openRunways * 800;
        if (cash < cost || openRunways >= MAX_RUNWAYS) {
            return false;
        }
        cash -= cost;
        Runway r = runwayAt(openRunways + 1);
        if (r != null) {
            r.open = true;
        }
        openRunways++;
        return true;
    }

    /**
     * Edition 2 priority score for auto-assist: lower is more urgent. Urgency
     * is dominated by remaining patience, but a plane's value (bigger planes
     * pay more and are costlier to lose) shaves the score so the tower protects
     * heavies when two planes are equally close to timing out. The size term is
     * bounded (&lt; one patience point) so it only breaks near-ties and never
     * lets a comfortable heavy jump ahead of a tiny plane about to leave.
     */
    private int priorityScore(Plane p) {
        return scoreFor(p.patience, p.sizeClass);
    }

    /** The Edition 2 priority formula, exposed for tests. Lower = more urgent. */
    int scoreFor(int patience, int sizeClass) {
        return patience * 4 - sizeClass;
    }

    @Override
    public void autoAssist() {
        Plane best = null;
        int bestScore = Integer.MAX_VALUE;
        for (Plane p : planes) {
            boolean actionable = p.state == INBOUND
                    || (p.state == TAXI_IN && p.gateIdx == 0)
                    || (p.state == AT_GATE && p.serviceLeft == 0);
            if (actionable) {
                int score = priorityScore(p);
                if (score < bestScore) {
                    bestScore = score;
                    best = p;
                }
            }
        }
        if (best == null) {
            return;
        }
        if (best.state == INBOUND) {
            // Edition 3+: if every open runway is iced, de-ice one first so the
            // most urgent inbound has somewhere safe to land. Keeps AUTO (and
            // the headless self-play) coherent once weather is in the mix.
            if (weather != null && freeRunway() != null
                    && !weather.safeToLand(freeRunway().index)) {
                for (int i = 1; i <= MAX_RUNWAYS; i++) {
                    if (weather.deIce(i)) {
                        break;
                    }
                }
            }
            clearToLand(best.id);
        } else if (best.state == TAXI_IN) {
            assignGate(best.id, 0);
        } else {
            clearForTakeoff(best.id);
        }
    }

    @Override
    public void step() {
        if (gameOver) {
            return;
        }
        tick++;

        for (Runway r : runways) {
            if (r.cooldown > 0) {
                r.cooldown--;
            }
        }

        // Edition 3+: drift the weather front and (Edition 7+) let a delegated
        // de-ice policy clear iced runways on the player's behalf.
        if (weather != null) {
            weather.tick();
            if (automation != null && automation.autoDeIce()) {
                for (int i = 1; i <= MAX_RUNWAYS; i++) {
                    weather.deIce(i);
                }
            }
        }
        // Edition 7+: a delegated maintenance policy services a flagged fleet.
        if (automation != null && automation.autoMaintain()
                && fleet != null && fleet.needsReview()) {
            scheduleMaintenance();
        }
        // Edition 7+: a delegated booking policy keeps the book topped up.
        if (automation != null && automation.autoBook()
                && network != null && network.reservations() < GimmickSystems.Network.TARGET_BOOK) {
            network.book(1);
        }

        for (Plane p : planes) {
            advancePlane(p);
        }

        planes.removeIf(p -> p.state == DONE || p.state == ANGRY);

        spawnTimer--;
        if (spawnTimer <= 0) {
            spawnPlane();
            if (spawnEvery > 6) {
                spawnEvery--;
            }
            spawnTimer = spawnEvery;
        }

        if (reputation <= 0) {
            reputation = 0;
            gameOver = true;
        }
    }

    private void advancePlane(Plane p) {
        int s = p.state;
        if (s == DONE || s == ANGRY) {
            return;
        }

        boolean waiting = s == INBOUND
                || (s == TAXI_IN && p.gateIdx == 0)
                || (s == AT_GATE && p.serviceLeft == 0);
        if (waiting) {
            p.patience--;
            if (p.patience <= 0) {
                abandon(p);
                return;
            }
        }

        if (s == INBOUND) {
            if (p.posX < 0.35) {
                p.posX += 0.01;
            }
            return;
        }
        if (s == LANDING) {
            p.posX = 0.45;
            p.posY = 0.78;
            if (p.runwayIdx != 0) {
                Runway rw = runwayAt(p.runwayIdx);
                if (rw != null) {
                    rw.occupant = 0;
                }
                p.runwayIdx = 0;
            }
            if (!assignGate(p.id, 0)) {
                p.state = TAXI_IN;
                p.gateIdx = 0;
            }
            return;
        }
        if (s == TAXI_IN) {
            if (p.gateIdx == 0) {
                Gate g = freeGate();
                if (g != null) {
                    assignGate(p.id, g.index);
                }
                return;
            }
            p.posX = 0.6 + (p.gateIdx % 4) * 0.09;
            p.posY = 0.3 + (p.gateIdx / 4) * 0.18;
            p.state = AT_GATE;
            return;
        }
        if (s == AT_GATE) {
            if (p.serviceLeft > 0) {
                p.serviceLeft--;
            }
            return;
        }
        if (s == DEPARTING) {
            p.posX = 0.9;
            p.posY = 0.82;
            if (p.runwayIdx != 0) {
                Runway rw = runwayAt(p.runwayIdx);
                if (rw != null) {
                    rw.occupant = 0;
                }
                p.runwayIdx = 0;
            }
            cash += fareFor(p.sizeClass);
            served++;
            if (reputation < 100) {
                reputation++;
            }
            // Edition 2: a clean, on-time departure extends the combo. Every
            // COMBO_STEP in a row pays a flat tip on top of the fare.
            streak++;
            if (streak % COMBO_STEP == 0) {
                cash += COMBO_TIP;
                comboTips += COMBO_TIP;
            }
            // --- Cumulative systems react to a clean departure --------------
            if (fleet != null) {
                fleet.onDeparture();          // Ed. 4: wear the fleet a little
            }
            if (network != null) {
                network.onDeparture();        // Ed. 5: draw down the book...
                cash += network.departureBonus(); // ...and pay the steady bonus
            }
            if (terminals != null) {
                terminals.routeDeparture();   // Ed. 6: balance across terminals
            }
            p.state = DONE;
        }
    }

    private void abandon(Plane p) {
        if (p.gateIdx != 0) {
            Gate g = gateAt(p.gateIdx);
            if (g != null) {
                g.occupant = 0;
            }
            p.gateIdx = 0;
        }
        if (p.runwayIdx != 0) {
            Runway rw = runwayAt(p.runwayIdx);
            if (rw != null) {
                rw.occupant = 0;
            }
            p.runwayIdx = 0;
        }
        p.state = ANGRY;
        lost++;
        reputation -= 12;
        // Edition 2: an angry departure breaks the on-time combo.
        streak = 0;
    }

    @Override
    public GameSnapshot snapshot() {
        List<GameSnapshot.PlaneView> views = new ArrayList<>();
        for (Plane p : planes) {
            if (p.state == DONE || p.state == ANGRY) {
                continue;
            }
            views.add(new GameSnapshot.PlaneView(p.id, p.flight, p.state,
                    p.sizeClass, p.patience, p.maxPatience, p.serviceLeft,
                    p.posX, p.posY));
        }
        return new GameSnapshot(tick, cash, reputation, served, lost,
                openGates, openRunways, planes.size(), gameOver, views,
                streak, servicePremiumPct());
    }

    /**
     * A human-readable status line for every cumulative system live at this
     * edition. The UIs render this down the side so the player sees the whole
     * mastery ladder they are currently holding. Pure data; no UI dependency.
     */
    String systemsStatus() {
        StringBuilder sb = new StringBuilder("ED|").append(gimmicks.edition())
                .append("|").append(gimmicks.codename())
                .append("|depth=").append(gimmicks.depth());
        sb.append("\nTOWER|cash=").append(cash).append("|rep=").append(reputation)
                .append("|served=").append(served).append("|lost=").append(lost);
        if (gimmicks.has(EditionGimmicks.System.SERVICE_PREMIUM)) {
            sb.append("\nPREMIUM|pct=").append(servicePremiumPct())
                    .append("|streak=").append(streak).append("|tips=").append(comboTips);
        }
        if (weather != null) {
            sb.append("\nWEATHER|phase=").append(weather.phase())
                    .append("|rec=").append(weather.recommend());
        }
        if (fleet != null) {
            sb.append("\nFLEET|rating=").append(fleet.useRating())
                    .append("|review=").append(fleet.needsReview())
                    .append("|rec=").append(fleet.recommend());
        }
        if (network != null) {
            sb.append("\nNETWORK|book=").append(network.reservations())
                    .append("|fulfilled=").append(network.fulfilled())
                    .append("|rec=").append(network.recommend());
        }
        if (terminals != null) {
            sb.append("\nTERMINALS|open=").append(terminals.open())
                    .append("/").append(terminals.capacity())
                    .append("|imbalance=").append(terminals.imbalance())
                    .append("|rec=").append(terminals.recommend());
        }
        if (automation != null) {
            sb.append("\n").append(automation.policyLine())
                    .append("|delegated=").append(automation.delegatedCount());
        }
        if (gimmicks.has(EditionGimmicks.System.LIVE_ECONOMY)) {
            sb.append("\n").append(GimmickSystems.LiveEconomyScore.line(
                    "hub-1", "MAJOR-EAST", running(), 1, liveScore()));
        }
        return sb.toString();
    }

    private boolean running() {
        return !gameOver;
    }

    @Override
    public String backendName() {
        return "Local Java mirror (SLeeLa rules)";
    }

    // Expose a couple of raw values for tests.
    int cash() {
        return cash;
    }

    int reputation() {
        return reputation;
    }

    int served() {
        return served;
    }

    int streak() {
        return streak;
    }

    int comboTips() {
        return comboTips;
    }
}
