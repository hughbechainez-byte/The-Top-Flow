# Coding Rules

## Brobro Development Framework

- Preserve the working app first.
- Inspect the relevant files before changing code.
- Make small, reversible changes.
- Keep each task focused on the stated goal.
- Verify the touched behavior when practical.
- Summaries should be concise and useful for the next Codex session.

## Conservative Coding Policy

- Do not rewrite the app.
- Do not refactor unrelated code.
- Do not make speculative cleanup changes.
- Preserve the current architecture unless Dave approves a larger change.
- Ask before major architectural, storage, dependency, or release-process changes.
- Avoid broad scans of generated files, build folders, release artifacts, keystores, and unrelated assets.

## Cost-Conscious Codex Usage

- Treat compute and context as limited.
- Read only the files needed for the task.
- Prefer targeted searches over whole-project exploration.
- Keep explanations brief.
- Record durable project facts in `PROJECT_STATUS.md` and `HANDOFF.md` instead of relying on chat history.

## Android Project Rules

- Keep changes scoped to the Android app module unless the task requires build or release edits.
- Be careful around signing files, release APKs, update manifests, and local SDK/tooling files.
- Do not change app identity, package name, versioning, permissions, or update URLs without explicit approval.
