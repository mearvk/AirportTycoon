package com.mearvk.sleela.airport;

/**
 * EditionConfig &mdash; the one place that records which edition this folder is.
 *
 * <p>The gameplay code ({@link LocalGameModel}, {@link AirportTycoonApp}) is
 * edition-agnostic: the ordered 1&rarr;8 mastery ladder ({@link EditionGimmicks})
 * is driven entirely by this single edition number, so the <em>same</em> Java is
 * copied into every numbered edition folder and only this constant changes.
 * Edition&nbsp;N therefore lights exactly systems&nbsp;1..N from identical code.
 *
 * <p>Override at launch with {@code -Dat.edition=N} to compare editions.
 */
public final class EditionConfig {

    private EditionConfig() {
    }

    /** This folder's edition (1..8). */
    public static final int EDITION = 7;
}
