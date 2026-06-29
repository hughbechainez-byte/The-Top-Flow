# Next UI Brief: 22.2

## Purpose

Begin the live Compose-led shell migration without disturbing the working Java app. The goal is an immediate visual foundation upgrade: true OLED black, crisp studio linework, and premium neon detail behind the existing note/editor workflows.

## 5.3 Implementation Scope

- Add a Java-callable Compose bridge returning a `ComposeView`.
- Build a static premium OLED backdrop with crisp neon rails, waveform marks, and grid details.
- Mount the backdrop behind the existing Java UI as the first root child.
- Avoid blurred orbs, bokeh, cheap gradients, and behavior changes.

## Codex Review Notes

- Accepted the bridge and root mounting.
- Adjusted the Compose `Surface` to explicitly render black so it cannot inherit a non-black Material surface color.
- Existing Java workflows remain above the Compose layer.
