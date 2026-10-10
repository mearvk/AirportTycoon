package com.mearvk.sleela.airport;

/**
 * EditionConfig &mdash; the one place that records which edition this folder is.
 *
 * <p>The gameplay code ({@link LocalGameModel}, {@link AirportTycoonApp}) is now
 * edition-agnostic: the ordered 1&rarr;8 mastery ladder ({@link EditionGimmicks})
 * is driven entirely by a single edition number, so the <em>same</em> Java is
 * copied into every numbered edition folder and only this constant changes. That
 * is what makes the progression real rather than cosmetic &mdash; Edition&nbsp;N
 * lights exactly systems&nbsp;1..N from the identical code.
 *
 * <p>This value is the default; it can still be overridden at launch with
 * {@code -Dat.edition=N} for quick comparison between editions.
 */
public final class EditionConfig {

    private EditionConfig() {
    }

    /** This folder's edition (1..8). */
    public static final int EDITION = 8;
}
