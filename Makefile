# Airport Tycoon — Business Edition (SLeeLa)
# ==========================================
# Top-level build dispatcher, in the spirit of the SLeeLa root Makefile:
# product-specific build systems remain authoritative; this only enters them.
#
#   make game     Type/parse-check the .sleela Wrappers with Sleelvac (if present)
#   make run      Headless self-play of the game Wrapper (needs SLeeLa toolchain)
#   make run-life Headless run of the business/life layer (Character + Citizen)
#   make test     Run the SLeeLa self-check Wrapper (needs SLeeLa toolchain)
#   make ui     Build the JavaFX Sleela UI (needs Maven + JavaFX)
#   make ui-run Launch the JavaFX game window
#   make check  Build + unit-test the UI's game-logic mirror (no JavaFX needed)
#   make all    game + ui + check
#   make clean  Remove UI build output

SLEELA ?= sleela
GAME_SRC := game/AirportTycoon.sleela
LIFE_SRC := game/AirportTycoonLife.sleela
TEST_SRC := game/AirportTycoonTest.sleela
# Imported SLeeLa library sources the life/business layer resolves against.
SOURCES := sources/character sources/citizen

.PHONY: all game run run-life test ui ui-run check clean help

all: game check ui

game:
	@if command -v $(SLEELA) >/dev/null 2>&1; then \
		echo "Checking Wrappers + imported /sources with Sleelvac..."; \
		$(SLEELA) check $(GAME_SRC) $(LIFE_SRC) $(TEST_SRC) \
			--library $(SOURCES); \
	else \
		echo "SLeeLa toolchain ('$(SLEELA)') not found — skipping .sleela check."; \
		echo "Install SLeeLa and set SLEELA=/path/to/sleela to enable it."; \
	fi

run:
	$(SLEELA) run $(GAME_SRC)

# Run the business/life layer headless (resolves Character/Citizen from /sources)
run-life:
	$(SLEELA) run $(LIFE_SRC) --library $(SOURCES)

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
	    src/test/java/com/mearvk/sleela/airport/LocalGameModelTest.java && \
	  java -cp target/classes com.mearvk.sleela.airport.LocalGameModelTest )

clean:
	cd ui && mvn -q clean || true
	rm -rf ui/target

help:
	@echo "Airport Tycoon — Business Edition (SLeeLa)"
	@echo "  make game     Type/parse-check the .sleela Wrappers (Sleelvac)"
	@echo "  make run      Headless self-play (SLeeLa toolchain)"
	@echo "  make run-life Headless business/life layer (Character + Citizen)"
	@echo "  make test     Run the SLeeLa self-check Wrapper"
	@echo "  make ui      Build the JavaFX Sleela UI"
	@echo "  make ui-run  Launch the JavaFX game window"
	@echo "  make check   Build + unit-test the game-logic mirror (no JavaFX)"
	@echo "  make all     game + check + ui"
	@echo "  make clean   Remove UI build output"
