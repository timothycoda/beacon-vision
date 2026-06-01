# TalkBack testing checklist

Install **beaconDebug** or **eleniiDebug**. Enable TalkBack on the device.

## Voice Guide

- [ ] Settings → Accessibility → Voice Guide on
- [ ] Mode **When TalkBack is off**: no duplicate speech with TalkBack enabled
- [ ] Mode **Always**: screen intro plays on Home
- [ ] **Repeat screen intro**: leaving and returning Home speaks intro again
- [ ] Long-press or “More help” on a Guided card speaks extra detail

## Screens (TalkBack reads controls)

- [ ] Home — all main cards have clear names; Emergency warns danger
- [ ] Phone mode — back chip, helper actions
- [ ] Read this, Walking mode, Emergency, Trusted helpers, Settings
- [ ] Settings → Accessibility

## Regression

- [ ] WhatsApp helper buttons still work
- [ ] Hausa labels + MMS voice unchanged
- [ ] No forced glasses pairing after phone mode
