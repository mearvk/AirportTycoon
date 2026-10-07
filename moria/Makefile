# Moria — Dungeon Crawler (SLeeLa)
# ================================
# Build dispatcher for the Moria text-pane game, in the spirit of the Airport
# Tycoon edition Makefiles. All game logic is SLeeLa source; the GUI is the
# SleelaUI text pane. The SleelaUI toolkit is reused from the Airport Tycoon
# Edition 1 vendor drop (../1/sources/user-interface) rather than duplicated, so
# the one authoritative copy of the widget vocabulary serves both games.
#
#   make game    Type/parse-check the Wrappers with Sleelvac (if present)
#   make run     Headless self-descent of the dungeon model
#   make ui      Open the native SleelaUI text-pane front-end (real window)
#   make test    Run the deterministic self-check Wrapper
#   make all     game + test
#   make help    This message

SLEELA ?= sleela
GAME_SRC := game/MoriaDungeon.sleela
TEST_SRC := game/MoriaTest.sleela
UI_SRC   := ui-sleela/MoriaUI.sleela

# The SleelaUI widget toolkit, reused from the Edition 1 vendor drop. These are
# byte-for-byte the upstream mearvk/SLeeLa lib/user-interface sources.
UI_SOURCES := ../1/sources/user-interface

.PHONY: all game run ui test help

all: game test

game:
	@if command -v $(SLEELA) >/dev/null 2>&1; then \
		echo "Checking Moria Wrappers with Sleelvac..."; \
		$(SLEELA) check $(GAME_SRC) $(TEST_SRC) $(UI_SRC) --library $(UI_SOURCES); \
	else \
		echo "SLeeLa toolchain ('$(SLEELA)') not found — skipping .sleela check."; \
		echo "Install SLeeLa and set SLEELA=/path/to/sleela to enable it."; \
	fi

# Headless self-descent: prints the final map, HUD, and chronicle.
run:
	$(SLEELA) run $(GAME_SRC)

# Open the native SleelaUI text-pane window (X11 / Cocoa / Win32).
ui:
	$(SLEELA) run $(UI_SRC) --library $(UI_SOURCES)

# Deterministic self-check (no display required).
test:
	$(SLEELA) run $(TEST_SRC)

help:
	@echo "Moria — Dungeon Crawler (SLeeLa)"
	@echo "  make game   Type/parse-check the Wrappers (Sleelvac)"
	@echo "  make run    Headless self-descent (prints map + HUD + chronicle)"
	@echo "  make ui     Open the native SleelaUI text-pane front-end"
	@echo "  make test   Run the deterministic self-check Wrapper"
	@echo "  make all    game + test"
