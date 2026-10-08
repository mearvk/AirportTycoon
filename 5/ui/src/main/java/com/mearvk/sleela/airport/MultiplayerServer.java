package com.mearvk.sleela.airport;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Airport Tycoon multiplayer server.
 *
 * <p>One server hosts both modes simultaneously:
 * <ul>
 *   <li><b>shared</b> — up to four players operate the same deterministic airport.</li>
 *   <li><b>individual</b> — each player receives a private airport, while all
 *       connected players share the global chat.</li>
 * </ul>
 *
 * <p>The server is authoritative. Clients send named intents; the server
 * advances the world at 10 Hz and broadcasts snapshots. No external libraries
 * or cloud service are required.</p>
 */
public final class MultiplayerServer {
    private static final int DEFAULT_PORT = 47500;
    private final int port;
    private final ServerSocket server;
    private final ScheduledExecutorService clock = Executors.newScheduledThreadPool(2);
    private final ExecutorService clients = Executors.newCachedThreadPool();
    private final Set<Peer> peers = ConcurrentHashMap.newKeySet();
    private final AtomicInteger nextPeerId = new AtomicInteger(1);
    private final LocalGameModel sharedWorld = new LocalGameModel();
    private final Object sharedLock = new Object();

    public MultiplayerServer(int port) throws IOException {
        this.port = port;
        this.server = new ServerSocket(port);
    }

    public void start() {
        clock.scheduleAtFixedRate(() -> tick(), 100, 100, TimeUnit.MILLISECONDS);
        System.out.println("Airport Tycoon multiplayer server listening on port " + port);
        System.out.println("Modes: shared (same world) or individual (private worlds + global chat).");
        while (!server.isClosed()) {
            try {
                Socket socket = server.accept();
                if (peers.size() >= MultiplayerProtocol.MAX_PLAYERS) {
                    try (PrintWriter out = new PrintWriter(
                            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
                        out.println("INFO|" + MultiplayerProtocol.enc("Server is full (maximum 4 players)."));
                    }
                    socket.close();
                    continue;
                }
                Peer peer = new Peer(socket);
                peers.add(peer);
                clients.submit(peer);
            } catch (IOException e) {
                if (!server.isClosed()) {
                    System.err.println("Accept failed: " + e.getMessage());
                }
            }
        }
    }

    private void tick() {
        synchronized (sharedLock) {
            sharedWorld.step();
        }
        for (Peer peer : peers) {
            if (peer.mode.equals(MultiplayerProtocol.INDIVIDUAL)) {
                synchronized (peer.modelLock) {
                    peer.model.step();
                }
            }
            peer.sendState();
        }
    }

    private void broadcastChat(String name, String message) {
        String line = MultiplayerProtocol.chat(name, message);
        for (Peer peer : peers) peer.send(line);
    }

    private void broadcastCcp(String line) {
        for (Peer peer : peers) peer.send(line);
    }

    private void sendPeerList() {
        StringBuilder entries = new StringBuilder();
        for (Peer peer : peers) {
            if (entries.length() > 0) entries.append('\\n');
            entries.append(peer.id).append("=").append(peer.name).append("@").append(peer.mode);
        }
        String line = ClientCommunicationProtocol.peers(entries.toString());
        for (Peer peer : peers) peer.send(line);
    }

    private void remove(Peer peer) {
        peers.remove(peer);
        peer.close();
        broadcastChat("Server", peer.name + " left the airport network.");
        broadcastCcp(ClientCommunicationProtocol.presence("LEAVE", peer.id, peer.name, peer.mode));
        sendPeerList();
    }

    private final class Peer implements Runnable {
        final Socket socket;
        final Object modelLock = new Object();
        final LocalGameModel model = new LocalGameModel();
        BufferedReader in;
        PrintWriter out;
        String id;
        String name = "Player";
        String mode = MultiplayerProtocol.SHARED;
        volatile boolean connected = true;

        Peer(Socket socket) {
            this.socket = socket;
        }

        @Override public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                String first = in.readLine();
                if (first == null || !first.startsWith("HELLO|")) {
                    send("INFO|" + MultiplayerProtocol.enc("HELLO required."));
                    return;
                }
                String[] hello = first.split("\|", 3);
                if (hello.length >= 2) name = safeName(MultiplayerProtocol.dec(hello[1]));
                if (hello.length == 3 && MultiplayerProtocol.INDIVIDUAL.equals(hello[2])) {
                    mode = MultiplayerProtocol.INDIVIDUAL;
                }
                send("WELCOME|" + peers.size() + "|" + mode + "|" + MultiplayerProtocol.MAX_PLAYERS);
                broadcastCcp(ClientCommunicationProtocol.presence("JOIN", id, name, mode));
                sendPeerList();
                broadcastChat("Server", name + " joined (" + mode + ").");
                sendState();

                String line;
                while ((line = in.readLine()) != null) {
                    handle(line);
                }
            } catch (Exception e) {
                if (connected) System.err.println("Player connection ended: " + e.getMessage());
            } finally {
                remove(this);
            }
        }

        private void handle(String line) {
            if (line.startsWith("CCP/1|BROADCAST|")) {
                broadcastCcp(line);
                send(ClientCommunicationProtocol.ack(extractMessageId(line, 2)));
                return;
            }
            if (line.startsWith("CCP/1|DIRECT|")) {
                String[] p = line.split("\\|", 6);
                if (p.length == 6) {
                    String target = ClientCommunicationProtocol.dec(p[4]);
                    for (Peer peer : peers) {
                        if (peer.id.equals(target) || peer.name.equals(target)) {
                            peer.send(line);
                            send(ClientCommunicationProtocol.ack(ClientCommunicationProtocol.dec(p[2])));
                            return;
                        }
                    }
                    send("INFO|" + MultiplayerProtocol.enc("Peer not found: " + target));
                }
                return;
            }
            if (line.startsWith("CCP/1|PING|")) {
                send(ClientCommunicationProtocol.pong(Long.parseLong(line.substring("CCP/1|PING|".length()))));
                return;
            }
            if (line.startsWith("CHAT|")) {
                String[] p = line.split("\|", 3);
                if (p.length == 3) broadcastChat(name, MultiplayerProtocol.dec(p[2]));
                return;
            }
            if (!line.startsWith("CMD|")) return;
            String[] p = line.split("\|", 4);
            if (p.length < 4) return;
            String action = p[1];
            int id = parse(p[2]);
            int arg = parse(p[3]);
            LocalGameModel m = mode.equals(MultiplayerProtocol.SHARED) ? sharedWorld : model;
            synchronized (mode.equals(MultiplayerProtocol.SHARED) ? sharedLock : modelLock) {
                switch (action) {
                    case "LAND" -> m.clearToLand(id);
                    case "GATE" -> m.assignGate(id, arg);
                    case "TAKEOFF" -> m.clearForTakeoff(id);
                    case "GATE_BUY" -> m.buyGate();
                    case "RUNWAY_BUY" -> m.buyRunway();
                    case "AUTO" -> m.autoAssist();
                    case "RESET" -> m.reset();
                    default -> { }
                }
            }
            sendState();
        }

        void sendState() {
            if (!connected || out == null) return;
            LocalGameModel m = mode.equals(MultiplayerProtocol.SHARED) ? sharedWorld : model;
            synchronized (mode.equals(MultiplayerProtocol.SHARED) ? sharedLock : modelLock) {
                send(MultiplayerProtocol.state(m.snapshot().toWire()));
            }
        }

        void send(String line) {
            if (out != null && connected) out.println(line);
        }

        void close() {
            connected = false;
            try { socket.close(); } catch (IOException ignored) {}
        }

        private String extractMessageId(String line, int field) {
            String[] p = line.split("\\|", 6);
            return p.length > field ? ClientCommunicationProtocol.dec(p[field]) : "unknown";
        }

        private int parse(String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
        }

        private String safeName(String n) {
            n = n == null ? "Player" : n.trim();
            if (n.isEmpty()) n = "Player";
            return n.length() > 24 ? n.substring(0, 24) : n;
        }
    }

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        new MultiplayerServer(port).start();
    }
}
