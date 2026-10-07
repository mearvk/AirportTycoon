package com.mearvk.sleela.airport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A runnable text-input terminal with basic OS functionality — the host-side
 * twin of {@code 1/terminal/AuthorTerminal.sleela}.
 *
 * <p>SLeeLa's surface has no stdin primitive (its {@code lib/io} console classes
 * are stubs), so the SLeeLa Wrapper exposes a pure decision core and this Java
 * host supplies the actual terminal: it reads lines from {@code System.in} and
 * prints what the shared logic returns. The response rules here mirror the
 * Wrapper's {@code handleLine()} exactly, and the OS facts come from the real
 * JVM/host ({@code System.getProperty}, {@code InetAddress}, {@code ProcessHandle})
 * — the Java-side analogue of the {@code os*} VM bridge.
 *
 * <p>Behaviour:
 * <ol>
 *   <li>greet with live OS facts;</li>
 *   <li>ask <b>"Are you the Author? (yes/no)"</b>;</li>
 *   <li>if not, ask <b>"What is your Number?"</b> and respond to it;</li>
 *   <li>then accept simple OS commands until {@code exit}.</li>
 * </ol>
 */
public final class AuthorTerminal {

    static final int AUTHOR_NUMBER = 1001;

    enum State { ASK_AUTHOR, ASK_NUMBER, SHELL }

    private State state = State.ASK_AUTHOR;
    private boolean isAuthor = false;

    // --- OS facts (the Java-side analogue of the os* bridge) ---

    static String osPlatform() {
        String os = System.getProperty("os.name", "unknown");
        if (os.startsWith("Windows")) {
            return "Windows";
        }
        if (os.startsWith("Mac") || os.contains("OS X")) {
            return "macOS";
        }
        return "Linux";
    }

    static String osUserName() {
        return System.getProperty("user.name", "unknown");
    }

    static String osHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            String h = System.getenv("HOSTNAME");
            return h != null ? h : "localhost";
        }
    }

    static String osCurrentDir() {
        return System.getProperty("user.dir", ".");
    }

    static String osTempDir() {
        return System.getProperty("java.io.tmpdir", "/tmp");
    }

    static long osProcessId() {
        return ProcessHandle.current().pid();
    }

    // --- prompts ---

    String authorQuestion() {
        return "Are you the Author? (yes/no)";
    }

    String numberQuestion() {
        return "What is your Number?";
    }

    String prompt() {
        return switch (state) {
            case ASK_AUTHOR -> authorQuestion() + " > ";
            case ASK_NUMBER -> numberQuestion() + " > ";
            case SHELL -> "atc$ ";
        };
    }

    String banner() {
        return "Airport Tycoon — Author Terminal\n"
                + "platform=" + osPlatform()
                + " host=" + osHostName()
                + " user=" + osUserName()
                + " cwd=" + osCurrentDir() + "\n"
                + "Type 'help' once inside the shell. " + authorQuestion();
    }

    boolean inShellExit = false;

    /** Take one input line; return the text to print. Advances state. */
    String handleLine(String line) {
        String s = trimLower(line);

        switch (state) {
            case ASK_AUTHOR -> {
                if (isYes(s)) {
                    isAuthor = true;
                    state = State.SHELL;
                    return authorGreeting();
                }
                if (isNo(s)) {
                    state = State.ASK_NUMBER;
                    return numberQuestion();
                }
                state = State.ASK_NUMBER;
                return "I'll take that as a no. " + numberQuestion();
            }
            case ASK_NUMBER -> {
                state = State.SHELL;
                return answerForNumber(parseInt(s));
            }
            default -> {
                return runCommand(s);
            }
        }
    }

    String authorGreeting() {
        return "Welcome back, Author. You knew the clues.\n"
                + "Your path wins in " + AUTHOR_NUMBER + " moves, every time.\n"
                + "The shell is yours. Type 'help'.";
    }

    String answerForNumber(int n) {
        if (n == AUTHOR_NUMBER) {
            isAuthor = true;
            return "That is the author's number. You are the Author after all.\n"
                    + "The path wins in " + AUTHOR_NUMBER + " moves, every time. "
                    + "Type 'help'.";
        }
        if (n >= 1 && n <= 12) {
            String region = districtName(n);
            String tier = isMajorEast(n) ? "a Major Eastern Region" : "a region";
            return "Number " + n + ": the " + region + " Federal Reserve District — "
                    + tier + ". Welcome, traveler. Type 'help'.";
        }
        if (n < 0) {
            return "A number can't be negative here. Starting the shell anyway. "
                    + "Type 'help'.";
        }
        return "Number " + n + " noted. You're not the Author, and that's fine — "
                + "most winners aren't. Type 'help'.";
    }

    String runCommand(String cmd) {
        if (cmd.isEmpty() || cmd.equals("help")) {
            return helpText();
        }
        return switch (cmd) {
            case "whoami" -> osUserName();
            case "host" -> osHostName();
            case "pwd" -> osCurrentDir();
            case "platform" -> osPlatform();
            case "pid" -> Long.toString(osProcessId());
            case "temp" -> osTempDir();
            case "whoami?" -> isAuthor
                    ? "You are the Author (number " + AUTHOR_NUMBER + ")."
                    : "You are a traveler.";
            case "author?" -> isAuthor ? "yes" : "no";
            case "exit", "quit" -> {
                inShellExit = true;
                yield "bye";
            }
            default -> "unknown command: " + cmd + " (try 'help')";
        };
    }

    String helpText() {
        return "commands: help  whoami  host  pwd  platform  pid  temp  "
                + "author?  whoami?  exit";
    }

    boolean isAuthor() {
        return isAuthor;
    }

    State state() {
        return state;
    }

    // --- Fed districts (mirror the imported FederalReserveID) ---
    static boolean isMajorEast(int d) {
        return d == 1 || d == 2 || d == 3 || d == 5 || d == 6;
    }

    static String districtName(int d) {
        return switch (d) {
            case 1 -> "Boston";
            case 2 -> "New York";
            case 3 -> "Philadelphia";
            case 4 -> "Cleveland";
            case 5 -> "Richmond";
            case 6 -> "Atlanta";
            case 7 -> "Chicago";
            case 8 -> "St. Louis";
            case 9 -> "Minneapolis";
            case 10 -> "Kansas City";
            case 11 -> "Dallas";
            case 12 -> "San Francisco";
            default -> "Unknown";
        };
    }

    // --- small helpers ---
    static boolean isYes(String s) {
        return s.equals("yes") || s.equals("y") || s.equals("yeah")
                || s.equals("yep") || s.equals("true");
    }

    static boolean isNo(String s) {
        return s.equals("no") || s.equals("n") || s.equals("nope")
                || s.equals("false");
    }

    static String trimLower(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase();
    }

    /** Non-negative integer from an all-digit token; -1 otherwise. */
    static int parseInt(String s) {
        if (s == null || s.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** The interactive loop: read stdin, print responses, until exit. */
    public void run() {
        System.out.println(banner());
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print(prompt());
            System.out.flush();
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(handleLine(line));
                if (inShellExit) {
                    break;
                }
                System.out.print(prompt());
                System.out.flush();
            }
        } catch (IOException e) {
            System.out.println("terminal closed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new AuthorTerminal().run();
    }
}
