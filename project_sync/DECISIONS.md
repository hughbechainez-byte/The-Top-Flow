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

## 2026-06-28

- Decision: Fix 21.2 rhyme latency by caching pronunciation/family info, stabilizing fast-row cache keys, bounding expanded scoring, and reducing caret-popup churn inside the existing Java/RhymeEngine architecture.
- Reason: Device logs pointed to repeated rhyme scoring allocations and popup/IME churn, not a need for a full engine replacement.
- Alternative rejected: Starting a larger UI/rhyme rewrite before the freeze path was validated.

## 2026-06-28

- Decision: Fix 21.3 lag by moving expanded rhyme lookup off the UI thread and removing long-note per-keystroke body copies before changing rhyme quality or UI styling.
- Reason: 21.2 script timing passed, so the remaining lag was in real UI paths not covered by lookup-only validation.
- Alternative rejected: More rhyme scoring changes or visual UI work before isolating the device hot path.
