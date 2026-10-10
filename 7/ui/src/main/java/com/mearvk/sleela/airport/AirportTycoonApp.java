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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Airport Tycoon &mdash; the Sleela UI, rendered in the unified green-phosphor
 * look shared by every edition.
 *
 * <p>All colour comes from {@link Phosphor} and all geometry from
 * {@link Phosphor.Metrics}, so Editions 1&ndash;8 are visually one machine: a
 * dark CRT field, a six-step green intensity ladder, and a single amber alert
 * tint for danger. The only thing that changes from 1 to 8 is how many systems
 * of the {@link EditionGimmicks} ladder are lit on the same panel.
 *
 * <p>The edition is read from the {@code at.edition} system property (default 8)
 * so the one JavaFX host renders any edition. The rules still live in
 * {@code game/AirportTycoon.sleela} / its Java mirror; this class only draws and
 * forwards intents.
 */
public final class AirportTycoonApp extends Application {

    private static final double W = 960;
    private static final double H = 680;
    private static final double FIELD_H = 440;

    private final int edition = readEdition();
    private final SleelaRuntime runtime = new LocalGameModel(edition);
    private final EditionGimmicks gimmicks = new EditionGimmicks(edition);
    private final LifeEconomy life = new LifeEconomy();

    private final java.util.Map<Integer, double[]> sprites = new java.util.HashMap<>();

    private Canvas canvas;
    private Label hud;
    private Label backendLabel;
    private Label messageLabel;
    private Label ladderLabel;
    private ToggleButton pauseBtn;

    private long lastTickNanos = 0;
    private static final long TICK_NANOS = 100_000_000L; // 10 Hz
    private static final int TICKS_PER_MONTH = 60;
    private int cashAtMonthStart = 0;
    private boolean paused = false;

    private static int readEdition() {
        try {
            int e = Integer.getInteger("at.edition", EditionConfig.EDITION);
            return Math.max(EditionGimmicks.MIN_EDITION,
                    Math.min(EditionGimmicks.MAX_EDITION, e));
        } catch (RuntimeException ex) {
            return EditionConfig.EDITION;
        }
    }

    private static Color c(int rgb) {
        return Color.rgb(Phosphor.red(rgb), Phosphor.green(rgb), Phosphor.blue(rgb));
    }

    @Override
    public void start(Stage stage) {
        runtime.reset();
        LocalGameModel model = (LocalGameModel) runtime;

        canvas = new Canvas(W, FIELD_H);

        hud = phosphorLabel(Phosphor.PEAK, 15, true);
        backendLabel = phosphorLabel(Phosphor.LABEL, 11, false);
        backendLabel.setText("backend: " + runtime.backendName()
                + "  ·  edition " + edition + " — " + gimmicks.codename());
        messageLabel = phosphorLabel(Phosphor.ACTIVE, 12, false);
        messageLabel.setText("Clear the most urgent plane. Space=pause, A=auto"
                + editionHints(model) + ".");
        ladderLabel = phosphorLabel(Phosphor.LABEL, 11, false);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + Phosphor.GROUND_WEB + ";");
        root.setTop(buildHeader());
        root.setCenter(canvas);
        root.setRight(buildLadderPanel());
        root.setBottom(buildControls(model));

        cashAtMonthStart = runtime.snapshot().cash;

        Scene scene = new Scene(root, W, H);
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case SPACE -> togglePause();
                case A -> runtime.autoAssist();
                case G -> buyGateFeedback();
                case R -> buyRunwayFeedback();
                case D -> deIceFeedback(model);
                case M -> maintainFeedback(model);
                case B -> bookFeedback(model);
                case T -> unlockTerminalFeedback(model);
                default -> { /* ignore */ }
            }
        });

        stage.setTitle("Airport Tycoon — Edition " + edition + " · " + gimmicks.codename());
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
                render(snap, model);
                updateHud(snap);
                updateLadder(model);
            }
        };
        loop.start();
    }

    private Label phosphorLabel(int rgb, int size, boolean bold) {
        Label l = new Label();
        l.setFont(Font.font("Consolas", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        l.setTextFill(c(rgb));
        return l;
    }

    private String editionHints(LocalGameModel m) {
        StringBuilder sb = new StringBuilder();
        if (m.weather() != null) {
            sb.append(", D=de-ice");
        }
        if (m.fleet() != null) {
            sb.append(", M=maintain");
        }
        if (m.network() != null) {
            sb.append(", B=book");
        }
        if (m.terminals() != null) {
            sb.append(", T=terminal");
        }
        return sb.toString();
    }

    private Region buildHeader() {
        VBox box = new VBox(2, hud, backendLabel);
        box.setPadding(new Insets(10, 14, 8, 14));
        box.setStyle("-fx-background-color: " + Phosphor.web(Phosphor.GROUND) + ";"
                + "-fx-border-color: " + Phosphor.FRAME_WEB + "; -fx-border-width: 0 0 1 0;");
        return box;
    }

    private Region buildControls(LocalGameModel m) {
        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.getChildren().add(btn("Auto (A)", e -> runtime.autoAssist()));

        pauseBtn = new ToggleButton("Pause (Space)");
        stylePhosphor(pauseBtn);
        pauseBtn.setOnAction(e -> togglePause());
        buttons.getChildren().add(pauseBtn);

        buttons.getChildren().add(btn("Buy gate (G)", e -> buyGateFeedback()));
        buttons.getChildren().add(btn("Buy runway (R)", e -> buyRunwayFeedback()));

        // Edition-specific controls appear only when their system is lit.
        if (m.weather() != null) {
            buttons.getChildren().add(btn("De-ice (D)", e -> deIceFeedback(m)));
        }
        if (m.fleet() != null) {
            buttons.getChildren().add(btn("Maintain (M)", e -> maintainFeedback(m)));
        }
        if (m.network() != null) {
            buttons.getChildren().add(btn("Book +5 (B)", e -> bookFeedback(m)));
        }
        if (m.terminals() != null) {
            buttons.getChildren().add(btn("Terminal (T)", e -> unlockTerminalFeedback(m)));
        }

        buttons.getChildren().add(btn("Restart", e -> {
            runtime.reset();
            life.setup();
            sprites.clear();
            cashAtMonthStart = runtime.snapshot().cash;
            message("New shift started.");
        }));

        VBox box = new VBox(6, buttons, messageLabel);
        box.setPadding(new Insets(10, 14, 12, 14));
        box.setStyle("-fx-background-color: " + Phosphor.web(Phosphor.GROUND) + ";"
                + "-fx-border-color: " + Phosphor.FRAME_WEB + "; -fx-border-width: 1 0 0 0;");
        VBox.setVgrow(box, Priority.NEVER);
        return box;
    }

    private Button btn(String text, javafx.event.EventHandler<javafx.event.ActionEvent> h) {
        Button b = new Button(text);
        stylePhosphor(b);
        b.setOnAction(h);
        return b;
    }

    private void stylePhosphor(javafx.scene.control.ButtonBase b) {
        b.setStyle("-fx-background-color: " + Phosphor.INACTIVE_WEB + ";"
                + "-fx-text-fill: " + Phosphor.PEAK_WEB + ";"
                + "-fx-border-color: " + Phosphor.FRAME_WEB + ";"
                + "-fx-border-radius: 4; -fx-background-radius: 4;"
                + "-fx-font-family: Consolas;");
    }

    /** The right-hand panel: the ordered mastery ladder for this edition. */
    private Region buildLadderPanel() {
        Label title = phosphorLabel(Phosphor.PEAK, 12, true);
        title.setText("MASTERY LADDER — EDITION " + edition);
        Label sub = phosphorLabel(Phosphor.LABEL, 10, false);
        sub.setText(gimmicks.codename() + "\n" + gimmicks.epithet()
                + "\nholding " + gimmicks.depth() + " system(s)");

        VBox box = new VBox(8, title, sub, ladderLabel);
        box.setPadding(new Insets(12));
        box.setPrefWidth(300);
        box.setStyle("-fx-background-color: " + Phosphor.web(Phosphor.GROUND) + ";"
                + "-fx-border-color: " + Phosphor.FRAME_WEB + "; -fx-border-width: 0 0 0 1;");
        return box;
    }

    private void maybeAdvanceMonth() {
        GameSnapshot snap = runtime.snapshot();
        if (snap.tick > 0 && snap.tick % TICKS_PER_MONTH == 0) {
            int fareIncomeCents = Math.max(0, (snap.cash - cashAtMonthStart)) * 100;
            life.liveAMonth(fareIncomeCents);
            cashAtMonthStart = snap.cash;
            message(String.format("Month %d closed — %d/%d seats, owner $%d (rep %d).",
                    life.month(), life.seatsSold(), life.seatsOffered(),
                    life.ownerCashCents() / 100, life.ownerReputation()));
        }
    }

    private void updateLadder(LocalGameModel m) {
        StringBuilder sb = new StringBuilder();
        for (EditionGimmicks.System s : gimmicks.systems()) {
            String marker = (s.introducedIn() == edition) ? "▸ NEW " : "  ✓   ";
            sb.append(marker).append(s.introducedIn()).append(". ")
                    .append(s.title).append('\n')
                    .append("        ").append(s.masteryVerb).append('\n');
        }
        sb.append('\n');
        // Live readouts for each active system, from the model's status line.
        for (String line : m.systemsStatus().split("\n")) {
            String pretty = prettyStatus(line);
            if (pretty != null) {
                sb.append(pretty).append('\n');
            }
        }
        ladderLabel.setText(sb.toString());
    }

    private String prettyStatus(String line) {
        if (line.startsWith("WEATHER|")) {
            return "WX   : phase " + field(line, "phase");
        }
        if (line.startsWith("FLEET|")) {
            return "FLEET: rating " + field(line, "rating")
                    + (Boolean.parseBoolean(field(line, "review")) ? " (REVIEW)" : "");
        }
        if (line.startsWith("NETWORK|")) {
            return "BOOK : " + field(line, "book") + " reserved";
        }
        if (line.startsWith("TERMINALS|")) {
            return "TERM : " + field(line, "open") + " open, imbal " + field(line, "imbalance");
        }
        if (line.startsWith("POLICY|")) {
            return "AUTO : " + field(line, "delegated") + "/4 delegated";
        }
        if (line.startsWith("LIVE|")) {
            return "LIVE : score " + field(line, "score");
        }
        return null;
    }

    private static String field(String line, String key) {
        for (String kv : line.split("\\|")) {
            int eq = kv.indexOf('=');
            if (eq > 0 && kv.substring(0, eq).equals(key)) {
                return kv.substring(eq + 1);
            }
        }
        return "?";
    }

    private void togglePause() {
        paused = !paused;
        if (pauseBtn != null) {
            pauseBtn.setSelected(paused);
        }
        message(paused ? "Paused." : "Running.");
    }

    private void buyGateFeedback() {
        message(runtime.buyGate() ? "Opened a new gate." : "Can't buy a gate right now.");
    }

    private void buyRunwayFeedback() {
        message(runtime.buyRunway() ? "Opened a new runway." : "Can't buy a runway right now.");
    }

    private void deIceFeedback(LocalGameModel m) {
        boolean any = false;
        for (int i = 1; i <= LocalGameModel.MAX_RUNWAYS; i++) {
            if (m.deIce(i)) {
                any = true;
            }
        }
        message(any ? "De-iced the runways." : "No runway needs de-icing.");
    }

    private void maintainFeedback(LocalGameModel m) {
        message(m.scheduleMaintenance() ? "Maintenance Engineer serviced the fleet."
                : "Maintenance unavailable.");
    }

    private void bookFeedback(LocalGameModel m) {
        message(m.book(5) ? "Booked 5 reservations ahead." : "Booking unavailable.");
    }

    private void unlockTerminalFeedback(LocalGameModel m) {
        message(m.unlockTerminal() ? "Unlocked another terminal."
                : "No more terminals to unlock.");
    }

    private void message(String m) {
        messageLabel.setText(m);
    }

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

    // --- rendering: everything in the one green-phosphor palette ------------
    private void render(GameSnapshot snap, LocalGameModel m) {
        GraphicsContext g = canvas.getGraphicsContext2D();

        // CRT ground.
        g.setFill(c(Phosphor.GROUND));
        g.fillRect(0, 0, W, FIELD_H);

        drawPhosphorGrid(g);
        drawWeatherBand(g, m);          // Ed. 3+
        drawRunways(g, snap, m);
        drawGates(g, snap);
        drawTerminalBar(g, m);          // Ed. 6+
        drawPlanes(g, snap);

        if (snap.gameOver) {
            g.setFill(Color.color(0, 0, 0, 0.6));
            g.fillRect(0, 0, W, FIELD_H);
            g.setFill(c(Phosphor.ALERT));
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 40));
            g.fillText("AIRPORT CLOSED", W / 2 - 170, FIELD_H / 2);
            g.setFont(Font.font("Consolas", 18));
            g.setFill(c(Phosphor.PEAK));
            g.fillText("Served " + snap.served + "  ·  Lost " + snap.lost
                    + "  ·  Click Restart", W / 2 - 170, FIELD_H / 2 + 36);
        }
    }

    /** The faint scanline/grid that gives the whole series its CRT identity. */
    private void drawPhosphorGrid(GraphicsContext g) {
        g.setStroke(c(Phosphor.INACTIVE));
        g.setLineWidth(1);
        int u = Phosphor.Metrics.UNIT;
        for (double x = 0; x <= W; x += u * 2) {
            g.strokeLine(x, 0, x, FIELD_H);
        }
        for (double y = 0; y <= FIELD_H; y += u * 2) {
            g.strokeLine(0, y, W, y);
        }
    }

    private void drawWeatherBand(GraphicsContext g, LocalGameModel m) {
        if (m.weather() == null) {
            return;
        }
        int phase = m.weather().phase();
        if (phase == GimmickSystems.Weather.CLEAR) {
            return;
        }
        // A drifting band across the top: brighter + wider as the storm builds.
        double h = phase == GimmickSystems.Weather.STORM ? FIELD_H * 0.5 : FIELD_H * 0.28;
        int tint = phase == GimmickSystems.Weather.STORM ? Phosphor.ALERT : Phosphor.FRAME;
        g.setFill(Color.rgb(Phosphor.red(tint), Phosphor.green(tint), Phosphor.blue(tint), 0.18));
        g.fillRect(0, 0, W, h);
        g.setFill(c(phase == GimmickSystems.Weather.STORM ? Phosphor.ALERT : Phosphor.LABEL));
        g.setFont(Font.font("Consolas", 11));
        g.fillText(phase == GimmickSystems.Weather.STORM ? "STORM — RUNWAYS ICED" : "FRONT INBOUND",
                12, 16);
    }

    private void drawRunways(GraphicsContext g, GameSnapshot snap, LocalGameModel m) {
        double y = FIELD_H * 0.80;
        int rad = Phosphor.Metrics.RADIUS;
        for (int i = 0; i < LocalGameModel.MAX_RUNWAYS; i++) {
            double x = 60 + i * 300;
            boolean open = i < snap.openRunways;
            boolean iced = m.weather() != null && m.weather().iced(i + 1);
            int base = !open ? Phosphor.INACTIVE : (iced ? Phosphor.ALERT : Phosphor.FRAME);
            g.setFill(c(base));
            g.fillRoundRect(x, y, 260, 26, rad, rad);
            if (open && !iced) {
                g.setStroke(c(Phosphor.ACTIVE));
                g.setLineWidth(2);
                for (double dx = x + 12; dx < x + 248; dx += 26) {
                    g.strokeLine(dx, y + 13, dx + 12, y + 13);
                }
            }
            g.setFill(c(open ? (iced ? Phosphor.ALERT : Phosphor.LABEL) : Phosphor.INACTIVE));
            g.setFont(Font.font("Consolas", 11));
            g.fillText(!open ? "locked" : (iced ? ("RWY " + (i + 1) + " ICED") : ("RWY " + (i + 1))),
                    x + 6, y - 4);
        }
    }

    private void drawGates(GraphicsContext g, GameSnapshot snap) {
        int rad = Phosphor.Metrics.RADIUS;
        for (int i = 0; i < LocalGameModel.MAX_GATES; i++) {
            int idx = i + 1;
            double gx = (0.6 + (idx % 4) * 0.09) * W;
            double gy = (0.3 + (idx / 4) * 0.18) * FIELD_H;
            boolean open = idx <= snap.openGates;
            g.setFill(c(open ? Phosphor.FRAME : Phosphor.INACTIVE));
            g.fillRoundRect(gx - 14, gy - 10, 28, 20, rad, rad);
            g.setFill(c(open ? Phosphor.PEAK : Phosphor.INACTIVE));
            g.setFont(Font.font("Consolas", 9));
            g.fillText("G" + idx, gx - 8, gy + 4);
        }
    }

    private void drawTerminalBar(GraphicsContext g, LocalGameModel m) {
        if (m.terminals() == null) {
            return;
        }
        GimmickSystems.Terminals t = m.terminals();
        double x = 12;
        double y = FIELD_H - 18;
        g.setFont(Font.font("Consolas", 10));
        for (int i = 0; i < t.capacity(); i++) {
            boolean open = i < t.open();
            g.setFill(c(open ? Phosphor.ACTIVE : Phosphor.INACTIVE));
            g.fillRoundRect(x + i * 22, y, 18, 12, 3, 3);
        }
        g.setFill(c(Phosphor.LABEL));
        g.fillText("TERMINALS " + t.open() + "/" + t.capacity(), x, y - 4);
    }

    private void drawPlanes(GraphicsContext g, GameSnapshot snap) {
        for (GameSnapshot.PlaneView p : snap.planes) {
            double[] s = sprites.get(p.id);
            if (s == null) {
                continue;
            }
            double x = s[0];
            double y = s[1];
            double size = Phosphor.Metrics.planeHalfLength(p.sizeClass) + 3;

            // One monochrome body colour, fading to amber as patience runs out.
            int tint = Phosphor.healthTint(p.patienceFraction());
            g.setFill(c(tint));

            // Orthogonal glyph: nose always points along +X (toward departure).
            g.fillOval(x - size, y - size / 3, size * 2, size * 0.66);
            g.fillPolygon(
                    new double[]{x + size, x + size, x + size * 1.4},
                    new double[]{y - size / 3, y + size / 3, y}, 3);
            g.fillRect(x - size * 0.4, y - size / 6, size * 0.8, size * 0.9);

            g.setFill(c(Phosphor.LABEL));
            g.setFont(Font.font("Consolas", 9));
            g.fillText(p.flight, x - size, y - size - 3);
            g.setFill(c(Phosphor.INACTIVE));
            g.fillRect(x - size, y + size * 0.5, size * 2, 3);
            g.setFill(c(tint));
            g.fillRect(x - size, y + size * 0.5, size * 2 * p.patienceFraction(), 3);
        }
    }

    private void updateHud(GameSnapshot snap) {
        hud.setText(String.format(
                "CASH $%-6d  REP %-4d  FARE %d%%  STREAK %-3d  SERVED %-4d  LOST %-4d  "
                        + "GATES %d/%d  RWY %d/%d  PLANES %-3d  t=%d",
                snap.cash, snap.reputation, snap.servicePremiumPct, snap.streak,
                snap.served, snap.lost,
                snap.openGates, LocalGameModel.MAX_GATES,
                snap.openRunways, LocalGameModel.MAX_RUNWAYS,
                snap.planeCount, snap.tick));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
