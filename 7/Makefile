# Airport Tycoon — Business Edition (SLeeLa)
# ==========================================
# Top-level build dispatcher, in the spirit of the SLeeLa root Makefile:
# product-specific build systems remain authoritative; this only enters them.
#
#   make game     Type/parse-check the .sleela Wrappers with Sleelvac (if present)
#   make run      Headless self-play of the game Wrapper (needs SLeeLa toolchain)
#   make run-life     Headless run of the business/life layer (Character + Citizen)
#   make ui-sleela    Open the native SleelaUI front-end (real window)
#   make terminal     Author Terminal self-demo (SLeeLa toolchain)
#   make terminal-run Interactive text terminal (reads real stdin)
#   make desk         Business Desk self-demo (casino + investment, Level-5)
#   make test         Run the SLeeLa self-check Wrapper (needs SLeeLa toolchain)
#   make ui     Build the JavaFX Sleela UI (needs Maven + JavaFX)
#   make ui-run Launch the JavaFX game window
#   make check  Build + unit-test the UI's game-logic mirror (no JavaFX needed)
#   make all    game + ui + check
#   make clean  Remove UI build output

SLEELA ?= sleela
GAME_SRC := game/AirportTycoon.sleela
LIFE_SRC := game/AirportTycoonLife.sleela
AUTHOR_SRC := game/AuthorPath.sleela
TEST_SRC := game/AirportTycoonTest.sleela
UI_SLEELA_SRC := ui-sleela/AirportTycoonUI.sleela
TERMINAL_SRC := terminal/AuthorTerminal.sleela
MEDIA_SRC := media/EventsCenter.sleela media/Reporter.sleela
DESK_SRC := game/BusinessDesk.sleela
# Imported SLeeLa library sources the Wrappers resolve against:
#   character/citizen -> the business/life layer
#   gameplay          -> the three-move turn system (help index / ask help / new desire)
#   user-interface    -> the native SleelaUI front-end
#   os                -> the Author Terminal's OS functionality
SOURCES := sources/character sources/citizen sources/gameplay
UI_SOURCES := sources/user-interface
OS_SOURCES := sources/os

.PHONY: all game run run-life author ui-sleela terminal terminal-run media desk test ui ui-run check clean help

all: game check ui

game:
	@if command -v $(SLEELA) >/dev/null 2>&1; then \
		echo "Checking Wrappers + imported /sources with Sleelvac..."; \
		$(SLEELA) check $(GAME_SRC) $(LIFE_SRC) $(AUTHOR_SRC) $(TEST_SRC) \
			$(UI_SLEELA_SRC) $(TERMINAL_SRC) $(MEDIA_SRC) $(DESK_SRC) \
			--library $(SOURCES) $(UI_SOURCES) $(OS_SOURCES); \
	else \
		echo "SLeeLa toolchain ('$(SLEELA)') not found — skipping .sleela check."; \
		echo "Install SLeeLa and set SLEELA=/path/to/sleela to enable it."; \
	fi

run:
	$(SLEELA) run $(GAME_SRC)

# Run the business/life layer headless (resolves Character/Citizen from /sources)
run-life:
	$(SLEELA) run $(LIFE_SRC) --library $(SOURCES)

# Prove the author path wins in exactly 1001 moves, every time.
author:
	$(SLEELA) run $(AUTHOR_SRC)

# Open the native SleelaUI front-end (real X11/Cocoa/Win32 window).
ui-sleela:
	$(SLEELA) run $(UI_SLEELA_SRC) --library $(UI_SOURCES)

# Run the Author Terminal's scripted self-demo via the SLeeLa toolchain.
terminal:
	$(SLEELA) run $(TERMINAL_SRC) --library $(OS_SOURCES)

# Run the media center (300) and the Reporter (220) self-demos.
media:
	$(SLEELA) run media/EventsCenter.sleela
	$(SLEELA) run media/Reporter.sleela

# Run the Business Desk self-demo (casino + investment, Level-5 assimilation).
# Falls back to the Java mirror's main() when the SLeeLa toolchain is absent.
desk:
	@if command -v $(SLEELA) >/dev/null 2>&1; then \
		$(SLEELA) run $(DESK_SRC); \
	else \
		echo "SLeeLa toolchain not found — running the Java mirror demo:"; \
		mkdir -p ui/target/desk && \
		javac -d ui/target/desk \
		  ui/src/main/java/com/mearvk/sleela/airport/BusinessDesk.java && \
		java -cp ui/target/desk com.mearvk.sleela.airport.BusinessDesk; \
	fi

# Launch the INTERACTIVE text terminal (reads real stdin via the Java host).
terminal-run:
	cd ui && mvn -q -DskipTests exec:java \
	  -Dexec.mainClass=com.mearvk.sleela.airport.AuthorTerminal 2>/dev/null || \
	( mkdir -p ui/target/term && \
	  javac -d ui/target/term \
	    ui/src/main/java/com/mearvk/sleela/airport/AuthorTerminal.java && \
	  java -cp ui/target/term com.mearvk.sleela.airport.AuthorTerminal )

test:
	$(SLEELA) run $(TEST_SRC)

ui:
	cd ui && mvn -q clean package

ui-run:
	cd ui && mvn -q javafx:run

# Build + run the pure-Java game-logic tests. No JavaFX download required.
check:
	cd ui && mvn -q -DskipTests=false test || \
	( echo "Maven/JavaFX unavailable — running the mirror tests directly:"; \
	  mkdir -p target/classes && \
	  javac -d target/classes \
	    src/main/java/com/mearvk/sleela/airport/GameSnapshot.java \
	    src/main/java/com/mearvk/sleela/airport/SleelaRuntime.java \
	    src/main/java/com/mearvk/sleela/airport/LocalGameModel.java \
	    src/main/java/com/mearvk/sleela/airport/SleelaProcessRuntime.java \
	    src/main/java/com/mearvk/sleela/airport/LifeEconomy.java \
	    src/main/java/com/mearvk/sleela/airport/AuthorPath.java \
	    src/main/java/com/mearvk/sleela/airport/AuthorTerminal.java \
	    src/main/java/com/mearvk/sleela/airport/MediaCenter.java \
	    src/main/java/com/mearvk/sleela/airport/BusinessDesk.java \
	    src/test/java/com/mearvk/sleela/airport/LocalGameModelTest.java && \
	  java -cp target/classes com.mearvk.sleela.airport.LocalGameModelTest )

clean:
	cd ui && mvn -q clean || true
	rm -rf ui/target

help:
	@echo "Airport Tycoon — Business Edition (SLeeLa)"
	@echo "  make game     Type/parse-check the .sleela Wrappers (Sleelvac)"
	@echo "  make run      Headless self-play (SLeeLa toolchain)"
	@echo "  make run-life  Headless business/life layer (Character + Citizen)"
	@echo "  make author    Prove the author path wins in 1001 moves, every time"
	@echo "  make ui-sleela Open the native SleelaUI front-end (real window)"
	@echo "  make terminal     Author Terminal self-demo (SLeeLa toolchain)"
	@echo "  make terminal-run Interactive text terminal (reads real stdin)"
	@echo "  make media        Events center (300) + Reporter (220) self-demos"
	@echo "  make desk         Business Desk self-demo (casino + investment)"
	@echo "  make test      Run the SLeeLa self-check Wrapper"
	@echo "  make ui      Build the JavaFX Sleela UI"
	@echo "  make ui-run  Launch the JavaFX game window"
	@echo "  make check   Build + unit-test the game-logic mirror (no JavaFX)"
	@echo "  make all     game + check + ui"
	@echo "  make clean   Remove UI build output"
