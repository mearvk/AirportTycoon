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
BUILD_DIR := ../build/dungeons-of-moria
GAME_SRC := game/MoriaDungeon.sleela
LIGHTS_SRC := game/CityLights.sleela
TEST_SRC := game/MoriaTest.sleela
UI_SRC   := ui-sleela/MoriaUI.sleela
# The presentation helpers beside MoriaUI (kept in the Moria tree so the
# vendored toolkit stays byte-for-byte with upstream):
#   SLImageFile          — the file loader: loads a real image from a path.
#   SLFontEffect         — the light/glow emitter descriptor (down-directional).
#   MoriaTitleLogo       — the D&D mark as a cool title logo (uses SLImageFile).
#   MoriaThrobber        — the thin full-width white/yellow downward light bar.
#   MoriaCharacterSelect — the choose/create-a-hero GUI startup step.
UI_LOADER_SRC := ui-sleela/SLImageFile.sleela
UI_FX_SRC     := ui-sleela/SLFontEffect.sleela
UI_LOGO_SRC   := ui-sleela/MoriaTitleLogo.sleela
UI_THROB_SRC  := ui-sleela/MoriaThrobber.sleela
UI_SELECT_SRC := ui-sleela/MoriaCharacterSelect.sleela
# The SPRITE front-end: a re-pointable image tile (SLSprite), the moving-picture
# gameboard (MoriaSpriteBoard), and the sprite window front-end (MoriaSpriteUI).
UI_SPRITE_WIDGET_SRC := ui-sleela/SLSprite.sleela
UI_SPRITE_BOARD_SRC  := ui-sleela/MoriaSpriteBoard.sleela
UI_SPRITE_UI_SRC     := ui-sleela/MoriaSpriteUI.sleela
# All the Moria-side UI helpers, in dependency-friendly order (loaders/effects
# first, then the widgets that use them).
UI_EXTRA_SRC  := $(UI_LOADER_SRC) $(UI_FX_SRC) $(UI_LOGO_SRC) $(UI_THROB_SRC) $(UI_SELECT_SRC)
# The sprite-UI sources (plus the shared helpers it reuses).
UI_SPRITE_SRC := $(UI_SPRITE_WIDGET_SRC) $(UI_SPRITE_BOARD_SRC) $(UI_SPRITE_UI_SRC) \
                 $(UI_LOADER_SRC) $(UI_FX_SRC) $(UI_LOGO_SRC) $(UI_THROB_SRC) $(UI_SELECT_SRC)
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
ANIM_SRC    := game/MoriaAnimation.sleela
CATALOG_SRC := game/MoriaDungeonCatalog.sleela
RPG_SRC := $(STATS_SRC) $(SPELLS_SRC) $(WEAPONS_SRC) $(SAVE_SRC) $(BESTIARY_SRC) $(GRIMOIRE_SRC) $(CREATOR_SRC) $(ANIM_SRC) $(CATALOG_SRC)

# The SleelaUI widget toolkit, reused from the Edition 1 vendor drop. These are
# byte-for-byte the upstream mearvk/SLeeLa lib/user-interface sources.
UI_SOURCES := ../1/sources/user-interface

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
# self-contained, $(SLEELA_ENV) below DISCOVERS the SLeeLa root at build time
# and exports the vars for the sleela invocation — so the environment is set at
# the time of Make, not left for the user to locate. Any value already exported
# is honored and never overridden.
#
# $(SLEELA_ENV) is a shell prelude prepended (with `&&`) to each sleela call.
# A valid SLEELA_HOME is any directory holding BOTH the manifest and the verify
# tool. Search order: an exported SLEELA_HOME; the dir two levels above the
# resolved `sleela` binary (…/bin/sleela -> root); then standard install roots
# and a sibling SLeeLa checkout.
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

.PHONY: build-dir all game run lights figures stats spells weapons save bestiary grimoire creator animation catalog sprites updown-sprites tiles ui sprite-ui test help

all: game test

# Standardized repository build location.
build-dir:
	@mkdir -p $(BUILD_DIR)

# Sleelvac resolves cross-file types in command-line order, so a file that
# USES a type must be listed AFTER the file that DECLARES it. $(RPG_SRC) is
# checked first because it declares shared structs (e.g. `struct Legend` in
# MoriaBestiary) that the dungeon/UI Wrappers reference.
game:
	@if command -v $(SLEELA) >/dev/null 2>&1; then \
		echo "Checking Moria Wrappers with Sleelvac..."; \
		$(SLEELA_ENV) && \
		$(SLEELA) check $(RPG_SRC) $(GAME_SRC) $(LIGHTS_SRC) $(TEST_SRC) $(UI_SRC) $(UI_EXTRA_SRC) $(UI_SPRITE_WIDGET_SRC) $(UI_SPRITE_BOARD_SRC) $(UI_SPRITE_UI_SRC) --library $(UI_SOURCES); \
	else \
		echo "SLeeLa toolchain ('$(SLEELA)') not found — skipping .sleela check."; \
		echo "Install SLeeLa and set SLEELA=/path/to/sleela to enable it."; \
	fi

# Headless self-descent: prints the final map, HUD, and chronicle.
run:
	@$(SLEELA_ENV) && $(SLEELA) run $(GAME_SRC)

# The scoreboard city-lights demo (great=orange, excellent=green, player=purple).
lights:
	@$(SLEELA_ENV) && $(SLEELA) run $(LIGHTS_SRC)

# The figuring-pieces read: the headless self-descent shows the roster of
# Original Characters and their positions on the board (same as `make run`).
figures:
	@$(SLEELA_ENV) && $(SLEELA) run $(GAME_SRC)

# The RPG-layer self-demos.
stats:
	@$(SLEELA_ENV) && $(SLEELA) run $(STATS_SRC)

spells:
	@$(SLEELA_ENV) && $(SLEELA) run $(SPELLS_SRC)

weapons:
	@$(SLEELA_ENV) && $(SLEELA) run $(WEAPONS_SRC)

# The walk animation: start/mid/stop frames, the character moving like he walks.
animation:
	@$(SLEELA_ENV) && $(SLEELA) run $(ANIM_SRC)

# The 2000+ dungeon catalog, each sized for authoring AND the Sleela GUI board.
catalog:
	@$(SLEELA_ENV) && $(SLEELA) run $(CATALOG_SRC)

# Autocrop the sprite sheets in images/ into images/<character>/{start,mid,stop}.
# Pure-stdlib Python (no deps); decodes baseline JPEG, trims to the figure, and
# slices the start/mid/stop animation sample. Pass SHEET=... CHARACTER=...
SHEET ?= images/character-animation-sprites-005.jpeg
CHARACTER ?= adventurer
sprites:
	python3 tools/autocrop_sprites.py $(SHEET) $(CHARACTER) --frames 3 --directions --out images

save:
	@$(SLEELA_ENV) && $(SLEELA) run $(SAVE_SRC)

# The Legends Bestiary: 24 named legends keyed to dungeon depth.
bestiary:
	@$(SLEELA_ENV) && $(SLEELA) run $(BESTIARY_SRC)

# The Grimoire: usable castings by class + the lich obedience pact / duo demo.
grimoire:
	@$(SLEELA_ENV) && $(SLEELA) run $(GRIMOIRE_SRC)

# The character-creation generator: base allowances + lifetime feats (snuff).
creator:
	@$(SLEELA_ENV) && $(SLEELA) run $(CREATOR_SRC)

# Open the native SleelaUI text-pane window (X11 / Cocoa / Win32).
ui:
	@$(SLEELA_ENV) && $(SLEELA) run $(UI_SRC) $(UI_EXTRA_SRC) --library $(UI_SOURCES)

# Open the native SleelaUI SPRITE window: the moving-picture gameboard where the
# hero and the Fellowship walk in all four directions (falls back to the text
# pane / headless read with no display or no image bridge).
sprite-ui:
	@$(SLEELA_ENV) && $(SLEELA) run $(UI_SPRITE_SRC) $(GAME_SRC) $(RPG_SRC) --library $(UI_SOURCES)

# Complete the two autocropped heroes (adventurer, warden) with their TOP/DOWN
# directional frames (derived from their real left/right crops) so they move in
# all four directions on the sprite board. Pure-stdlib Python (no deps).
updown-sprites:
	python3 tools/make_updown_sprites.py adventurer warden --out images

# Render the dungeon terrain tiles (floor/wall/door/stairs/treasure/dark/surface)
# the sprite board draws under the characters. Pure-stdlib Python (no deps).
tiles:
	python3 tools/make_tiles.py --out images --size 64

# Deterministic self-check (no display required).
test:
	@$(SLEELA_ENV) && $(SLEELA) run $(TEST_SRC)

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
	@echo "  make sprite-ui Open the SPRITE gameboard — sprites moving in all directions"
	@echo "  make tiles  Render the dungeon terrain tiles for the sprite board"
	@echo "  make updown-sprites  Add the hero's top/down frames (all-direction walk)"
	@echo "  make test   Run the deterministic self-check Wrapper"
	@echo "  make all    game + test"
