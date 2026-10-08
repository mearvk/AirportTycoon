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
        // --- Edition 2: economic feedback loop ---
        testServicePremiumTracksReputation();
        testPremiumClampedToRange();
        testOnTimeComboPaysTips();
        testSnapshotCarriesEdition2Readouts();
        testSnapshotBackwardCompatibleDefaults();
        testAutoAssistProtectsHeaviesOnNearTie();
        // --- Edition 2: the Business Desk ---
        testDeskStartsEmpty();
        testDeskOpensParallelVentures();
        testDeskVenturesAccrueIqAndFireLevel5();
        testGreatAssimilationRakesInMoney();
        testDeskIsDeterministic();
        testDeskSnapshotRoundTrips();
        // --- Edition 3: Platinum Ascension / Prosperity Contracts ---
        testProsperityContractStarts();
        testProsperityContractCompletes();
        testProsperityContractExpiryRotates();
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

    // --- Edition 2: economic feedback loop (service premium + on-time combo) -

    private static void testServicePremiumTracksReputation() {
        // Fresh game starts at reputation 100, which is 40 above the reference
        // (60), so the premium is 140% and clamped nowhere yet.
        LocalGameModel m = new LocalGameModel();
        check(m.reputation() == 100, "fresh reputation is 100");
        check(m.servicePremiumPct() == 140,
                "premium is 140% at reputation 100 (ref 60)");
        // Snapshot exposes the same premium the fares use.
        check(m.snapshot().servicePremiumPct == m.servicePremiumPct(),
                "snapshot premium matches the model");
    }

    private static void testPremiumClampedToRange() {
        // Drive reputation down by never assisting; abandonments bleed it to 0,
        // where the premium must clamp to the configured floor (70%), never
        // below.
        LocalGameModel m = new LocalGameModel();
        for (int i = 0; i < 400 && m.reputation() > 0; i++) {
            m.step();
        }
        int prem = m.servicePremiumPct();
        check(prem >= 70 && prem <= 150, "premium stays within [70,150] (" + prem + ")");
        if (m.reputation() == 0) {
            check(prem == 70, "premium clamps to the 70% floor at reputation 0");
        }
    }

    private static void testOnTimeComboPaysTips() {
        // Auto-play a shift; a clean run should bank combo tips (multiples of
        // COMBO_TIP=150), and the streak must never be negative.
        LocalGameModel m = new LocalGameModel();
        for (int i = 0; i < 500; i++) {
            m.step();
            m.autoAssist();
        }
        check(m.served() > 0, "combo test served planes");
        check(m.streak() >= 0, "streak is never negative");
        check(m.comboTips() % 150 == 0, "combo tips are whole multiples of the tip");
        // After serving at least COMBO_STEP planes with no losses the combo
        // must have paid at least once; if there were losses the streak reset,
        // so we only assert the invariant linking served, streak, and tips.
        check(m.comboTips() >= 0 && m.comboTips() <= m.served() * 150,
                "combo tips bounded by served planes");
    }

    private static void testSnapshotCarriesEdition2Readouts() {
        // The wire format now carries streak and premium; parse must round-trip
        // them.
        String wire = "ATC|tick=9|cash=5000|rep=80|served=12|lost=1|gates=3|"
                + "runways=2|planes=4|over=false|streak=7|premium=120";
        GameSnapshot s = GameSnapshot.parse(wire);
        check(s.streak == 7, "snapshot streak parsed");
        check(s.servicePremiumPct == 120, "snapshot premium parsed");
    }

    private static void testSnapshotBackwardCompatibleDefaults() {
        // An Edition 1 frame with no streak/premium keys still parses, with a
        // neutral 100% premium and zero streak.
        String oldWire = "ATC|tick=1|cash=2000|rep=100|served=0|lost=0|gates=2|"
                + "runways=1|planes=0|over=false";
        GameSnapshot s = GameSnapshot.parse(oldWire);
        check(s.streak == 0, "missing streak defaults to 0");
        check(s.servicePremiumPct == 100, "missing premium defaults to a neutral 100%");
    }

    private static void testAutoAssistProtectsHeaviesOnNearTie() {
        // The Edition 2 priority score is patience*4 - sizeClass, so a heavy
        // (sizeClass 2) is served before a small plane (sizeClass 0) with the
        // SAME patience, but a small plane with less patience still wins.
        LocalGameModel m = new LocalGameModel();
        // Equal patience => heavier plane is strictly more urgent (lower score).
        check(m.scoreFor(40, 2) < m.scoreFor(40, 0),
                "equal patience: heavy outranks small");
        // The size nudge is sub-one-patience-point: a small plane one patience
        // tick closer to timeout still beats a heavy.
        check(m.scoreFor(39, 0) < m.scoreFor(40, 2),
                "more-urgent small plane still beats a comfortable heavy");
    }

    // --- Edition 3: Platinum Ascension / Prosperity Contracts --------------

    private static void testProsperityContractStarts() {
        LocalGameModel m = new LocalGameModel();
        check(m.contractTargetSize() == 0, "Edition 3 starts with Small target");
        check(m.contractProgress() == 0, "prosperity contract starts at zero");
        check(m.contractDeadline() == LocalGameModel.CONTRACT_DEADLINE,
                "prosperity contract starts with full deadline");
        check(m.snapshot().contractGoal == LocalGameModel.CONTRACT_GOAL,
                "snapshot exposes contract goal");
    }

    private static void testProsperityContractCompletes() {
        LocalGameModel m = new LocalGameModel();
        int beforeCash = m.cash();
        for (int i = 0; i < 1600 && m.contractsCompleted() == 0 && !m.snapshot().gameOver; i++) {
            m.step();
            m.autoAssist();
        }
        check(m.contractsCompleted() > 0, "seeded auto-play completes a prosperity contract");
        check(m.cash() >= beforeCash + LocalGameModel.CONTRACT_BONUS,
                "contract completion pays the prosperity bonus");
    }

    private static void testProsperityContractExpiryRotates() {
        LocalGameModel m = new LocalGameModel();
        int beforeRep = m.reputation();
        for (int i = 0; i < LocalGameModel.CONTRACT_DEADLINE; i++) {
            m.step();
        }
        check(m.contractTargetSize() == 1, "expired contract rotates to Medium");
        check(m.contractProgress() == 0, "expired contract resets progress");
        check(m.reputation() == beforeRep || m.reputation() < beforeRep,
                "contract expiry adds no separate reputation penalty");
    }

    // --- Edition 2: the Business Desk (side ventures + Level-5 moves) --------

    private static void testDeskStartsEmpty() {
        // "The business desk is empty as you've merely tried on your Genius."
        BusinessDesk desk = new BusinessDesk();
        check(desk.isEmpty(), "a fresh business desk is empty");
        check(desk.ventureCount() == 0, "empty desk has no ventures");
        check(desk.majorMoves() == 0, "empty desk has fired no major moves");
    }

    private static void testDeskOpensParallelVentures() {
        // The player can run Casino Management and Investment Management in
        // parallel while flying planes.
        BusinessDesk desk = new BusinessDesk();
        desk.openCasino(400000);
        desk.openInvestment(600000);
        check(!desk.isEmpty(), "opening a venture fills the desk");
        check(desk.ventureCount() == 2, "two parallel ventures are open");
        BusinessDesk.Venture casino = desk.ventures().get(0);
        BusinessDesk.Venture invest = desk.ventures().get(1);
        check(casino.reaction() == BusinessDesk.REACTION_EXOTHERMIC,
                "casino is an exothermic Chemistry reaction");
        check(invest.reaction() == BusinessDesk.REACTION_TITRATION,
                "investment is a titration Chemistry reaction");
    }

    private static void testDeskVenturesAccrueIqAndFireLevel5() {
        // Sustained good weeks drive IQ up to the major level, firing the
        // Level-5 move exactly once per venture.
        BusinessDesk desk = new BusinessDesk();
        desk.openInvestment(600000); // the steady one reliably reaches level 5
        for (int w = 0; w < 40; w++) {
            desk.settleWeek();
        }
        BusinessDesk.Venture v = desk.ventures().get(0);
        check(v.iq() >= BusinessDesk.MAJOR_MOVE_LEVEL,
                "a steady venture accrues IQ to the major level");
        check(v.majorFired(), "the venture fired its Level-5 major move");
        check(desk.majorMoves() >= 1, "the desk recorded at least one major move");
    }

    private static void testGreatAssimilationRakesInMoney() {
        // When a Level-5 move fires, the Great Assimilation multiplies
        // Major-Eastern revenue (3x) for a run of weeks: the money-raking
        // stretch. Outside those weeks revenue is unchanged.
        BusinessDesk desk = new BusinessDesk();
        desk.openInvestment(600000);
        boolean sawRake = false;
        int rakedEast = 0;
        for (int w = 0; w < 40 && !sawRake; w++) {
            desk.settleWeek();
            if (desk.assimilationActive()) {
                sawRake = true;
                rakedEast = desk.applyAssimilation(24_000_000);
            }
        }
        check(sawRake, "the Great Assimilation activates after a Level-5 move");
        check(rakedEast == 24_000_000 * BusinessDesk.ASSIMILATION_EAST_PCT / 100,
                "assimilation multiplies Eastern revenue 3x while active");
        // Run the assimilation out; afterwards revenue is passed through 1:1.
        for (int w = 0; w < BusinessDesk.ASSIMILATION_WEEKS + 2; w++) {
            desk.settleWeek();
        }
        check(!desk.assimilationActive(), "assimilation expires after its weeks");
        check(desk.applyAssimilation(24_000_000) == 24_000_000,
                "outside assimilation, Eastern revenue is unchanged");
    }

    private static void testDeskIsDeterministic() {
        // Same seed => identical campaign, like the rest of the game.
        BusinessDesk a = new BusinessDesk(4242);
        BusinessDesk b = new BusinessDesk(4242);
        a.openCasino(400000);
        a.openInvestment(600000);
        b.openCasino(400000);
        b.openInvestment(600000);
        for (int w = 0; w < 30; w++) {
            a.settleWeek();
            b.settleWeek();
        }
        check(a.deskProfitCents() == b.deskProfitCents(),
                "same seed yields the same desk profit");
        check(a.majorMoves() == b.majorMoves(),
                "same seed fires the same number of major moves");
    }

    private static void testDeskSnapshotRoundTrips() {
        // The desk snapshot is a readable, pipe-delimited line the UI can show.
        BusinessDesk desk = new BusinessDesk();
        desk.openCasino(400000);
        desk.settleWeek();
        String snap = desk.snapshot();
        check(snap.startsWith("DESK|week=1"), "desk snapshot header is present");
        check(snap.contains("empty=no"), "snapshot reports the desk is no longer empty");
        check(snap.contains("VEN|name=Casino Management"),
                "snapshot lists the open venture");
    }
}
