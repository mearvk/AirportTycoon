package com.mearvk.sleela.airport;

/**
 * Phosphor &mdash; the unified Airport Tycoon visual language.
 *
 * <p>Every edition (1&ndash;8) renders through one monochrome, 8-bit
 * <em>green-phosphor</em> palette so the whole series looks like a single
 * machine: a dark CRT ground with a small, ordered ladder of green intensities
 * and one amber "alert" tint reserved for danger. There is deliberately no
 * second hue &mdash; the look is orthogonal across editions, and the only thing
 * that changes from 1 to 8 is <em>how many systems are lit on the same panel</em>.
 *
 * <p>This class is pure Java (no JavaFX, no SleelaUI) so it compiles in the
 * minimal {@code javac} self-check and can be unit-tested. The JavaFX renderer
 * ({@link AirportTycoonApp}) and the SleelaUI theme both read their colours from
 * here as {@code 0xRRGGBB} words / {@code #rrggbb} web strings, and their object
 * sizes from the {@link Metrics} ladder, so a change here restyles both fronts
 * at once.
 *
 * <h2>Design rules (kept identical for every edition)</h2>
 * <ul>
 *   <li><b>One hue.</b> Interface elements are green; only imminent danger
 *       (patience/▸reputation critical) borrows the single amber alert tint.</li>
 *   <li><b>Ordered intensity.</b> Six fixed green steps, dim&rarr;bright, map to
 *       meaning: ground, inactive, frame, label, active, peak.</li>
 *   <li><b>Orthogonal geometry.</b> All world objects snap to the {@link Metrics}
 *       grid: the same unit, the same corner radius, the same orientation
 *       (aircraft nose always points along +X toward departure).</li>
 * </ul>
 */
public final class Phosphor {

    private Phosphor() {
    }

    // --- The green-phosphor ladder (0xRRGGBB), dim -> bright -----------------
    /** The deep CRT ground the whole airfield sits on (near-black green). */
    public static final int GROUND = 0x041008;
    /** A faint fill for inactive / locked objects (barely glowing). */
    public static final int INACTIVE = 0x0b3b1f;
    /** Frame / separator / grid lines. */
    public static final int FRAME = 0x167a3c;
    /** Default label and outline green. */
    public static final int LABEL = 0x2fae5e;
    /** Active object fill (a plane under control, an open gate). */
    public static final int ACTIVE = 0x45d67f;
    /** Peak highlight: the brightest green, for the one thing to look at now. */
    public static final int PEAK = 0x8dffb0;

    /** The single non-green tint, reserved for imminent danger only. */
    public static final int ALERT = 0xffb000;      // phosphor amber

    // --- Web-string forms for the SleelaUI theme / CSS -----------------------
    public static String web(int rgb) {
        return String.format("#%06x", rgb & 0xFFFFFF);
    }

    public static final String GROUND_WEB = web(GROUND);
    public static final String INACTIVE_WEB = web(INACTIVE);
    public static final String FRAME_WEB = web(FRAME);
    public static final String LABEL_WEB = web(LABEL);
    public static final String ACTIVE_WEB = web(ACTIVE);
    public static final String PEAK_WEB = web(PEAK);
    public static final String ALERT_WEB = web(ALERT);

    /** Channel helpers so a renderer can build its own colour objects. */
    public static int red(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    public static int green(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    public static int blue(int rgb) {
        return rgb & 0xFF;
    }

    /**
     * Linear blend from {@code a} toward {@code b} by {@code t} in [0,1],
     * per channel. Used to fade a healthy green object toward the amber alert
     * tint as its patience/health runs out &mdash; the only colour transition
     * in the whole palette.
     */
    public static int mix(int a, int b, double t) {
        double u = t < 0 ? 0 : (t > 1 ? 1 : t);
        int r = (int) Math.round(red(a) + (red(b) - red(a)) * u);
        int g = (int) Math.round(green(a) + (green(b) - green(a)) * u);
        int bl = (int) Math.round(blue(a) + (blue(b) - blue(a)) * u);
        return (r << 16) | (g << 8) | bl;
    }

    /**
     * The alert-aware fill for a timed object: {@link #ACTIVE} when healthy,
     * fading to {@link #ALERT} as {@code fraction} (1 = full, 0 = empty) drops
     * below {@link Metrics#ALERT_THRESHOLD}. Identical rule in every edition.
     */
    public static int healthTint(double fraction) {
        if (fraction >= Metrics.ALERT_THRESHOLD) {
            return ACTIVE;
        }
        double t = (Metrics.ALERT_THRESHOLD - fraction) / Metrics.ALERT_THRESHOLD;
        return mix(ACTIVE, ALERT, t);
    }

    /**
     * Metrics &mdash; the orthogonal geometry every edition shares.
     *
     * <p>All world objects are laid out on one unit grid, with one corner
     * radius and one orientation convention, so an aircraft in Edition 1 and a
     * weather cell added in Edition 3 are the same size, aligned to the same
     * lanes, and face the same way. Later editions only add <em>more</em>
     * objects on this grid; they never change the grid.
     */
    public static final class Metrics {
        private Metrics() {
        }

        /** The base grid unit (px). Everything is an integer multiple of this. */
        public static final int UNIT = 16;
        /** The one corner radius used by every rounded object. */
        public static final int RADIUS = 4;
        /** Standard glyph tile edge for a SleelaUI object (3 units). */
        public static final int TILE = UNIT * 3;
        /** Aircraft body half-length in units; size class scales up by one unit. */
        public static final int PLANE_BASE_UNITS = 1;
        /** Patience/health below this fraction fades an object toward amber. */
        public static final double ALERT_THRESHOLD = 0.4;

        /** Orientation convention: every aircraft nose points along +X. */
        public static final double NOSE_DIRECTION_X = 1.0;
        public static final double NOSE_DIRECTION_Y = 0.0;

        /** A size-class aircraft's half-length in pixels (orthogonal sizing). */
        public static int planeHalfLength(int sizeClass) {
            return (PLANE_BASE_UNITS + sizeClass) * UNIT / 2;
        }
    }
}
