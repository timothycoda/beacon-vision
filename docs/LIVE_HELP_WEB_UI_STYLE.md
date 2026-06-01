# Live Help — web UI style guide

Helper PWA uses the **webio** webinar layout (top bar, main video, control strip, chat sidebar) with **Elenii sky blue** brand colors (aligned with Android `colors.xml`).

Layout reference: webio session mock; colors match Elenii, not the mock’s green palette.

## Color tokens

| Token | Hex | Use |
|-------|-----|-----|
| Primary (navy) | `#0B4DBA` | Logo, buttons, scene card gradient |
| Primary deep | `#050816` | Video letterbox |
| Primary mid | `#0A3D91` | Hover / gradient end |
| Sky | `#38BDF8` | Focus ring, accents |
| Sky bubble | `#7DD3FC` | Helper chat bubbles |
| Sky soft | `#E0F2FE` | Light accents |
| Background | `#F0F4FA` | Page outside shell |
| Surface | `#FFFFFF` | Main frame |
| Text | `#0A1020` | Headings |
| Text muted | `#5B6B82` | Subtitles |
| Live red | `#E63946` | LIVE dot |

CSS variables live in `live-help/helper-pwa/src/styles/tokens.css`.

## Typography

- **Font:** DM Sans (Google Fonts), fallback system UI
- **Logo:** Bold, forest green, lowercase **elenii**
- **Session title:** 14px semibold, ellipsis when long
- **Subtitle:** 12px muted

## Layout (desktop)

```
┌─────────────────────────────────────────────────────────────┐
│ elenii   Live Help · Room ABC123          [copy] [End]      │
├───────────────────────────────────────┬─────────────────────┤
│                                       │ Chat | Scene        │
│         Remote video (user)           │ ┌ scene card ────┐  │
│         LIVE  00:12:34                │ └────────────────┘  │
│                          [PiP local]  │ chat feed           │
│                                       │ [ message input ]   │
│  Cam  Mic  Share  Rec                 │                     │
└───────────────────────────────────────┴─────────────────────┘
```

- **Shell radius:** 24px (`--radius-shell`)
- **Video radius:** 16px
- **Sidebar width:** 340px (stacks below video on narrow screens)

## Components

| Component | Notes |
|-----------|--------|
| Top bar | Brand + session meta + End session (forest button) |
| Video stage | Full bleed in rounded rect; LIVE pill top-left |
| Control bar | Circular icon buttons + labels (Cam, Mic, Share, Rec) |
| Scene card | Dark forest card — AI summary (poll-card style from mockup) |
| Chat | Grey bubbles (user), mint bubbles (helper), forest system lines |
| Join card | Centered white card on soft green-grey page |

## Accessibility

- Visible focus rings (mint outline)
- `aria-live` on connection status (dev user page)
- Semantic headings on join screen
- Record control disabled — no server-side recording in Phase 2

## Brand note

Elenii Android uses navy/sky (`#0B4DBA`, `#38BDF8`). Live Help web intentionally uses **forest + mint** from the webinar mock so helpers get a familiar “video session” look distinct from the dark mobile app.
