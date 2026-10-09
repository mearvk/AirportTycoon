# Airport Tycoon — Unified Build Dispatcher
# ==========================================
# Standard project build entry point for Editions 1-8 and Moria.
#
# Build output is kept under /build. Individual edition Makefiles remain
# authoritative for their game-specific targets.

SHELL := /bin/sh

EDITIONS := 1 2 3 4 5 6 7 8
BUILD_DIR := build

.PHONY: autocheck all build prepare game check editions moria clean help

all: build game check

build: prepare

autocheck:
	@sh ./scripts/autocheck-toolchain.sh

prepare:
	@mkdir -p $(BUILD_DIR)
	@for n in $(EDITIONS); do mkdir -p $(BUILD_DIR)/$$n; done
	@mkdir -p $(BUILD_DIR)/dungeons-of-moria
	@echo "Airport Tycoon build tree prepared under /$(BUILD_DIR)/"

game: prepare autocheck
	@for n in $(EDITIONS); do echo "== Edition $$n =="; $(MAKE) -C $$n game; done
	@echo "== Moria =="
	@$(MAKE) -C dungeons-of-moria game

check: prepare autocheck
	@for n in $(EDITIONS); do echo "== Edition $n checks =="; $(MAKE) -C $n check; done
	@echo "Edition checks requested; individual Makefiles remain authoritative."

editions: prepare
	@for n in $(EDITIONS); do $(MAKE) -C $$n build-dir; done

moria: prepare
	@$(MAKE) -C dungeons-of-moria build-dir

clean:
	@rm -rf $(BUILD_DIR)
	@for n in $(EDITIONS); do $(MAKE) -C $$n clean || true; done
	@echo "Build output and edition UI targets cleaned."

help:
	@echo "Airport Tycoon — Unified Build"
	@echo "  make build     Prepare /build and per-edition build directories"
	@echo "  make game      Check Editions 1-8 and Moria with SLeeLa"
	@echo "  make check     Run edition checks where available"
	@echo "  make editions  Prepare Edition 1-8 build directories"
	@echo "  make moria     Prepare Moria build directory"
	@echo "  make all       Prepare + game checks"
	@echo "  make clean     Remove generated /build and UI target output"
