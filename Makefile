SHELL := /bin/bash

# Defaults (override with: make start PORT=9090 PROFILE=dev)
PORT ?= 8080
FRONT_PORT ?= 5173
FRONT_FALLBACK_PORT ?= 8000
PROFILE ?=
LOG ?= logs/krag-api.log
FRONT_LOG ?= logs/krag-web.log

.PHONY: help install build start run stop tail test clean
.PHONY: start-web stop-web tail-web dev stop-all

## Default target
.DEFAULT_GOAL := help

help:
	@echo "Common targets:"
	@echo "  install    - Build all modules (mvn -DskipTests install)"
	@echo "  build      - Package API module"
	@echo "  start      - Start API via script (background)"
	@echo "  run        - Run API via Maven (foreground)"
	@echo "  stop       - Stop process listening on PORT ($(PORT))"
	@echo "  tail       - Tail API log ($(LOG))"
	@echo "  test       - Run ingestion tests (requires API up)"
	@echo "  clean      - Maven clean"
	@echo "  start-web  - Start Web dev server (5173) or fallback static (8000)"
	@echo "  stop-web   - Stop Web server"
	@echo "  tail-web   - Tail Web log ($(FRONT_LOG))"
	@echo "  dev        - Start API and Web together (background)"
	@echo "  stop-all   - Stop API and Web"
	@echo ""
	@echo "Variables: PORT, PROFILE, LOG, FRONT_PORT, FRONT_FALLBACK_PORT, FRONT_LOG"

install:
	mvn -DskipTests install

build:
	mvn -DskipTests -pl krag-api package

start:
	@echo "Starting KRAG API on port $(PORT) $(if $(PROFILE),with profile '$(PROFILE)',)"
	scripts/start-api.sh --port $(PORT) $(if $(PROFILE),--profile $(PROFILE)) --kill

run:
	@echo "Running KRAG API in foreground on port $(PORT) (set via application.yml)"
	cd krag-api && mvn -DskipTests spring-boot:run

stop:
	@pids=$$(lsof -ti tcp:$(PORT) || true); \
	if [ -n "$$pids" ]; then \
	  echo "Stopping processes on port $(PORT): $$pids"; \
	  kill $$pids || true; \
	  sleep 1; \
	  pids2=$$(lsof -ti tcp:$(PORT) || true); \
	  if [ -n "$$pids2" ]; then echo "Force killing: $$pids2"; kill -9 $$pids2 || true; fi; \
	else \
	  echo "No process found on port $(PORT)"; \
	fi

tail:
	@echo "Tailing $(LOG)"
	test -f $(LOG) || (echo "Log file not found: $(LOG)" && exit 1)
	tail -n 200 -f $(LOG)

test:
	@echo "Running ingestion tests (requires API at http://localhost:$(PORT))"
	@curl -sS "http://localhost:$(PORT)/api/v1/hello" > /dev/null || (echo "API not ready on :$(PORT). Start with 'make start' or 'make run'." && exit 1)
	python3 tests/python/test_ingest.py && python3 tests/python/test_query.py

clean:
	mvn clean

# ===== Web (Vue + Vite) =====
.PHONY: web-install web-dev web-build web-preview

web-install:
	cd krag-web-app && npm install

web-dev:
	cd krag-web-app && npm run dev

web-build:
	cd krag-web-app && npm run build

web-preview:
	cd krag-web-app && npm run preview

# Start Web in background (prefer Vite dev @ 5173; fallback static @ 8000)
start-web:
	@mkdir -p logs .run
	@echo "Starting Web..."
	# Kill existing web ports if occupied
	@wp=$$(lsof -ti tcp:$(FRONT_PORT) || true); if [ -n "$$wp" ]; then echo "Freeing :$(FRONT_PORT) -> $$wp"; kill $$wp || true; sleep 1; wp2=$$(lsof -ti tcp:$(FRONT_PORT) || true); if [ -n "$$wp2" ]; then kill -9 $$wp2 || true; fi; fi
	@fp=$$(lsof -ti tcp:$(FRONT_FALLBACK_PORT) || true); if [ -n "$$fp" ]; then echo "Freeing :$(FRONT_FALLBACK_PORT) -> $$fp"; kill $$fp || true; sleep 1; fp2=$$(lsof -ti tcp:$(FRONT_FALLBACK_PORT) || true); if [ -n "$$fp2" ]; then kill -9 $$fp2 || true; fi; fi
	@if command -v node >/dev/null 2>&1 && command -v npm >/dev/null 2>&1; then \
	  echo "Starting Vite dev server on :$(FRONT_PORT)"; \
	  nohup bash -lc 'cd krag-web-app && npm run dev' >> $(FRONT_LOG) 2>&1 & echo $$! > .run/krag-web.pid; \
	  echo "Web ready at http://localhost:$(FRONT_PORT)/"; \
	else \
	  echo "Node/npm not found. Starting static preview on :$(FRONT_FALLBACK_PORT)"; \
	  nohup python3 -m http.server $(FRONT_FALLBACK_PORT) --directory krag-web >> $(FRONT_LOG) 2>&1 & echo $$! > .run/krag-web.pid; \
	  echo "Web ready at http://localhost:$(FRONT_FALLBACK_PORT)/index.html"; \
	fi

stop-web:
	@pid=$$(cat .run/krag-web.pid 2>/dev/null || true); \
	if [ -n "$$pid" ]; then echo "Stopping Web PID $$pid"; kill $$pid || true; sleep 1; kill -9 $$pid || true; rm -f .run/krag-web.pid; else echo "No Web PID file"; fi; \
	wp=$$(lsof -ti tcp:$(FRONT_PORT) || true); if [ -n "$$wp" ]; then echo "Freeing :$(FRONT_PORT) -> $$wp"; kill $$wp || true; sleep 1; wp2=$$(lsof -ti tcp:$(FRONT_PORT) || true); if [ -n "$$wp2" ]; then kill -9 $$wp2 || true; fi; fi; \
	fp=$$(lsof -ti tcp:$(FRONT_FALLBACK_PORT) || true); if [ -n "$$fp" ]; then echo "Freeing :$(FRONT_FALLBACK_PORT) -> $$fp"; kill $$fp || true; sleep 1; fp2=$$(lsof -ti tcp:$(FRONT_FALLBACK_PORT) || true); if [ -n "$$fp2" ]; then kill -9 $$fp2 || true; fi; fi

tail-web:
	@echo "Tailing $(FRONT_LOG)"
	@test -f $(FRONT_LOG) || (echo "Log file not found: $(FRONT_LOG)" && exit 1)
	@tail -n 200 -f $(FRONT_LOG)

# Start API and Web together
dev:
	@$(MAKE) start
	@$(MAKE) start-web
	@echo "All started: API http://localhost:$(PORT)/, Web (5173 or 8000)"

# Stop API and Web together
stop-all:
	@$(MAKE) stop
	@$(MAKE) stop-web