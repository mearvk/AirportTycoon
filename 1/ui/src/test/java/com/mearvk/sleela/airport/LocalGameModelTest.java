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
        testLifeOwnerEarnsFromBusinessModel();
        testLifeTravelersBuySeats();
        testLifeHighTaxGroundsTravelers();
        testEasternWinThreshold();
        testWinRequiresMonthOverMonthIncrease();
        testAuthorPathWinsInExactly1001Moves();
        testAuthorPathWinsFromAnyStartingState();
        testTerminalAuthorYes();
        testTerminalNumberFlow();
        testTerminalAuthorNumber1001();
        testTerminalOsCommands();
        testMediaPriceTiers();
        testMediaCountriesHaveThreeTheories();
        testMediaFlightsMainlyOnTime();
        testMediaReporterFusesAuthorAndEast();
        testMediaSoftPad();
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

    // --- business/life layer (Character owner + Citizen travelers) ---

    private static void testLifeOwnerEarnsFromBusinessModel() {
        LifeEconomy life = new LifeEconomy();
        int before = life.ownerCashCents();
        life.liveAMonth(0); // no tower fares; pure business-model profit
        int after = life.ownerCashCents();
        // SubscriptionModel(20,90000,5,30000): retained=19 -> rev 1,710,000;
        // cost 20*30000=600,000; profit 1,110,000. Plus travelers buying seats.
        check(after > before, "owner cash grows from a profitable month");
        check(life.month() == 1, "month advanced");
        // reputation starts at 50 and climbs +3 per profitable month, so
        // "thriving" (needs reputation >= 60) takes a few good months, exactly
        // as the vendored Character.sleela specifies.
        for (int i = 0; i < 5; i++) {
            life.liveAMonth(0);
        }
        check(life.ownerThriving(), "owner thrives after sustained profit");
    }

    private static void testLifeTravelersBuySeats() {
        LifeEconomy life = new LifeEconomy();
        int sold = life.liveAMonth(0);
        check(sold > 0, "able, solvent travelers buy seats");
        check(sold <= life.seatsOffered(), "seats sold never exceed offered");
    }

    private static void testLifeHighTaxGroundsTravelers() {
        LifeEconomy life = new LifeEconomy();
        life.setTaxRate(100);               // confiscatory tax
        life.setTicketPrice(1_000_000);     // $10,000 ticket nobody can afford
        int sold = life.liveAMonth(0);
        check(sold == 0, "unaffordable tickets ground every traveler");
    }

    // --- the win condition: >$240k in a Major Eastern Region, rising m/m ---

    private static void testEasternWinThreshold() {
        // Below the threshold with no Eastern fare: not a win.
        LifeEconomy low = new LifeEconomy();
        low.liveAMonth(0);
        check(!low.won(), "small Eastern revenue is not a win");

        // Push well past $240,000 of Eastern fare income in month 1.
        LifeEconomy hi = new LifeEconomy();
        hi.liveAMonth(LifeEconomy.EAST_WIN_THRESHOLD_CENTS + 500_000);
        check(hi.eastRevenueCents() > LifeEconomy.EAST_WIN_THRESHOLD_CENTS,
                "Eastern revenue clears the $240,000 bar");
        check(hi.won(), "month over $240k East and rising from zero wins");
    }

    private static void testWinRequiresMonthOverMonthIncrease() {
        LifeEconomy life = new LifeEconomy();
        // Month 1: a big winning East revenue (rises from 0).
        life.liveAMonth(LifeEconomy.EAST_WIN_THRESHOLD_CENTS + 1_000_000);
        check(life.won(), "month 1 wins");
        int m1East = life.eastRevenueCents();
        // Month 2: still above the bar, but NOT higher than month 1 -> no win,
        // because the win requires an increase month over month.
        life.liveAMonth(LifeEconomy.EAST_WIN_THRESHOLD_CENTS + 1_000_000 - 500_000);
        check(life.eastRevenueCents() > LifeEconomy.EAST_WIN_THRESHOLD_CENTS,
                "month 2 still above the $240k bar");
        check(life.eastRevenueCents() < m1East, "month 2 East dipped vs month 1");
        check(!life.won(), "no increase over previous month => not a win");
    }

    // --- the author path: wins in exactly 1001 moves, every time ---

    private static void testAuthorPathWinsInExactly1001Moves() {
        AuthorPath.Result r = AuthorPath.playCanonical();
        check(r.moves == 1001, "author path is exactly 1001 moves");
        check(r.won, "author path wins");
        check(r.winningMonths == r.monthsPlayed && r.monthsPlayed >= 1,
                "every month the author plays is a winning month");
        check(r.finalEastCents > LifeEconomy.EAST_WIN_THRESHOLD_CENTS,
                "author finishes above the $240k Eastern bar");
    }

    private static void testAuthorPathWinsFromAnyStartingState() {
        // "Wins in every case": vary the starting economy (ticket price, tax,
        // and how many months the author advances) and confirm the author path
        // still wins and is still exactly 1001 moves.
        int cases = 0;
        int wins = 0;
        for (int ticket = 10000; ticket <= 40000; ticket += 10000) {
            for (int tax = 0; tax <= 40; tax += 20) {
                for (int liveMonths = 1; liveMonths <= 24; liveMonths += 23) {
                    LifeEconomy econ = new LifeEconomy();
                    econ.setTicketPrice(ticket);
                    econ.setTaxRate(tax);
                    AuthorPath.Result r =
                            AuthorPath.play(econ, AuthorPath.build(liveMonths));
                    cases++;
                    if (r.won && r.moves == 1001
                            && r.winningMonths == r.monthsPlayed) {
                        wins++;
                    }
                }
            }
        }
        check(cases > 0, "exercised multiple starting states");
        check(wins == cases, "author path wins in every case (" + wins + "/" + cases + ")");
    }

    // --- the author terminal: asks if you're the Author, else your Number ---

    private static void testTerminalAuthorYes() {
        AuthorTerminal t = new AuthorTerminal();
        check(t.banner().contains("Are you the Author?"), "terminal asks if you are the Author");
        String reply = t.handleLine("yes");
        check(reply.contains("Welcome back, Author"), "answering yes greets the Author");
        check(reply.contains("1001"), "author greeting cites the 1001-move path");
        check(t.isAuthor(), "author flag set on yes");
    }

    private static void testTerminalNumberFlow() {
        AuthorTerminal t = new AuthorTerminal();
        String q = t.handleLine("no");
        check(q.equals("What is your Number?"), "answering no asks for your Number");
        String reply = t.handleLine("2");
        check(reply.contains("New York") && reply.contains("Major Eastern"),
                "number 2 => New York, a Major Eastern Region");
        AuthorTerminal t2 = new AuthorTerminal();
        t2.handleLine("no");
        String west = t2.handleLine("12");
        check(west.contains("San Francisco") && !west.contains("Major Eastern"),
                "number 12 => San Francisco, not Major Eastern");
    }

    private static void testTerminalAuthorNumber1001() {
        AuthorTerminal t = new AuthorTerminal();
        t.handleLine("no");
        String reply = t.handleLine("1001");
        check(reply.contains("author's number") && reply.contains("Author after all"),
                "number 1001 reveals you are the Author");
        check(t.isAuthor(), "1001 sets the author flag");
    }

    private static void testTerminalOsCommands() {
        AuthorTerminal t = new AuthorTerminal();
        t.handleLine("no");
        t.handleLine("7"); // Chicago -> into the shell
        check(t.handleLine("platform").matches("Windows|Linux|macOS"),
                "platform command returns a real OS name");
        check(!t.handleLine("whoami").isEmpty(), "whoami returns a user name");
        check(t.handleLine("help").contains("whoami"), "help lists commands");
        check(t.handleLine("author?").equals("no"), "non-author reports author?=no");
        check(t.handleLine("bogus").contains("unknown command"), "unknown command handled");
        check(t.handleLine("exit").equals("bye"), "exit says bye");
    }

    // --- the media / events center + the Reporter (Boss) ---

    private static void testMediaPriceTiers() {
        // Six real tiers 1.0..6.0, strictly increasing, 6 the most luxurious.
        int prev = -1;
        for (int t = 1; t <= 6; t++) {
            int fare = MediaCenter.fareForTierX10(t * 10);
            check(fare > prev, "tier " + t + " fare rises ($" + fare + ")");
            prev = fare;
        }
        check(MediaCenter.fareForTierX10(60) == 1360, "tier 6 is the most luxurious ($1360)");
    }

    private static void testMediaCountriesHaveThreeTheories() {
        MediaCenter m = new MediaCenter();
        check(m.countries().size() == 5, "five focus countries present");
        boolean allThree = true;
        boolean haveAsia = false, haveEuam = false;
        for (MediaCenter.Country c : m.countries()) {
            if (c.theories().length != 3) {
                allThree = false;
            }
            if (c.region.equals("ASIA")) {
                haveAsia = true;
            }
            if (c.region.equals("EUAM")) {
                haveEuam = true;
            }
        }
        check(allThree, "every country has exactly 3 national theories");
        check(haveAsia && haveEuam, "both Asia and Euro-American regions represented");
    }

    private static void testMediaFlightsMainlyOnTime() {
        MediaCenter m = new MediaCenter();
        for (int i = 0; i < 12; i++) {
            m.tick();
        }
        int asia = m.regionOnTime("ASIA");
        int euam = m.regionOnTime("EUAM");
        check(asia >= 80, "Asia routes are mainly on time (" + asia + "%)");
        check(euam >= 75, "Euro-American routes are mainly on time (" + euam + "%)");
        // Contained by theories: never exceeds the strongest ceiling (98).
        for (MediaCenter.Route r : m.routes()) {
            check(r.onTimePct <= 100 && r.onTimePct >= 0, "route " + r.label + " on-time in range");
        }
    }

    private static void testMediaReporterFusesAuthorAndEast() {
        MediaCenter m = new MediaCenter();
        for (int i = 0; i < 8; i++) {
            m.tick();
        }
        m.report();
        check(MediaCenter.eastVersion().equals("East 5.0"), "Asia model is East 5.0");
        check(m.publishedAsiaOnTime() > 0 && m.publishedEuamOnTime() > 0,
                "reporter publishes both regions");
        check(m.confidence() >= 0 && m.confidence() <= 100, "confidence in range");
        check(m.headline().contains("Asia") || m.headline().contains("Euro-American"),
                "headline interprets the focus routes");
    }

    private static void testMediaSoftPad() {
        // The soft economic pad moves a value only part-way toward its target.
        int padded = MediaCenter.softPad(100, 200);
        check(padded > 100 && padded < 200, "soft pad moves gently toward target");
        // With a 35% pad, a 100->200 step should move 65 of the 100 gap.
        check(padded == 165, "soft pad applies the configured 35% cushion");
    }
}
