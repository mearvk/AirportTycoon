package com.mearvk.sleela.airport;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Airport Tycoon &mdash; the Sleela UI.
 *
 * <p>A JavaFX host that renders the SLeeLa Wrapper's state and animates planes
 * flowing through the airport. This is the presentation/animation substrate
 * described in {@code gui/INTEGRATION.md} (Path 2): all rules live in
 * {@code game/AirportTycoon.sleela}; this class only draws and forwards intents.
 *
 * <p>The animation loop ticks the simulation at ~10&nbsp;Hz and interpolates
 * plane sprites toward their reported positions every frame for smooth motion.
 */
public final class AirportTycoonApp extends Application {

    private static final double W = 960;        // canvas / field width
    private static final double H = 640;        // window height
    private static final double FIELD_H = 440;
    private static final double WINDOW_W = 1280; // room for the economy panel

    private final SleelaRuntime runtime = new SleelaProcessRuntime();

    // The business/life layer (Character owner + Citizen travelers), mirroring
    // game/AirportTycoonLife.sleela and the vendored /sources classes.
    private final LifeEconomy life = new LifeEconomy();

    // Smoothed sprite positions keyed by plane id (for interpolation).
    private final java.util.Map<Integer, double[]> sprites = new java.util.HashMap<>();

    private Canvas canvas;
    private Label hud;
    private Label backendLabel;
    private Label messageLabel;
    private Label economyLabel;
    private ToggleButton pauseBtn;

    private long lastTickNanos = 0;
    private static final long TICK_NANOS = 100_000_000L; // 10 Hz
    // One in-game "month" of the business/life layer per this many ticks.
    private static final int TICKS_PER_MONTH = 60;
    private int cashAtMonthStart = 0;
    private boolean paused = false;

    @Override
    public void start(Stage stage) {
        runtime.reset();

        canvas = new Canvas(W, FIELD_H);

        hud = new Label();
        hud.setFont(Font.font("Consolas", FontWeight.BOLD, 15));
        hud.setTextFill(Color.web("#e8f0ff"));

        backendLabel = new Label("backend: " + runtime.backendName());
        backendLabel.setFont(Font.font("Consolas", 11));
        backendLabel.setTextFill(Color.web("#7fa8d0"));

        messageLabel = new Label("Clear the most urgent plane. Space = pause, A = auto.");
        messageLabel.setFont(Font.font("Consolas", 12));
        messageLabel.setTextFill(Color.web("#aee6b4"));

        economyLabel = new Label();
        economyLabel.setFont(Font.font("Consolas", 12));
        economyLabel.setTextFill(Color.web("#e8f0ff"));

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0b1220;");
        root.setTop(buildHeader());
        root.setCenter(canvas);
        root.setRight(buildEconomyPanel());
        root.setBottom(buildControls());

        cashAtMonthStart = runtime.snapshot().cash;

        Scene scene = new Scene(root, W, H);
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case SPACE -> togglePause();
                case A -> runtime.autoAssist();
                case L -> autoFor(GameSnapshot.INBOUND);
                case G -> buyGateFeedback();
                case R -> buyRunwayFeedback();
                default -> { /* ignore */ }
            }
        });

        stage.setTitle("Airport Tycoon — Business Edition (SLeeLa)");
        stage.setScene(scene);
        stage.show();

        AnimationTimer loop = new AnimationTimer() {
            @Override
            public void run(long now) {
                if (lastTickNanos == 0) {
                    lastTickNanos = now;
                }
                if (!paused && now - lastTickNanos >= TICK_NANOS) {
                    runtime.step();
                    lastTickNanos = now;
                    maybeAdvanceMonth();
                }
                GameSnapshot snap = runtime.snapshot();
                updateSprites(snap);
                render(snap);
                updateHud(snap);
                updateEconomy();
            }
        };
        loop.start();
    }

    private Region buildHeader() {
        VBox box = new VBox(2, hud, backendLabel);
        box.setPadding(new Insets(10, 14, 8, 14));
        box.setStyle("-fx-background-color: #111a2e;");
        return box;
    }

    private Region buildControls() {
        Button auto = new Button("Auto-assist (A)");
        auto.setOnAction(e -> runtime.autoAssist());

        pauseBtn = new ToggleButton("Pause (Space)");
        pauseBtn.setOnAction(e -> togglePause());

        Button buyGate = new Button("Buy gate (G)");
        buyGate.setOnAction(e -> buyGateFeedback());

        Button buyRunway = new Button("Buy runway (R)");
        buyRunway.setOnAction(e -> buyRunwayFeedback());

        Button restart = new Button("Restart");
        restart.setOnAction(e -> {
            runtime.reset();
            life.setup();
            sprites.clear();
            cashAtMonthStart = runtime.snapshot().cash;
            message("New shift started.");
        });

        HBox buttons = new HBox(8, auto, pauseBtn, buyGate, buyRunway, restart);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(6, buttons, messageLabel);
        box.setPadding(new Insets(10, 14, 12, 14));
        box.setStyle("-fx-background-color: #111a2e;");
        VBox.setVgrow(box, Priority.NEVER);
        return box;
    }

    private Region buildEconomyPanel() {
        Label title = new Label("THE BUSINESS & THE TRAVELERS");
        title.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        title.setTextFill(Color.web("#9bc4ff"));

        Label sub = new Label("Owner = Character · Passengers = Citizen\n(both ends of the economy)");
        sub.setFont(Font.font("Consolas", 10));
        sub.setTextFill(Color.web("#7fa8d0"));

        Button priceUp = new Button("Ticket +$20");
        priceUp.setOnAction(e -> {
            life.setTicketPrice(life.ticketPriceCents() + 2000);
            message("Ticket price now $" + (life.ticketPriceCents() / 100));
        });
        Button priceDown = new Button("Ticket -$20");
        priceDown.setOnAction(e -> {
            life.setTicketPrice(Math.max(0, life.ticketPriceCents() - 2000));
            message("Ticket price now $" + (life.ticketPriceCents() / 100));
        });
        Button taxUp = new Button("Tax +1%");
        taxUp.setOnAction(e -> {
            life.setTaxRate(Math.min(100, life.taxRatePercent() + 1));
            message("Traveler tax now " + life.taxRatePercent() + "%");
        });
        Button taxDown = new Button("Tax -1%");
        taxDown.setOnAction(e -> {
            life.setTaxRate(Math.max(0, life.taxRatePercent() - 1));
            message("Traveler tax now " + life.taxRatePercent() + "%");
        });

        HBox priceRow = new HBox(6, priceUp, priceDown);
        HBox taxRow = new HBox(6, taxUp, taxDown);

        VBox box = new VBox(8, title, sub, economyLabel, priceRow, taxRow);
        box.setPadding(new Insets(12));
        box.setPrefWidth(300);
        box.setStyle("-fx-background-color: #0e1830; -fx-border-color: #1c2a44; -fx-border-width: 0 0 0 1;");
        return box;
    }

    // Advance the business/life layer one in-game month every TICKS_PER_MONTH
    // ticks, folding the tower game's fare income earned this month into the
    // owner's Character book.
    private void maybeAdvanceMonth() {
        GameSnapshot snap = runtime.snapshot();
        if (snap.tick > 0 && snap.tick % TICKS_PER_MONTH == 0) {
            int fareIncomeCents = Math.max(0, (snap.cash - cashAtMonthStart)) * 100;
            life.liveAMonth(fareIncomeCents);
            cashAtMonthStart = snap.cash;
            message(String.format("Month %d closed — %d/%d seats sold, owner cash $%d (rep %d).",
                    life.month(), life.seatsSold(), life.seatsOffered(),
                    life.ownerCashCents() / 100, life.ownerReputation()));
        }
    }

    private void updateEconomy() {
        StringBuilder sb = new StringBuilder();
        sb.append("Month ").append(life.month()).append('\n');
        sb.append("Owner: ").append(life.ownerName()).append('\n');
        sb.append(String.format("  cash $%d%n", life.ownerCashCents() / 100));
        sb.append(String.format("  reputation %d · grit %d%n",
                life.ownerReputation(), life.ownerGrit()));
        sb.append(String.format("  %s%n", life.ownerThriving() ? "THRIVING" : "grinding"));
        sb.append(String.format("Ticket $%d · Tax %d%%%n",
                life.ticketPriceCents() / 100, life.taxRatePercent()));
        sb.append(String.format("Seats %d/%d sold%n", life.seatsSold(), life.seatsOffered()));
        sb.append(String.format("East rev $%d (prev $%d)%n",
                life.eastRevenueCents() / 100, life.prevEastRevenueCents() / 100));
        sb.append(String.format("Total rev $%d (prev $%d)%n",
                life.totalRevenueCents() / 100, life.prevTotalRevenueCents() / 100));
        sb.append(String.format("Win bar: >$%d East, rising%n",
                life.eastWinThresholdCents() / 100));
        sb.append(life.won() ? ">> WINNING MONTH <<\n\n" : "(not a winning month)\n\n");
        sb.append("Travelers (Citizens):\n");
        for (String line : life.travelerLines()) {
            sb.append("  ").append(line).append('\n');
        }
        economyLabel.setText(sb.toString());
    }

    private void togglePause() {
        paused = !paused;
        if (pauseBtn != null) {
            pauseBtn.setSelected(paused);
        }
        message(paused ? "Paused." : "Running.");
    }

    private void autoFor(int state) {
        runtime.autoAssist();
    }

    private void buyGateFeedback() {
        message(runtime.buyGate() ? "Opened a new gate." : "Can't buy a gate right now.");
    }

    private void buyRunwayFeedback() {
        message(runtime.buyRunway() ? "Opened a new runway." : "Can't buy a runway right now.");
    }

    private void message(String m) {
        messageLabel.setText(m);
    }

    // --- sprite interpolation ---
    private void updateSprites(GameSnapshot snap) {
        java.util.Set<Integer> live = new java.util.HashSet<>();
        for (GameSnapshot.PlaneView p : snap.planes) {
            live.add(p.id);
            double tx = p.posX * W;
            double ty = p.posY * FIELD_H;
            double[] s = sprites.get(p.id);
            if (s == null) {
                sprites.put(p.id, new double[]{tx, ty});
            } else {
                s[0] += (tx - s[0]) * 0.18;
                s[1] += (ty - s[1]) * 0.18;
            }
        }
        sprites.keySet().removeIf(id -> !live.contains(id));
    }

    // --- rendering ---
    private void render(GameSnapshot snap) {
        GraphicsContext g = canvas.getGraphicsContext2D();

        // Sky gradient.
        LinearGradient sky = new LinearGradient(0, 0, 0, 1, true, null,
                new Stop(0, Color.web("#16233f")),
                new Stop(1, Color.web("#223a5e")));
        g.setFill(sky);
        g.fillRect(0, 0, W, FIELD_H);

        // Apron / tarmac.
        g.setFill(Color.web("#2b2f3a"));
        g.fillRect(0, FIELD_H * 0.62, W, FIELD_H * 0.38);

        drawRunways(g, snap);
        drawGates(g, snap);
        drawPlanes(g, snap);

        if (snap.gameOver) {
            g.setFill(Color.color(0, 0, 0, 0.6));
            g.fillRect(0, 0, W, FIELD_H);
            g.setFill(Color.web("#ff6b6b"));
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 40));
            g.fillText("AIRPORT CLOSED", W / 2 - 170, FIELD_H / 2);
            g.setFont(Font.font("Consolas", 18));
            g.setFill(Color.web("#e8f0ff"));
            g.fillText("Served " + snap.served + "  ·  Lost " + snap.lost
                    + "  ·  Click Restart", W / 2 - 170, FIELD_H / 2 + 36);
        }
    }

    private void drawRunways(GraphicsContext g, GameSnapshot snap) {
        double y = FIELD_H * 0.80;
        for (int i = 0; i < LocalGameModel.MAX_RUNWAYS; i++) {
            double x = 60 + i * 300;
            boolean open = i < snap.openRunways;
            g.setFill(open ? Color.web("#3b3f4a") : Color.web("#1a1d24"));
            g.fillRoundRect(x, y, 260, 26, 8, 8);
            // centreline dashes
            if (open) {
                g.setStroke(Color.web("#d9c04a"));
                g.setLineWidth(2);
                for (double dx = x + 12; dx < x + 248; dx += 26) {
                    g.strokeLine(dx, y + 13, dx + 12, y + 13);
                }
            }
            g.setFill(open ? Color.web("#8fb0d8") : Color.web("#4a4f5a"));
            g.setFont(Font.font("Consolas", 11));
            g.fillText(open ? ("RWY " + (i + 1)) : "locked", x + 6, y - 4);
        }
    }

    private void drawGates(GraphicsContext g, GameSnapshot snap) {
        for (int i = 0; i < LocalGameModel.MAX_GATES; i++) {
            int idx = i + 1;
            double gx = (0.6 + (idx % 4) * 0.09) * W;
            double gy = (0.3 + (idx / 4) * 0.18) * FIELD_H;
            boolean open = idx <= snap.openGates;
            g.setFill(open ? Color.web("#2f6f4f") : Color.web("#20242e"));
            g.fillRoundRect(gx - 14, gy - 10, 28, 20, 5, 5);
            g.setFill(open ? Color.web("#bfe8cf") : Color.web("#3a3f4a"));
            g.setFont(Font.font("Consolas", 9));
            g.fillText("G" + idx, gx - 8, gy + 4);
        }
    }

    private void drawPlanes(GraphicsContext g, GameSnapshot snap) {
        for (GameSnapshot.PlaneView p : snap.planes) {
            double[] s = sprites.get(p.id);
            if (s == null) {
                continue;
            }
            double x = s[0];
            double y = s[1];
            double size = 9 + p.sizeClass * 4;

            Color body = switch (p.state) {
                case GameSnapshot.INBOUND -> Color.web("#9bc4ff");
                case GameSnapshot.LANDING, GameSnapshot.DEPARTING -> Color.web("#ffd98f");
                case GameSnapshot.AT_GATE -> Color.web("#aee6b4");
                default -> Color.web("#c9d4e6");
            };
            // Low patience -> redder.
            double pf = p.patienceFraction();
            if (pf < 0.4) {
                body = body.interpolate(Color.web("#ff6b6b"), (0.4 - pf) / 0.4);
            }

            // Simple plane glyph: fuselage + wings.
            g.setFill(body);
            g.fillOval(x - size, y - size / 3, size * 2, size * 0.66);
            g.fillPolygon(
                    new double[]{x - 2, x + 2, x},
                    new double[]{y - size / 3, y - size / 3, y - size}, 3);
            g.fillRect(x - 1, y - size / 6, 2, size * 0.9);

            // Callsign + patience bar.
            g.setFill(Color.web("#dce6f5"));
            g.setFont(Font.font("Consolas", 9));
            g.fillText(p.flight, x - size, y - size - 3);
            g.setFill(Color.web("#30405a"));
            g.fillRect(x - size, y + size * 0.5, size * 2, 3);
            g.setFill(pf < 0.4 ? Color.web("#ff6b6b") : Color.web("#5fd38a"));
            g.fillRect(x - size, y + size * 0.5, size * 2 * pf, 3);
        }
    }

    private void updateHud(GameSnapshot snap) {
        hud.setText(String.format(
                "CASH $%-6d   REP %-4d   SERVED %-4d   LOST %-4d   GATES %d/%d   RWY %d/%d   PLANES %-3d   t=%d",
                snap.cash, snap.reputation, snap.served, snap.lost,
                snap.openGates, LocalGameModel.MAX_GATES,
                snap.openRunways, LocalGameModel.MAX_RUNWAYS,
                snap.planeCount, snap.tick));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
