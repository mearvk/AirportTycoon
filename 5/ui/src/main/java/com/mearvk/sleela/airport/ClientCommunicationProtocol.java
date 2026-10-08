package com.mearvk.sleela.airport;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * CCP/1 — dependency-free client-to-client communication protocol.
 *
 * <p>Clients never open peer sockets directly. The authoritative Airport
 * Tycoon server relays authenticated session traffic, keeping the network
 * topology simple and the simulation authoritative. CCP/1 carries presence,
 * peer lists, broadcast chat, direct messages, acknowledgements, and
 * heartbeat traffic alongside the existing game protocol.</p>
 */
final class ClientCommunicationProtocol {
    static final String VERSION = "CCP/1";
    static final String PEERS = "PEERS";
    static final String PRESENCE = "PRESENCE";
    static final String BROADCAST = "BROADCAST";
    static final String DIRECT = "DIRECT";
    static final String ACK = "ACK";
    static final String PING = "PING";
    static final String PONG = "PONG";

    static String enc(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    static String dec(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    static String broadcast(String messageId, String from, String message) {
        return VERSION + "|" + BROADCAST + "|" + enc(messageId) + "|" +
                enc(from) + "|" + enc(message);
    }

    static String direct(String messageId, String from, String to, String message) {
        return VERSION + "|" + DIRECT + "|" + enc(messageId) + "|" +
                enc(from) + "|" + enc(to) + "|" + enc(message);
    }

    static String ack(String messageId) {
        return VERSION + "|" + ACK + "|" + enc(messageId);
    }

    static String presence(String action, String playerId, String name, String mode) {
        return VERSION + "|" + PRESENCE + "|" + action + "|" +
                enc(playerId) + "|" + enc(name) + "|" + enc(mode);
    }

    static String ping(long nonce) {
        return VERSION + "|" + PING + "|" + nonce;
    }

    static String pong(long nonce) {
        return VERSION + "|" + PONG + "|" + nonce;
    }

    private ClientCommunicationProtocol() {}
}
