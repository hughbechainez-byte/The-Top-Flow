# Visual Framework — The Top Flow

## Core Principles

- **OLED-first**: True black (`#000000`) is the only background. No gray fills that lighten the canvas.
- **Neon as signal, not decoration**: Color appears only on active rails, accents, selected states, and focus.
- **Zero layout cost while typing**: Backdrop and ambient motion are pure Canvas draws. No recomposition of note content, no blur, no heavy shaders.
- **Single visual language**: One consistent treatment for panels, chips, rails, and buttons across Notes grid, Editor, sheets, and docks.

## Color System

| Role | Value | Usage |
|------|-------|-------|
| OLED Black | `#000000` | Root background |
| Surface | `#0B0F14` | Panels, sheets, rhyme strip |
| Surface Variant | `#151A20` | Secondary containers |
| Primary Neon (Mint) | `#84FFEE` | Active rails, primary accents, selected chips |
| Secondary Neon | `#739BFF` | Secondary rails, supporting accents |
| Amber | `#FFC875` | Secondary emphasis |
| Text Strong | `#E8EAED` | Titles, body |
| Text Soft | `#C3CAD4` | Metadata, captions |

Per-note accent, page, and text colors remain user-controlled.

## Surfaces & Structure

- Floating command surfaces: thin 1 px neon/hairline border, near-black fill, 12–18 dp corner radius.
- Accent rail: fixed 2–3 px neon line on the leading edge of note cards, editor chrome, and active dock items.
- Rhyme strip uses the same surface language as command panels.
- Editor page uses an extremely low-alpha note-color wash (~6 %) over pure black.

## Typography

- Primary: Space Grotesk (bundled)
- Mono: Share Tech Mono
- Pixel: Silkscreen
- System fallbacks only when the note font is set to sans/serif/monospace

## Motion

- Backdrop: two independent slow horizontal neon sweeps + one static soft vertical rail (left edge). Pure draw, 8–11 s cycles.
- UI transitions: short (120–220 ms) eased crossfades and panel reveals. Never compete with typing or rhyme updates.
- Active feedback: neon stroke + slight scale/alpha on dock items and selected chips.
- Editor open: smooth materialize (scale + fade) of the note surface.

## Constraints (non-negotiable)

- No blur, grain, or heavy backdrop filters while the editor is focused.
- No full-screen recomposition on keystroke.
- All ambient animation lives in a single Canvas behind the content.
- Neon remains low-opacity until the element is active or selected.
