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
#   make lights  Scoreboard city-lights demo (orange/green/purple, 2-4 mm)
#   make figures The figuring pieces — Original Characters roaming the board
#   make stats / spells / weapons / save   The RPG layer self-demos
#   make bestiary The Legends Bestiary — 24 named legends keyed to depth
#   make grimoire The Grimoire — castings by class + lich pacts / Spelling Duo
#   make creator  Character-creation generator — base allowances + lifetime feats
#   make ui      Open the native SleelaUI text-pane front-end (real window)
#   make test    Run the deterministic self-check Wrapper
#   make all     game + test
#   make help    This message

SLEELA ?= sleela
GAME_SRC := game/MoriaDungeon.sleela
LIGHTS_SRC := game/CityLights.sleela
TEST_SRC := game/MoriaTest.sleela
UI_SRC   := ui-sleela/MoriaUI.sleela
# The RPG layer: stats (incl. Constitution + Character#), the spellbook (the
# Fireball = 1d28 + 6/level), the armoury (+4 or greater), and Save/Load/Alter.
STATS_SRC   := game/MoriaStats.sleela
SPELLS_SRC  := game/MoriaSpells.sleela
WEAPONS_SRC := game/MoriaWeapons.sleela
SAVE_SRC    := game/MoriaSave.sleela
# The Legends Bestiary: 24 named legends keyed to dungeon depth.
BESTIARY_SRC := game/MoriaBestiary.sleela
# The Grimoire: usable castings by class, lich obedience pacts, the Spelling Duo.
GRIMOIRE_SRC := game/MoriaGrimoire.sleela
# The character-creation generator: base allowances + lifetime feats (the snuff).
CREATOR_SRC := game/MoriaCreator.sleela
RPG_SRC := $(STATS_SRC) $(SPELLS_SRC) $(WEAPONS_SRC) $(SAVE_SRC) $(BESTIARY_SRC) $(GRIMOIRE_SRC) $(CREATOR_SRC)

# The SleelaUI widget toolkit, reused from the Edition 1 vendor drop. These are
# byte-for-byte the upstream mearvk/SLeeLa lib/user-interface sources.
UI_SOURCES := ../1/sources/user-interface

.PHONY: all game run lights figures stats spells weapons save bestiary grimoire creator ui test help

all: game test

game:
	@if command -v $(SLEELA) >/dev/null 2>&1; then \
		echo "Checking Moria Wrappers with Sleelvac..."; \
		$(SLEELA) check $(GAME_SRC) $(LIGHTS_SRC) $(TEST_SRC) $(UI_SRC) $(RPG_SRC) --library $(UI_SOURCES); \
	else \
		echo "SLeeLa toolchain ('$(SLEELA)') not found — skipping .sleela check."; \
		echo "Install SLeeLa and set SLEELA=/path/to/sleela to enable it."; \
	fi

# Headless self-descent: prints the final map, HUD, and chronicle.
run:
	$(SLEELA) run $(GAME_SRC)

# The scoreboard city-lights demo (great=orange, excellent=green, player=purple).
lights:
	$(SLEELA) run $(LIGHTS_SRC)

# The figuring-pieces read: the headless self-descent shows the roster of
# Original Characters and their positions on the board (same as `make run`).
figures:
	$(SLEELA) run $(GAME_SRC)

# The RPG-layer self-demos.
stats:
	$(SLEELA) run $(STATS_SRC)

spells:
	$(SLEELA) run $(SPELLS_SRC)

weapons:
	$(SLEELA) run $(WEAPONS_SRC)

save:
	$(SLEELA) run $(SAVE_SRC)

# The Legends Bestiary: 24 named legends keyed to dungeon depth.
bestiary:
	$(SLEELA) run $(BESTIARY_SRC)

# The Grimoire: usable castings by class + the lich obedience pact / duo demo.
grimoire:
	$(SLEELA) run $(GRIMOIRE_SRC)

# The character-creation generator: base allowances + lifetime feats (snuff).
creator:
	$(SLEELA) run $(CREATOR_SRC)

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
	@echo "  make lights Scoreboard city-lights demo (orange/green/purple, 2-4 mm)"
	@echo "  make figures The figuring pieces — Original Characters roaming the board"
	@echo "  make stats   The six ability scores (incl. Constitution + Character#)"
	@echo "  make spells  The spellbook — the Fireball (1d28+6/level) + thousands named"
	@echo "  make weapons The armoury — thousands named, many +4 or greater"
	@echo "  make save    Save / Load / Alter demo (+ the leveling ledger)"
	@echo "  make bestiary The Legends Bestiary — 24 named legends keyed to depth"
	@echo "  make grimoire The Grimoire — castings by class + lich pacts / Spelling Duo"
	@echo "  make creator  Character-creation generator — base allowances + lifetime feats"
	@echo "  make ui     Open the native SleelaUI text-pane front-end"
	@echo "  make test   Run the deterministic self-check Wrapper"
	@echo "  make all    game + test"
