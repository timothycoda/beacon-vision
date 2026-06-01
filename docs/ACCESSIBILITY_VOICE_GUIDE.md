# Accessibility Voice Guide (Phase 1)

Optional in-app spoken navigation layered on Android TalkBack. Implemented in `:domain`, `:data`, and `:app`.

## Components

| Type | Location |
|------|----------|
| `AccessibilityVoiceGuide` | Speaks intros, focus hints, errors |
| `AccessibilityPhraseProvider` | Resolves `a11y_phrase_*` strings |
| `AccessibilitySettings` | User preferences (DataStore) |
| `GuidedBentoCard` | Accessible action cards with “More help” |
| `ScreenVoiceIntro` | Composable screen introductions |

## Settings

**Settings → Accessibility**

- Voice Guide on/off
- Mode: Always / Important only / When TalkBack is off (default)
- Speak focused controls, repeat screen intro, haptics

## Phrases

Add English in `app/src/main/res/values/accessibility_phrases.xml`. Keys map to `AccessibilityPhraseKey`. Hausa: add `values-ha/accessibility_phrases.xml` with the same `a11y_phrase_*` names.

## Builds

Works for both `beaconDebug` and `eleniiDebug` (shared code, flavor strings unchanged).
