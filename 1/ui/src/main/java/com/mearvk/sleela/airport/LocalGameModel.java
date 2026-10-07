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

    // Mirrors AirportTycoon.sleela constants.
    static final int MAX_GATES = 8;
    static final int MAX_RUNWAYS = 3;

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

    public LocalGameModel() {
        reset();
    }

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
        for (Runway r : runways) {
            if (r.open && r.occupant == 0 && r.cooldown == 0) {
                return r;
            }
        }
        return null;
    }

    private Plane planeById(int id) {
        for (Plane p : planes) {
            if (p.id == id) {
                return p;
            }
        }
        return null;
    }

    private int fareFor(int sizeClass) {
        if (sizeClass == 0) {
            return 120;
        }
        if (sizeClass == 1) {
            return 260;
        }
        return 540;
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
        p.state = LANDING;
        p.runwayIdx = rw.index;
        rw.occupant = planeId;
        rw.cooldown = 3;
        return true;
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

    @Override
    public void autoAssist() {
        Plane best = null;
        int bestPatience = Integer.MAX_VALUE;
        for (Plane p : planes) {
            boolean actionable = p.state == INBOUND
                    || (p.state == TAXI_IN && p.gateIdx == 0)
                    || (p.state == AT_GATE && p.serviceLeft == 0);
            if (actionable && p.patience < bestPatience) {
                bestPatience = p.patience;
                best = p;
            }
        }
        if (best == null) {
            return;
        }
        if (best.state == INBOUND) {
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
                openGates, openRunways, planes.size(), gameOver, views);
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
}
