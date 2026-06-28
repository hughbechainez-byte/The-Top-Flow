# Decisions

## 2026-06-28

- Decision: Use repository files under `project_sync/` as the sole synchronization source for Desktop Codex and WSL Codex CLI.
- Reason: Two agents need shared state without relying on chat history.
- Alternative rejected: Using conversation history as coordination memory.

## 2026-06-28

- Decision: Desktop GPT-5.5 owns architecture, performance, rhyme engine, UI architecture, and large refactors.
- Reason: Keeps high-impact technical direction in one lane.
- Alternative rejected: Allowing both agents to redesign architecture independently.

## 2026-06-28

- Decision: WSL GPT-5.3 owns implementation support, XML/resources, documentation, builds, tests, commits, and release preparation.
- Reason: Separates building/release work from architectural direction.
- Alternative rejected: Duplicating build and implementation tasks across both agents.
