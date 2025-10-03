SHELL := /bin/bash

# Defaults (override with: make start PORT=9090 PROFILE=dev)
PORT ?= 8080
PROFILE ?=
LOG ?= logs/krag-api.log

.PHONY: help install build start run stop tail test clean

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
	@echo ""
	@echo "Variables: PORT, PROFILE, LOG"

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
	python3 tests/python/test_ingest.py

clean:
	mvn clean