package com.mearvk.sleela.airport;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The contract the Sleela UI uses to talk to the game's business logic.
 *
 * <p>This is the "SLeeLa intent &rarr; Java GUI" boundary from
 * {@code gui/INTEGRATION.md} (Path 2), expressed as named operations. The
 * canonical implementation drives the compiled {@code game/AirportTycoon.sleela}
 * Wrapper; a local Java mirror ({@link LocalGameModel}) implements the same
 * contract so the UI and animation run even when the SLeeLa toolchain is not
 * installed.
 *
 * <p>All methods are called on the JavaFX animation thread. Implementations
 * must be fast and non-blocking.
 */
public interface SleelaRuntime {

    /** Build / reset the world to its initial state. */
    void reset();

    /** Advance the simulation by one tick. */
    void step();

    /** Produce the current frame of state for rendering. */
    GameSnapshot snapshot();

    // --- Player intents (named operations) ---

    boolean clearToLand(int planeId);

    boolean assignGate(int planeId, int gateIndex);

    boolean clearForTakeoff(int planeId);

    boolean buyGate();

    boolean buyRunway();

    /** Make a sensible move for the most urgent plane. */
    void autoAssist();

    /** A short human-readable label for which backend is active. */
    String backendName();
}


package com.mearvk.sleela.airport;


/**
 * Client-side SleelaRuntime for the Airport Tycoon multiplayer server.
 * The JavaFX thread remains non-blocking: network I/O happens on a reader
 * thread and intents are short TCP writes.
 */
final class NetworkRuntime implements SleelaRuntime, AutoCloseable {
    private final String host;
    private final int port;
    private final String playerName;
    private final String mode;
    private final ExecutorService reader = Executors.newSingleThreadExecutor();
    private final AtomicReference<GameSnapshot> current =
            new AtomicReference<>(GameSnapshot.parse(null));
    private final CopyOnWriteArrayList<String> chat = new CopyOnWriteArrayList<>();
    private Socket socket;
    private PrintWriter out;
    private volatile String status = "connecting";

    public NetworkRuntime(String host, int port, String playerName, String mode) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
        this.mode = "individual".equals(mode)
                ? "individual" : "shared";
        connect();
    }

    private void connect() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 3000);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            out.println("HELLO|"+mode+"|"+enc(playerName));
            status = "connected";
            reader.submit(this::readLoop);
        } catch (IOException e) {
            status = "offline: " + e.getMessage();
        }
    }

    private void readLoop() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("STATE|")) {
                    current.set(GameSnapshot.parse(dec(line.substring(6))));
                } else if (line.startsWith("CHAT|")) {
                    String[] p = line.split("\|", 3);
                    if (p.length == 3) {
                        chat.add(dec(p[1]) + ": " + dec(p[2]));
                        while (chat.size() > 8) chat.remove(0);
                    }
                } else if (line.startsWith("INFO|")) {
                    chat.add("Server: " + dec(line.substring(5)));
                } else if (line.startsWith("WELCOME|")) {
                    status = "connected/" + mode;
                }
            }
        } catch (IOException e) {
            status = "disconnected";
        }
    }

    private void send(String line) {
        if (out != null) out.println(line);
    }

    private void cmd(String action, int id, int arg) {
        send("CMD|"+action+"|"+id+"|"+arg);
    }

    public void sendChat(String message) {
        if (message == null || message.isBlank()) return;
        send("CHAT|"+enc(playerName)+"|"+enc(message.trim()));
    }

    public String networkStatus() { return status; }

    public String chatText() {
        return String.join("
", chat);
    }

    private static String enc(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String dec(String s) {
        try {
            return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return s;
        }
    }

    @Override public void reset() { cmd("RESET", 0, 0); }
    @Override public void step() { /* server is authoritative */ }
    @Override public GameSnapshot snapshot() { return current.get(); }
    @Override public boolean clearToLand(int planeId) { cmd("LAND", planeId, 0); return true; }
    @Override public boolean assignGate(int planeId, int gateIndex) { cmd("GATE", planeId, gateIndex); return true; }
    @Override public boolean clearForTakeoff(int planeId) { cmd("TAKEOFF", planeId, 0); return true; }
    @Override public boolean buyGate() { cmd("GATE_BUY", 0, 0); return true; }
    @Override public boolean buyRunway() { cmd("RUNWAY_BUY", 0, 0); return true; }
    @Override public void autoAssist() { cmd("AUTO", 0, 0); }
    @Override public String backendName() { return "Network " + mode + " — " + host + ":" + port; }

    @Override public void close() {
        reader.shutdownNow();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
