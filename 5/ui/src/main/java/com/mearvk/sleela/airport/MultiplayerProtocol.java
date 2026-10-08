package com.mearvk.sleela.airport;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Tiny dependency-free line protocol for Airport Tycoon multiplayer.
 *
 * <p>One TCP connection carries HELLO, CMD, STATE, CHAT, INFO and WELCOME
 * records. Human text is Base64 encoded so chat and player names cannot break
 * the line framing. The protocol is intentionally boring and inspectable.</p>
 */
final class MultiplayerProtocol {
    static final int MAX_PLAYERS = 4;
    static final String SHARED = "shared";
    static final String INDIVIDUAL = "individual";

    static String enc(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    static String dec(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    static String hello(String name, String mode) {
        return "HELLO|" + enc(name) + "|" + mode;
    }

    static String command(String action, int id, int arg) {
        return "CMD|" + action + "|" + id + "|" + arg;
    }

    static String chat(String name, String message) {
        return "CHAT|" + enc(name) + "|" + enc(message);
    }

    static String state(String wire) {
        return "STATE|" + enc(wire);
    }

    private MultiplayerProtocol() {}
}
