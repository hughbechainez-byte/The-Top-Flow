# Next UI Brief: 22.4

## Purpose

Make the writing surface feel premium while protecting the editor input hot paths. The editor should look more like a serious songwriting surface without changing typing, keyboard, cursor, rhyme, save, recording, playback, or update behavior.

## 5.3 Implementation Scope

- Add Draft Studio editor chrome.
- Add note-accent rail and compact signal detail.
- Refine title/body field surfaces and spacing.
- Keep `EditText`/`RuledEditText`, text watchers, suggestion scheduling, IME flags, and popup behavior intact.

## Codex Review Notes

- Accepted the presentation-only editor surface changes.
- Replaced the developer-like `EDITOR SURFACE` label with `Draft Studio`.
- Confirmed the implementation does not touch text watchers, suggestion scheduling, keyboard flags, update behavior, storage, or media behavior.
