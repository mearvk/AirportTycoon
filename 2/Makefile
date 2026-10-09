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
BUILD_DIR := ../build/2
GAME_SRC := game/AirportTycoon.sleela
LIFE_SRC := game/AirportTycoonLife.sleela
AUTHOR_SRC := game/AuthorPath.sleela
TEST_SRC := game/AirportTycoonTest.sleela
UI_SLEELA_SRC := ui-sleela/AirportTycoonUI.sleela
TERMINAL_SRC := terminal/AuthorTerminal.sleela
MEDIA_SRC := media/EventsCenter.sleela media/Reporter.sleela
ROUTE_ECONOMY_SRC := game/RouteProfitabilityModel.sleela
PACING_SRC := game/PacingEngine.sleela
DESK_OPS_SRC := game/ExecutiveOperationsDesk.sleela
PACING_UI_SRC := ui-sleela/AirportTycoonPacingUI.sleela
FLEET_MAINTENANCE_SRC := game/MaintenanceFleetRegistry.sleela
FLEET_MANAGEMENT_SRC := game/FleetManagementStandards.sleela
FLEET_DESK_SRC := game/FleetManagementDesk.sleela
DESK_SRC := game/BusinessDesk.sleela
# Imported SLeeLa library sources the Wrappers resolve against:
#   character/citizen -> the business/life layer
#   gameplay          -> the three-move turn system (help index / ask help / new desire)
#   user-interface    -> the native SleelaUI front-end
#   os                -> the Author Terminal's OS functionality
SOURCES := sources/character sources/citizen sources/gameplay
UI_SOURCES := sources/user-interface
OS_SOURCES := sources/os

# ---------------------------------------------------------------------------
# SLeeLa environment auto-discovery (fail-closed SHA-256 execution gate).
# ---------------------------------------------------------------------------
# The SLeeLa VM refuses to run unless it can find its verification assets:
#   * SLEELA_SHA256_MANIFEST -> security/sha256-manifest.json  (the trusted hashes)
#   * the verify tool        -> tools/verify-before-execution.py
#   * SLEELA_SHEET           -> SHEET.sheet                     (the object catalog)
# All three live under one SLeeLa root ("SLEELA_HOME"). The installer sets these
# env vars, but a user who runs `make` without sourcing the install profile (or
# who points SLEELA at a bare binary) hits a cryptic gate abort. To make `make`
# self-contained, $(SLEELA_ENV) below DISCOVERS the SLeeLa root at build time and
# exports the vars for the sleela invocation -- so the environment is set at the
# time of Make, not left for the user to locate. Any value already exported is
# honored and never overridden. $(SLEELA_ENV) is a shell prelude prepended (with
# `&&`) to each sleela call. Search order: an exported SLEELA_HOME; the dir two
# levels above the resolved `sleela` binary (.../bin/sleela -> root); then
# standard install roots and a sibling SLeeLa checkout.
SLEELA_ENV = \
	if [ -z "$${SLEELA_SHA256_MANIFEST:-}" ] || [ -z "$${SLEELA_HOME:-}" ]; then \
		_sl_bin=$$(command -v $(SLEELA) 2>/dev/null); \
		_sl_cands="$${SLEELA_HOME:-} $${_sl_bin:+$$(cd "$$(dirname "$$_sl_bin")/.." 2>/dev/null && pwd)} $$HOME/.local/sleela /usr/local/sleela /opt/sleela $(CURDIR)/../../SLeeLa $(CURDIR)/../SLeeLa"; \
		for _sl_h in $$_sl_cands; do \
			[ -n "$$_sl_h" ] || continue; \
			if [ -f "$$_sl_h/security/sha256-manifest.json" ] && [ -f "$$_sl_h/tools/verify-before-execution.py" ]; then \
				SLEELA_HOME="$$_sl_h"; export SLEELA_HOME; \
				: "$${SLEELA_SHA256_MANIFEST:=$$_sl_h/security/sha256-manifest.json}"; export SLEELA_SHA256_MANIFEST; \
				[ -f "$$_sl_h/SHEET.sheet" ] && : "$${SLEELA_SHEET:=$$_sl_h/SHEET.sheet}" && export SLEELA_SHEET; \
				break; \
			fi; \
		done; \
	fi; \
	if [ -z "$${SLEELA_SHA256_MANIFEST:-}" ]; then \
		echo "SLeeLa: could not locate the SHA-256 verification assets." >&2; \
		echo "  Set SLEELA_HOME to your SLeeLa root (the dir with security/sha256-manifest.json" >&2; \
		echo "  and tools/verify-before-execution.py), or run: make $@ SLEELA=/path/to/SLeeLa/bin/SLeeLa" >&2; \
		exit 1; \
	fi


.PHONY: build-dir all game run run-life author ui-sleela terminal terminal-run media desk test ui ui-run check clean help route-economy

all: game check ui

# Standardized repository build location.
build-dir:
	@mkdir -p $(BUILD_DIR)

game:
	@if command -v $(SLEELA) >/dev/null 2>&1; then \
		echo "Checking Wrappers + imported /sources with Sleelvac..."; \
		$(SLEELA_ENV) && \
		$(SLEELA) check $(GAME_SRC) $(LIFE_SRC) $(AUTHOR_SRC) $(TEST_SRC) \
			$(UI_SLEELA_SRC) $(TERMINAL_SRC) $(MEDIA_SRC) $(DESK_SRC) $(ROUTE_ECONOMY_SRC) $(PACING_SRC) $(DESK_OPS_SRC) $(PACING_UI_SRC) \
			--library $(SOURCES) $(UI_SOURCES) $(OS_SOURCES); \
	else \
		echo "SLeeLa toolchain ('$(SLEELA)') not found — skipping .sleela check."; \
		echo "Install SLeeLa and set SLEELA=/path/to/sleela to enable it."; \
	fi

# Run the timeless route-profitability observer as a standalone program load.
pacing:
	@$(SLEELA_ENV) && $(SLEELA) run $(PACING_SRC)

route-economy:
	@$(SLEELA_ENV) && $(SLEELA) run $(ROUTE_ECONOMY_SRC)

fleet-maintenance:
	@$(SLEELA_ENV) && $(SLEELA) run $(FLEET_MAINTENANCE_SRC)

fleet-management:
	@$(SLEELA_ENV) && $(SLEELA) run $(FLEET_MANAGEMENT_SRC) --library game

run:
	@$(SLEELA_ENV) && $(SLEELA) run $(GAME_SRC)

# Run the business/life layer headless (resolves Character/Citizen from /sources)
run-life:
	@$(SLEELA_ENV) && $(SLEELA) run $(LIFE_SRC) --library $(SOURCES)

# Prove the author path wins in exactly 1001 moves, every time.
author:
	@$(SLEELA_ENV) && $(SLEELA) run $(AUTHOR_SRC)

# Open the native SleelaUI front-end (real X11/Cocoa/Win32 window).
ui-sleela:
	@$(SLEELA_ENV) && $(SLEELA) run $(UI_SLEELA_SRC) --library $(UI_SOURCES)

# Run the Author Terminal's scripted self-demo via the SLeeLa toolchain.
terminal:
	@$(SLEELA_ENV) && $(SLEELA) run $(TERMINAL_SRC) --library $(OS_SOURCES)

# Run the media center (300) and the Reporter (220) self-demos.
media:
	@$(SLEELA_ENV) && $(SLEELA) run media/EventsCenter.sleela
	@$(SLEELA_ENV) && $(SLEELA) run media/Reporter.sleela

# Run the Business Desk self-demo (casino + investment, Level-5 assimilation).
# Falls back to the Java mirror's main() when the SLeeLa toolchain is absent.
desk:
	@if command -v $(SLEELA) >/dev/null 2>&1; then \
		$(SLEELA_ENV) && \
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
	@$(SLEELA_ENV) && $(SLEELA) run $(TEST_SRC)

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
	@echo "  make route-economy Run the timeless route-profitability observer"
	@echo "  make fleet-maintenance Run the 23-fleet aircraft maintenance registry"
	@echo "  make fleet-management Run fleet standards and Top Brass management controls"
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
