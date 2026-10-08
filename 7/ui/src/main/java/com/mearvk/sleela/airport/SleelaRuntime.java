package com.mearvk.sleela.airport;

/**
 * The contract the Sleela UI uses to talk to the game's business logic.
 *
 * <p>This is the "SLeeLa intent &rarr; Java GUI" boundary from
 * {@code gui/INTEGRATION.md} (Path 2), expressed as named operations. The
 * canonical implementation drives the compiled {@code game/AirportTycoon.sleela}
 * Wrapper; a local Java mirror ({@link LocalGameModel}) implements the same
 * contract so the UI and animation run even when the SLeeLa toolchain is not
 * installed.
 *
 * <p>All methods are called on the JavaFX animation thread. Implementations
 * must be fast and non-blocking.
 */
public interface SleelaRuntime {

    /** Build / reset the world to its initial state. */
    void reset();

    /** Advance the simulation by one tick. */
    void step();

    /** Produce the current frame of state for rendering. */
    GameSnapshot snapshot();

    // --- Player intents (named operations) ---

    boolean clearToLand(int planeId);

    boolean assignGate(int planeId, int gateIndex);

    boolean clearForTakeoff(int planeId);

    boolean buyGate();

    boolean buyRunway();

    /** Make a sensible move for the most urgent plane. */
    void autoAssist();

    /** A short human-readable label for which backend is active. */
    String backendName();
}
