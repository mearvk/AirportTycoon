package com.mearvk.sleela.airport;

import java.util.ArrayList;
import java.util.List;

/**
 * A faithful Java mirror of {@code game/AirportTycoonLife.sleela} and the
 * SLeeLa {@code Character}/{@code Citizen} classes it builds on (vendored under
 * {@code /sources}). It lets the Sleela UI show and animate the business/life
 * layer even without the SLeeLa toolchain, the same way {@link LocalGameModel}
 * mirrors the tower game.
 *
 * <p>The arithmetic matches the upstream sources byte-for-byte in intent:
 * <ul>
 *   <li>{@code Character.liveAMonth()} realizes {@code BusinessModel.monthlyProfit()}
 *       into cash and nudges reputation/grit;</li>
 *   <li>{@code SubscriptionModel}: revenue = retained seats x price, cost =
 *       seats x serviceCost;</li>
 *   <li>{@code Citizen}: getPaid → deposit wage, payTaxes → withdraw a % of
 *       balance, treatYourself → withdraw a ticket if solvent and able.</li>
 * </ul>
 */
public final class LifeEconomy {

    // --- Character / BusinessModel (owner end) ---
    static final class Subscription {
        int subscribers, priceCents, churnPercent, serviceCostCents;

        Subscription(int seats, int price, int churn, int serviceCost) {
            subscribers = Math.max(0, seats);
            priceCents = Math.max(0, price);
            churnPercent = Math.min(100, Math.max(0, churn));
            serviceCostCents = Math.max(0, serviceCost);
        }

        int retained() {
            return subscribers - (subscribers * churnPercent) / 100;
        }

        int monthlyRevenue() {
            return retained() * priceCents;
        }

        int monthlyCost() {
            return subscribers * serviceCostCents;
        }

        int monthlyProfit() {
            return monthlyRevenue() - monthlyCost();
        }

        boolean profitable() {
            return monthlyProfit() > 0;
        }
    }

    static final class Character {
        String name;
        int grit;
        int reputation;
        int cashCents;
        Subscription model;

        void introduce(String who, int startingGrit, int startingCash) {
            name = who;
            grit = clamp(startingGrit);
            reputation = 50;
            cashCents = Math.max(0, startingCash);
        }

        int liveAMonth() {
            if (model == null) {
                return 0;
            }
            int profit = model.monthlyProfit();
            cashCents += profit;
            if (profit > 0) {
                reputation = clamp(reputation + 3);
                grit = clamp(grit + 1);
            } else {
                reputation = clamp(reputation - 2);
                grit = clamp(grit - 5);
            }
            return profit;
        }

        boolean stillStanding() {
            return cashCents >= 0 && grit > 0;
        }

        boolean thriving() {
            return model != null && model.profitable() && reputation >= 60;
        }

        static int clamp(int v) {
            return v < 0 ? 0 : Math.min(v, 100);
        }
    }

    // --- Citizen (traveler end) ---
    static final class Citizen {
        String name;
        int age;
        int stage;         // 1 minor, 2 working, 3 retired
        int mood;          // 0..100
        int sector;
        int wageCents;
        boolean onPayroll;
        int balanceCents;
        boolean bankOpen;
        boolean fedValid;

        static final int STAGE_MINOR = 1;
        static final int STAGE_WORKING = 2;

        void beBorn(String who) {
            name = who;
            age = 0;
            stage = STAGE_MINOR;
            mood = 100;
            bankOpen = true;
            fedValid = true;
            balanceCents = 0;
        }

        void haveABirthday() {
            age++;
            if (age >= 18 && stage == STAGE_MINOR) {
                stage = STAGE_WORKING;
            }
            mood = Math.min(100, mood + 1);
        }

        void getAJob(int whichSector, int cents) {
            sector = whichSector;
            wageCents = Math.max(0, cents);
            onPayroll = whichSector != 0;
            if (stage == STAGE_MINOR && age >= 16) {
                stage = STAGE_WORKING;
            }
            mood = Math.min(100, mood + 5);
        }

        boolean employed() {
            return onPayroll && sector != 0 && stage == STAGE_WORKING;
        }

        int getPaid() {
            if (!employed()) {
                return 0;
            }
            balanceCents += wageCents;
            mood = Math.min(100, mood + 10);
            return wageCents;
        }

        int payTaxes(int ratePercent) {
            int r = Math.min(100, Math.max(0, ratePercent));
            int owed = (balanceCents * r) / 100;
            balanceCents -= owed;
            mood = Math.max(0, mood - 15);
            return owed;
        }

        boolean treatYourself(int cents) {
            if (!bankOpen || cents <= 0 || cents > balanceCents) {
                return false;
            }
            balanceCents -= cents;
            mood = Math.min(100, mood + 8);
            return true;
        }

        boolean alive() {
            return stage != 0 && stage != 4;
        }

        boolean isAble() {
            return alive() && fedValid && bankOpen && mood > 0;
        }

        int netWorthCents() {
            return balanceCents;
        }
    }

    // Federal Reserve districts (mirror of the imported FederalReserveID).
    static final int DISTRICT_BOSTON = 1;
    static final int DISTRICT_NEW_YORK = 2;
    static final int DISTRICT_PHILADELPHIA = 3;
    static final int DISTRICT_RICHMOND = 5;
    static final int DISTRICT_ATLANTA = 6;
    static final int DISTRICT_CHICAGO = 7;
    static final int DISTRICT_SAN_FRANCISCO = 12;

    /**
     * The win threshold: a winning month needs MORE THAN $240,000
     * ($24,000,000 cents) of revenue in the Major Eastern Region, and that
     * revenue must rise month over month both overall and in the East.
     */
    static final int EAST_WIN_THRESHOLD_CENTS = 24_000_000;

    private Character owner;
    private final List<Citizen> pax = new ArrayList<>();
    private final List<Integer> paxDistrict = new ArrayList<>();

    private int month;
    private int ticketPriceCents = 24000;
    private int seatsOffered = 20;
    private int taxRatePercent = 18;
    private int seatsSold;
    private int ownerAddOnCash;

    // Regional revenue tracking (the win condition lives here).
    private int revenueTotalCents;
    private int revenueEastCents;
    private int prevRevenueTotalCents;
    private int prevRevenueEastCents;
    private boolean wonThisMonth;

    public LifeEconomy() {
        setup();
    }

    public void setup() {
        month = 0;
        ticketPriceCents = 24000;
        seatsOffered = 20;
        taxRatePercent = 18;
        seatsSold = 0;
        ownerAddOnCash = 0;

        owner = new Character();
        owner.introduce("Avery Sloane", 70, 500000);
        owner.model = new Subscription(20, 90000, 5, 30000);

        pax.clear();
        pax.add(makeCitizen("Dana Reyes", 11, 650000));
        pax.add(makeCitizen("Marco Hale", 8, 720000));
        pax.add(makeCitizen("Priya Nandi", 9, 580000));
        pax.add(makeCitizen("Sam Okafor", 10, 300000));
        pax.add(makeCitizen("Lin Zhou", 6, 260000));

        // Region tags: three Major-Eastern, two not — so East < overall.
        paxDistrict.clear();
        paxDistrict.add(DISTRICT_NEW_YORK);
        paxDistrict.add(DISTRICT_BOSTON);
        paxDistrict.add(DISTRICT_PHILADELPHIA);
        paxDistrict.add(DISTRICT_CHICAGO);
        paxDistrict.add(DISTRICT_SAN_FRANCISCO);

        revenueTotalCents = 0;
        revenueEastCents = 0;
        prevRevenueTotalCents = 0;
        prevRevenueEastCents = 0;
        wonThisMonth = false;
    }

    /** The Major Eastern Region: the major eastern Fed districts. */
    static boolean isMajorEast(int district) {
        return district == DISTRICT_BOSTON
                || district == DISTRICT_NEW_YORK
                || district == DISTRICT_PHILADELPHIA
                || district == DISTRICT_RICHMOND
                || district == DISTRICT_ATLANTA;
    }

    private Citizen makeCitizen(String who, int sector, int wage) {
        Citizen c = new Citizen();
        c.beBorn(who);
        for (int b = 0; b < 30; b++) {
            c.haveABirthday();
        }
        c.getAJob(sector, wage);
        return c;
    }

    /**
     * Advance one in-game month. {@code fareIncomeCents} is the operating cash
     * the tower game earned this month, folded onto the owner's book.
     */
    public int liveAMonth(int fareIncomeCents) {
        month++;

        // Roll this month's figures into "previous" before recomputing.
        prevRevenueTotalCents = revenueTotalCents;
        prevRevenueEastCents = revenueEastCents;
        revenueTotalCents = 0;
        revenueEastCents = 0;

        seatsSold = 0;
        for (int i = 0; i < pax.size(); i++) {
            Citizen c = pax.get(i);
            c.getPaid();
            c.payTaxes(taxRatePercent);
            if (c.isAble() && c.netWorthCents() >= ticketPriceCents
                    && seatsSold < seatsOffered && c.treatYourself(ticketPriceCents)) {
                seatsSold++;
                revenueTotalCents += ticketPriceCents;
                if (isMajorEast(paxDistrict.get(i))) {
                    revenueEastCents += ticketPriceCents;
                }
            }
        }

        // The tower game's fare income lands at the airport's own region — a
        // Major Eastern hub (New York) — so it counts toward both totals.
        revenueTotalCents += fareIncomeCents;
        revenueEastCents += fareIncomeCents;

        owner.liveAMonth();
        ownerAddOnCash += revenueTotalCents;

        wonThisMonth = winningMonth();
        return seatsSold;
    }

    /**
     * The win condition. A winning month needs all three: Major-Eastern revenue
     * strictly above $240,000, and an increase over the previous month both
     * overall and in the Major Eastern Region.
     */
    public boolean winningMonth() {
        return revenueEastCents > EAST_WIN_THRESHOLD_CENTS
                && revenueTotalCents > prevRevenueTotalCents
                && revenueEastCents > prevRevenueEastCents;
    }

    public boolean setTicketPrice(int cents) {
        if (cents < 0) {
            return false;
        }
        ticketPriceCents = cents;
        return true;
    }

    public boolean setTaxRate(int percent) {
        if (percent < 0 || percent > 100) {
            return false;
        }
        taxRatePercent = percent;
        return true;
    }

    // --- read-side accessors for the UI ---
    public int month() {
        return month;
    }

    public String ownerName() {
        return owner.name;
    }

    public int ownerCashCents() {
        return owner.cashCents + ownerAddOnCash;
    }

    public int ownerReputation() {
        return owner.reputation;
    }

    public int ownerGrit() {
        return owner.grit;
    }

    public boolean ownerThriving() {
        return owner.thriving();
    }

    public boolean ownerStillStanding() {
        return owner.stillStanding();
    }

    public int ticketPriceCents() {
        return ticketPriceCents;
    }

    public int taxRatePercent() {
        return taxRatePercent;
    }

    public int seatsOffered() {
        return seatsOffered;
    }

    public int seatsSold() {
        return seatsSold;
    }

    // --- win-condition read-side ---
    public boolean won() {
        return wonThisMonth;
    }

    public int eastRevenueCents() {
        return revenueEastCents;
    }

    public int totalRevenueCents() {
        return revenueTotalCents;
    }

    public int prevEastRevenueCents() {
        return prevRevenueEastCents;
    }

    public int prevTotalRevenueCents() {
        return prevRevenueTotalCents;
    }

    public int eastWinThresholdCents() {
        return EAST_WIN_THRESHOLD_CENTS;
    }

    public List<String> travelerLines() {
        List<String> out = new ArrayList<>();
        for (Citizen c : pax) {
            out.add(String.format("%s  worth=$%d  mood=%d  able=%s",
                    c.name, c.netWorthCents() / 100, c.mood, c.isAble() ? "yes" : "no"));
        }
        return out;
    }
}
