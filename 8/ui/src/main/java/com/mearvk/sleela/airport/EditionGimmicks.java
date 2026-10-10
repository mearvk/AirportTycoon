package com.mearvk.sleela.airport;

import java.util.ArrayList;
import java.util.List;

/**
 * EditionGimmicks &mdash; the ordered 1&rarr;8 mastery ladder.
 *
 * <p>Airport Tycoon is one game played at eight depths. Edition&nbsp;1 is the
 * bare tower loop; each later edition keeps <em>every</em> earlier system and
 * lights exactly <em>one</em> new controllable system on the same panel, so
 * Edition&nbsp;N contains systems&nbsp;1..N. The systems are ordered as a
 * progression of mastery: first you <em>operate</em>, then you learn to
 * <em>reward quality</em>, <em>read the environment</em>, <em>sustain the
 * machine</em>, <em>plan ahead</em>, <em>scale out</em>, <em>delegate</em>, and
 * finally <em>compete</em> with everything running at once.
 *
 * <p>This class is the single source of truth for <b>which</b> systems an
 * edition exposes and <b>what</b> each one does, as a small deterministic
 * model. It is pure Java (no JavaFX / SleelaUI) so it compiles in the minimal
 * {@code javac} self-check and is unit-tested. Both UI fronts ask it what to
 * show; the SLeeLa Wrappers mirror the same ladder in narrative form.
 *
 * <p>Each gimmick is deliberately a <em>decision-support</em> system: it reports
 * a condition and a recommended action and lets the player act, rather than
 * acting irreversibly on the player's behalf. Mastering an edition means
 * holding all of its systems in a good state at once &mdash; which is why
 * interest multiplies as you climb.
 */
public final class EditionGimmicks {

    /** The eight ordered systems. Ordinal + 1 == the edition that introduces it. */
    public enum System {
        /** Ed. 1 &mdash; land, gate, service, depart; patience, reputation, cash. */
        TOWER("Tower Operations", "Operate",
                "Land inbound planes, gate and service them, depart them before "
                        + "patience runs out. Reputation gates survival; cash gates growth."),
        /** Ed. 2 &mdash; reputation->fares and the on-time streak tip. */
        SERVICE_PREMIUM("Service Premium & On-Time Combo", "Reward quality",
                "Reputation scales every fare; five clean departures in a row pay "
                        + "a combo tip. Quality now has a legible price."),
        /** Ed. 3 &mdash; weather cells that gate landings until you clear them. */
        WEATHER("Weather & Runway Conditions", "Read the environment",
                "Fronts roll across the field and ice the runways. Watch the band, "
                        + "de-ice the right runway, and land into the gap."),
        /** Ed. 4 &mdash; per-aircraft health / fleet use-rating and engineer reviews. */
        FLEET("Fleet Health & Maintenance", "Sustain the machine",
                "Each aircraft carries a use-rating that decays with cycles. Below "
                        + "85 the Maintenance Engineer flags it; grounded fleet earns nothing."),
        /** Ed. 5 &mdash; routes/reservations you schedule ahead of arrivals. */
        NETWORK("Route Network & Reservations", "Plan ahead",
                "Book routes and reservations in advance so arrivals are a plan, not "
                        + "a surprise. A full book is steady money; an empty one starves the gates."),
        /** Ed. 6 &mdash; multiple terminals/zones you unlock and balance. */
        TERMINALS("Multi-Terminal Expansion", "Scale out",
                "Unlock continental terminals and balance load across them. Each "
                        + "terminal is its own small airport; the hub is the sum you keep level."),
        /** Ed. 7 &mdash; programmable auto-policies (configure the autopilot). */
        AUTOMATION("Automation & Policy", "Delegate control",
                "Write the rules the tower follows on its own: priority, de-ice, "
                        + "maintenance and booking policies. You stop pressing AUTO and start programming it."),
        /** Ed. 8 &mdash; the live competitive score/server tying it all together. */
        LIVE_ECONOMY("Live Competitive Economy", "Compete",
                "Publish one authoritative live score to the server and compete, with "
                        + "every earlier system running at once. Mastery is holding all eight green.");

        public final String title;
        public final String masteryVerb;
        public final String blurb;

        System(String title, String masteryVerb, String blurb) {
            this.title = title;
            this.masteryVerb = masteryVerb;
            this.blurb = blurb;
        }

        /** The edition that first introduces this system (1-based). */
        public int introducedIn() {
            return ordinal() + 1;
        }
    }

    public static final int MIN_EDITION = 1;
    public static final int MAX_EDITION = 8;

    private final int edition;

    public EditionGimmicks(int edition) {
        if (edition < MIN_EDITION || edition > MAX_EDITION) {
            throw new IllegalArgumentException(
                    "edition must be " + MIN_EDITION + ".." + MAX_EDITION + ", got " + edition);
        }
        this.edition = edition;
    }

    public int edition() {
        return edition;
    }

    /** True when this edition exposes the given system (edition >= intro). */
    public boolean has(System s) {
        return edition >= s.introducedIn();
    }

    /** The systems this edition exposes, in mastery order (1..edition). */
    public List<System> systems() {
        List<System> out = new ArrayList<>();
        for (System s : System.values()) {
            if (has(s)) {
                out.add(s);
            }
        }
        return out;
    }

    /** The one system this edition introduces on top of the previous one. */
    public System newThisEdition() {
        return System.values()[edition - 1];
    }

    /** How many controllable systems the player juggles at this depth. */
    public int depth() {
        return edition;
    }

    /**
     * The edition's canonical name/epithet, mirrored from the repo README so the
     * UIs and the narrative agree on the mastery theme per level.
     */
    public String codename() {
        return CODENAMES[edition - 1];
    }

    public String epithet() {
        return EPITHETS[edition - 1];
    }

    private static final String[] CODENAMES = {
            "Keystone Meridian", "Golden Assimilation", "Platinum Ascension",
            "Sovereign Skies", "Imperial Horizons", "Grand Meridian",
            "Celestial Concourse", "Apex Dominion"
    };

    private static final String[] EPITHETS = {
            "The Foundational Shift", "The Assimilation Era", "The Prosperity Era",
            "The Dominion Era", "The Grand Strategy Era", "The Continental Era",
            "The Zenith Era", "The Ultimate Era"
    };

    /**
     * A compact, UI-ready status line of every system at this edition, each
     * marked as the base, a carried-forward system, or the one new this level.
     * This is what the panel renders down the side so the player sees the whole
     * ladder they have mastered so far.
     */
    public String ladderLine() {
        StringBuilder sb = new StringBuilder("LADDER|edition=").append(edition)
                .append("|name=").append(codename())
                .append("|depth=").append(depth());
        for (System s : systems()) {
            String role = (s.introducedIn() == edition)
                    ? "new" : (s == System.TOWER ? "base" : "carried");
            sb.append("\nSYS|intro=").append(s.introducedIn())
                    .append("|role=").append(role)
                    .append("|title=").append(s.title)
                    .append("|mastery=").append(s.masteryVerb);
        }
        return sb.toString();
    }
}
