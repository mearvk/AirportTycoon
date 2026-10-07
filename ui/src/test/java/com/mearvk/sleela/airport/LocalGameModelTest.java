package com.mearvk.sleela.airport;

/**
 * Lightweight, dependency-free assertions for the Java mirror of the SLeeLa
 * rules. Run via {@code mvn test} (Surefire picks up {@code *Test}); the
 * {@code testMain} entry also lets it run with a plain {@code java} launcher.
 *
 * <p>Kept free of JUnit so the project builds and self-checks even in a minimal
 * environment, mirroring the deliberately narrow, testable scope the SLeeLa
 * Audio GUI follows.
 */
public final class LocalGameModelTest {

    private static int failures = 0;

    public static void main(String[] args) {
        testSnapshotRoundTrip();
        testAutoAssistServesPlanes();
        testIdleAirportLosesReputation();
        testBuyGate();
        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println("TESTS FAILED: " + failures);
            System.exit(1);
        }
    }

    // Surefire-visible test method.
    public void testMain() {
        main(new String[0]);
    }

    private static void check(boolean cond, String label) {
        if (cond) {
            System.out.println("ok   - " + label);
        } else {
            System.out.println("FAIL - " + label);
            failures++;
        }
    }

    private static void testSnapshotRoundTrip() {
        String wire = "ATC|tick=5|cash=1800|rep=97|served=2|lost=0|gates=2|runways=1|planes=1|over=false\n"
                + "{\"__type\":\"Plane\",\"id\":7,\"flight\":\"SL7\",\"state\":1,"
                + "\"sizeClass\":2,\"patience\":40,\"maxPatience\":80,"
                + "\"serviceLeft\":22,\"posX\":0.12,\"posY\":0.3}";
        GameSnapshot s = GameSnapshot.parse(wire);
        check(s.tick == 5, "snapshot tick parsed");
        check(s.cash == 1800, "snapshot cash parsed");
        check(s.reputation == 97, "snapshot reputation parsed");
        check(s.planes.size() == 1, "one plane parsed");
        GameSnapshot.PlaneView p = s.planes.get(0);
        check(p.id == 7 && p.flight.equals("SL7"), "plane id/flight parsed");
        check(p.sizeClass == 2 && p.patience == 40, "plane fields parsed");
        check(Math.abs(p.posX - 0.12) < 1e-9, "plane posX parsed");
        check(Math.abs(p.patienceFraction() - 0.5) < 1e-9, "patience fraction");
    }

    private static void testAutoAssistServesPlanes() {
        LocalGameModel m = new LocalGameModel();
        for (int i = 0; i < 400; i++) {
            m.step();
            m.autoAssist();
        }
        GameSnapshot s = m.snapshot();
        check(m.served() > 0, "auto-assist serves at least one plane");
        check(m.cash() >= 0, "cash never goes negative");
        check(s.tick == 400 || s.gameOver, "ticked the simulation");
    }

    private static void testIdleAirportLosesReputation() {
        LocalGameModel m = new LocalGameModel();
        // Never assist: planes should start timing out and cost reputation.
        for (int i = 0; i < 300; i++) {
            m.step();
        }
        check(m.reputation() < 100, "idle airport loses reputation");
    }

    private static void testBuyGate() {
        LocalGameModel m = new LocalGameModel();
        GameSnapshot before = m.snapshot();
        boolean bought = m.buyGate();
        GameSnapshot after = m.snapshot();
        check(bought, "can buy a gate with starting cash");
        check(after.openGates == before.openGates + 1, "gate count increased");
        check(after.cash < before.cash, "buying a gate costs cash");
    }
}
