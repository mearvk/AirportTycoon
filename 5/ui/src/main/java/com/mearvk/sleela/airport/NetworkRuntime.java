package com.mearvk.sleela.airport;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client-side SleelaRuntime for the Airport Tycoon multiplayer server.
 * The JavaFX thread remains non-blocking: network I/O happens on a reader
 * thread and intents are short TCP writes.
 */
public final class NetworkRuntime implements SleelaRuntime, AutoCloseable {
    private final String host;
    private final int port;
    private final String playerName;
    private final String mode;
    private final ExecutorService reader = Executors.newSingleThreadExecutor();
    private final AtomicReference<GameSnapshot> current =
            new AtomicReference<>(GameSnapshot.parse(null));
    private final CopyOnWriteArrayList<String> chat = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> peers = new CopyOnWriteArrayList<>();
    private Socket socket;
    private PrintWriter out;
    private volatile String status = "connecting";

    public NetworkRuntime(String host, int port, String playerName, String mode) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
        this.mode = MultiplayerProtocol.INDIVIDUAL.equals(mode)
                ? MultiplayerProtocol.INDIVIDUAL : MultiplayerProtocol.SHARED;
        connect();
    }

    private void connect() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 3000);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            out.println(MultiplayerProtocol.hello(playerName, mode));
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
                    current.set(GameSnapshot.parse(MultiplayerProtocol.dec(line.substring(6))));
                } else if (line.startsWith("CHAT|")) {
                    String[] p = line.split("\|", 3);
                    if (p.length == 3) {
                        chat.add(MultiplayerProtocol.dec(p[1]) + ": " + MultiplayerProtocol.dec(p[2]));
                        while (chat.size() > 8) chat.remove(0);
                    }
                } else if (line.startsWith("CCP/1|PEERS|")) {
                    peers.clear();
                    peers.addAll(java.util.Arrays.asList(ClientCommunicationProtocol.dec(line.substring("CCP/1|PEERS|".length())).split("\\n")));
                } else if (line.startsWith("CCP/1|BROADCAST|")) {
                    String[] p = line.split("\\|", 5);
                    if (p.length == 5) chat.add("[All] " + ClientCommunicationProtocol.dec(p[3]) + ": " + ClientCommunicationProtocol.dec(p[4]));
                } else if (line.startsWith("CCP/1|DIRECT|")) {
                    String[] p = line.split("\\|", 6);
                    if (p.length == 6) chat.add("[Direct] " + ClientCommunicationProtocol.dec(p[3]) + ": " + ClientCommunicationProtocol.dec(p[5]));
                } else if (line.startsWith("CCP/1|PRESENCE|")) {
                    String[] p = line.split("\\|", 6);
                    if (p.length == 6) chat.add("Network: " + ClientCommunicationProtocol.dec(p[4]) + " " + p[2] + ".");
                } else if (line.startsWith("CCP/1|ACK|")) {
                    chat.add("Network: delivered " + ClientCommunicationProtocol.dec(line.substring("CCP/1|ACK|".length())));
                } else if (line.startsWith("CCP/1|PING|")) {
                    send(ClientCommunicationProtocol.pong(Long.parseLong(line.substring("CCP/1|PING|".length()))));
                } else if (line.startsWith("INFO|")) {
                    chat.add("Server: " + MultiplayerProtocol.dec(line.substring(5)));
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
        send(MultiplayerProtocol.command(action, id, arg));
    }

    public void sendChat(String message) {
        if (message == null || message.isBlank()) return;
        send(MultiplayerProtocol.chat(playerName, message.trim()));
    }

    public String networkStatus() { return status; }

    public void sendPeerMessage(String message) {
        if (message == null || message.isBlank()) return;
        String id = playerName + "-" + System.nanoTime();
        send(ClientCommunicationProtocol.broadcast(id, playerName, message.trim()));
    }

    public void sendDirectMessage(String peerId, String message) {
        if (peerId == null || peerId.isBlank() || message == null || message.isBlank()) return;
        String id = playerName + "-" + System.nanoTime();
        send(ClientCommunicationProtocol.direct(id, playerName, peerId.trim(), message.trim()));
    }

    public String peerText() {
        return String.join("\\n", peers);
    }

    public String chatText() {
        return String.join("
", chat);
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
