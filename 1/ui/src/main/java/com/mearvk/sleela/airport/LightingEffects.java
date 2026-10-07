package com.mearvk.sleela.airport;

/**
 * A faithful Java mirror of the SLeeLa <b>Lighting Effects</b> module
 * ({@code media/LightingEffects.sleela}) and its <b>Loader</b>
 * ({@code media/LightingLoader.sleela}).
 *
 * <p>Edition 1.0 may now <b>use light effects</b> on your text/words. Lighting
 * is a loadable <b>module</b> (sharing the SLeeLa common module contract
 * {@code load/loaded/moduleName/describe}), brought online by a <b>Loader</b>
 * that discovers it, verifies the contract, and only then enables lighting —
 * the same discovery/verify pattern the SLeeLa compiler, Loader, and Nordshrift
 * use for a package facade.
 *
 * <p>On a real terminal the effects are true ANSI light (bold, colour, blink,
 * reverse-video spotlight); a GUI can instead read the structured
 * {@code [[fx:name:#RRGGBB|word]]} markup. The headline feature: you can
 * <b>see your Mayor's clues/excellents</b> — {@link #lightClues(String)} scans
 * a line and lights the important words (a {@code *}-starred word glows; an
 * {@code !}-ended word is spotlit).
 *
 * <p>This mirror exists so the lighting module is testable and demonstrable
 * without the SLeeLa toolchain, exactly like {@link LocalGameModel} mirrors the
 * tower game and {@link MediaCenter} mirrors the events center.
 */
public final class LightingEffects {

    public static final String MODULE = "LightingEffects";

    // --- The light effects this module can cast on a word ---
    public static final int FX_NONE = 0;
    public static final int FX_GLOW = 1;      // bold + bright colour (steady glow)
    public static final int FX_HIGHLIGHT = 2; // reverse-video marker (highlighter)
    public static final int FX_PULSE = 3;     // blink (a pulsing light)
    public static final int FX_SPOTLIGHT = 4; // bold + reverse (a bright spotlight)
    public static final int FX_DIM = 5;       // faint (lights lowered)

    private static final char ESC = '\u001b';

    // Named glow colours, packed 0xRRGGBBAA like SLColor.
    private int clrAmber;
    private int clrExcellent;
    private int clrCool;
    private int clrAlarm;

    // Common module contract state.
    private int loadedFlag;
    private String moduleTitle;
    private int lastCount; // words lit by the last clue scan

    /** Module contract: initialise palette and mark loaded. */
    public void load() {
        loadedFlag = 1;
        moduleTitle = MODULE;
        lastCount = 0;
        clrAmber = pack(255, 191, 0, 255);     // amber — a Mayor's clue
        clrExcellent = pack(255, 215, 0, 255); // gold  — an excellent!
        clrCool = pack(80, 170, 255, 255);     // blue  — a cool glow
        clrAlarm = pack(230, 60, 40, 255);     // red   — an alarm
    }

    public boolean loaded() {
        return loadedFlag == 1;
    }

    public String moduleName() {
        return MODULE;
    }

    public String describe() {
        return MODULE + " (loaded=" + loadedFlag + ", lastLit=" + lastCount + ")";
    }

    public int lastCount() {
        return lastCount;
    }

    static int pack(int r, int g, int b, int a) {
        return ((r & 255) << 24) | ((g & 255) << 16) | ((b & 255) << 8) | (a & 255);
    }

    /** The packed glow colour an effect uses (so a GUI can read it). */
    public int colorFor(int fx) {
        return switch (fx) {
            case FX_GLOW -> clrAmber;
            case FX_HIGHLIGHT -> clrExcellent;
            case FX_PULSE -> clrAlarm;
            case FX_SPOTLIGHT -> clrCool;
            default -> clrAmber;
        };
    }

    private String ansiOpen(int fx) {
        return switch (fx) {
            case FX_GLOW -> ESC + "[1;33m";       // bold yellow
            case FX_HIGHLIGHT -> ESC + "[1;30;103m"; // black on bright yellow
            case FX_PULSE -> ESC + "[5;31m";      // blinking red
            case FX_SPOTLIGHT -> ESC + "[1;7;36m"; // bold reverse cyan
            case FX_DIM -> ESC + "[2m";           // faint
            default -> "";
        };
    }

    private String ansiReset(int fx) {
        return fx == FX_NONE ? "" : ESC + "[0m";
    }

    /** Light a single word with an effect (true ANSI for a terminal). */
    public String light(String word, int fx) {
        if (word == null) {
            return "";
        }
        if (fx == FX_NONE) {
            return word;
        }
        return ansiOpen(fx) + word + ansiReset(fx);
    }

    /** Structured markup for a GUI: {@code [[fx:name:#RRGGBB|word]]}. */
    public String markup(String word, int fx) {
        if (word == null) {
            return "";
        }
        if (fx == FX_NONE) {
            return word;
        }
        return "[[fx:" + fxName(fx) + ":" + hex6(colorFor(fx)) + "|" + word + "]]";
    }

    public String fxName(int fx) {
        return switch (fx) {
            case FX_GLOW -> "glow";
            case FX_HIGHLIGHT -> "highlight";
            case FX_PULSE -> "pulse";
            case FX_SPOTLIGHT -> "spotlight";
            case FX_DIM -> "dim";
            default -> "none";
        };
    }

    static String hex6(int packed) {
        int r = (packed >>> 24) & 255;
        int g = (packed >>> 16) & 255;
        int b = (packed >>> 8) & 255;
        return String.format("#%02x%02x%02x", r, g, b);
    }

    /**
     * See your Mayor's clues/excellents: scan a line and light the clue words.
     * A word is a CLUE when it starts with {@code *} (the Mayor's star) or ends
     * with {@code !} (an "excellent"). Clues glow; excellents are spotlit.
     * Records how many words it lit in {@link #lastCount()}.
     */
    public String lightClues(String line) {
        lastCount = 0;
        if (line == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        StringBuilder word = new StringBuilder();
        int n = line.length();
        for (int i = 0; i <= n; i++) {
            boolean boundary = (i == n) || line.charAt(i) == ' ';
            if (boundary) {
                if (word.length() > 0) {
                    out.append(lightWordByClue(word.toString()));
                }
                if (i < n) {
                    out.append(' ');
                }
                word.setLength(0);
            } else {
                word.append(line.charAt(i));
            }
        }
        return out.toString();
    }

    private String lightWordByClue(String word) {
        int fx = clueEffect(word);
        if (fx != FX_NONE) {
            lastCount++;
        }
        return light(word, fx);
    }

    /** '*' prefix => glow (a clue); '!' suffix => spotlight (an excellent!). */
    public int clueEffect(String word) {
        if (word == null || word.isEmpty()) {
            return FX_NONE;
        }
        if (word.charAt(0) == '*') {
            return FX_GLOW;
        }
        if (word.charAt(word.length() - 1) == '!') {
            return FX_SPOTLIGHT;
        }
        return FX_NONE;
    }

    // ======================================================================
    // The Loader — mirror of media/LightingLoader.sleela.
    // ======================================================================

    /**
     * Loads the {@link LightingEffects} module, verifies its contract, and only
     * then enables lighting. When not enabled it passes text through unlit, so a
     * missing/broken module can never crash the host.
     */
    public static final class Loader {

        public static final String WANT_MODULE = "LightingEffects";

        private LightingEffects fx;
        private boolean enabledFlag;
        private String status = "not loaded";

        /** Find and load the module, then verify the common module contract. */
        public void loadModule() {
            enabledFlag = false;
            status = "not loaded";

            LightingEffects m = new LightingEffects();
            m.load();

            if (!m.loaded()) {
                status = "module failed to load";
                return;
            }
            if (!WANT_MODULE.equals(m.moduleName())) {
                status = "unexpected module: " + m.moduleName();
                return;
            }
            fx = m;
            enabledFlag = true;
            status = "loaded + verified: " + m.describe();
        }

        public boolean enabled() {
            return enabledFlag;
        }

        public String loadStatus() {
            return status;
        }

        /** Light one word; unlit passthrough when not enabled. */
        public String light(String word, int effect) {
            if (!enabledFlag) {
                return word == null ? "" : word;
            }
            return fx.light(word, effect);
        }

        /** Find the clues in a line and light them (the Mayor's clues). */
        public String findAndLightClues(String line) {
            if (!enabledFlag) {
                return line == null ? "" : line;
            }
            return fx.lightClues(line);
        }

        public int lastCluesLit() {
            return enabledFlag ? fx.lastCount() : 0;
        }

        public String markup(String word, int effect) {
            if (!enabledFlag) {
                return word == null ? "" : word;
            }
            return fx.markup(word, effect);
        }

        public int glow() {
            return FX_GLOW;
        }

        public int highlight() {
            return FX_HIGHLIGHT;
        }

        public int pulse() {
            return FX_PULSE;
        }

        public int spotlight() {
            return FX_SPOTLIGHT;
        }

        public int dim() {
            return FX_DIM;
        }
    }

    // ----------------------------------------------------------------------
    // Headless demo: load the module through the Loader and light the Mayor's
    // clue line — on a real terminal the clue words actually light up.
    // ----------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("Airport Tycoon — Lighting Effects module + Loader");
        Loader loader = new Loader();
        loader.loadModule();
        System.out.println("status : " + loader.loadStatus());
        System.out.println("enabled: " + loader.enabled());

        String line = "Mayor says: *land the heavies first, keep rep high. Excellent!";
        System.out.println("plain  : " + line);
        System.out.println("lit    : " + loader.findAndLightClues(line));
        System.out.println("lit " + loader.lastCluesLit() + " clue word(s).");
        System.out.println("markup : " + loader.markup("Excellent!", FX_SPOTLIGHT));
    }
}
