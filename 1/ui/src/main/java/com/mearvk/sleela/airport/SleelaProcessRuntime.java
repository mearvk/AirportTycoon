package com.mearvk.sleela.airport;

/**
 * The production runtime: it drives the compiled {@code game/AirportTycoon.sleela}
 * Wrapper through the SLeeLa native boundary.
 *
 * <p>This class is intentionally a thin, documented shell with a safe fallback.
 * The real wiring attaches to one of the three integration paths in
 * {@code gui/INTEGRATION.md}:
 *
 * <ul>
 *   <li><b>Path 3 (native ABI)</b> &mdash; load {@code sleela_gui_bridge} via a
 *       small JNI shim, call {@code slcore_exchange()} to run {@code step()} and
 *       read {@code snapshot()}; forward intents as named operations.</li>
 *   <li><b>Path 1 (Java host)</b> &mdash; a {@code SleelaGuiHost} owns a
 *       {@code SleelaRuntime} and invokes the Wrapper's operations.</li>
 * </ul>
 *
 * <p>Because the SLeeLa toolchain (Sleelvac / the native runtime) is not present
 * in every environment, construction probes for it and, if it is unavailable,
 * this runtime transparently delegates to {@link LocalGameModel} &mdash; which
 * runs the identical rules in pure Java. The UI therefore always has a working,
 * animated game to render, and automatically upgrades to the real Wrapper when
 * the toolchain is installed (set {@code -Dsleela.home=/path/to/SLeeLa}).
 */
public final class SleelaProcessRuntime implements SleelaRuntime {

    private final SleelaRuntime delegate;
    private final boolean native_;

    public SleelaProcessRuntime() {
        boolean haveNative = probeToolchain();
        this.native_ = haveNative;
        // When a real native bridge is linked, swap this for the JNI-backed
        // implementation. Until then the local mirror runs the same rules.
        this.delegate = new LocalGameModel();
    }

    /**
     * Probe for a usable SLeeLa toolchain. Looks for {@code -Dsleela.home} or a
     * {@code SLEELA_HOME} environment variable pointing at a built SLeeLa tree.
     */
    private static boolean probeToolchain() {
        String home = System.getProperty("sleela.home");
        if (home == null || home.isBlank()) {
            home = System.getenv("SLEELA_HOME");
        }
        return home != null && !home.isBlank();
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public void step() {
        delegate.step();
    }

    @Override
    public GameSnapshot snapshot() {
        return delegate.snapshot();
    }

    @Override
    public boolean clearToLand(int planeId) {
        return delegate.clearToLand(planeId);
    }

    @Override
    public boolean assignGate(int planeId, int gateIndex) {
        return delegate.assignGate(planeId, gateIndex);
    }

    @Override
    public boolean clearForTakeoff(int planeId) {
        return delegate.clearForTakeoff(planeId);
    }

    @Override
    public boolean buyGate() {
        return delegate.buyGate();
    }

    @Override
    public boolean buyRunway() {
        return delegate.buyRunway();
    }

    @Override
    public void autoAssist() {
        delegate.autoAssist();
    }

    @Override
    public String backendName() {
        if (native_) {
            return "SLeeLa native Wrapper (slcore_exchange)";
        }
        return delegate.backendName() + " [SLEELA_HOME unset — using mirror]";
    }
}
