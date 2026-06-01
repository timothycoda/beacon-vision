# Elenii product flavor

Beacon and Elenii share one codebase with Android product flavors on the `brand` dimension.

## Build variants

| Variant | Application ID | App name |
|---------|----------------|----------|
| `beaconDebug` / `beaconRelease` | `com.beacon.app` | Beacon |
| `eleniiDebug` / `eleniiRelease` | `com.elenii.app` | Elenii |

```bash
./gradlew :app:assembleBeaconDebug
./gradlew :app:assembleEleniiDebug
```

Both APKs can be installed side by side.

## Branding

- **Beacon** — `app/src/beacon/res/` (lime accent, vector launcher, wordmark in top bar text).
- **Elenii** — `app/src/elenii/res/` (sky blue `#38BDF8`, compass logo from `elenii-logo-assets/`, adaptive icon, splash logo).

Source logos: `elenii-logo-assets/el10.png` (launcher + welcome symbol, transparent), `el1.png` (optional header wordmark). Regenerate drawables:

```bash
./scripts/generate-elenii-assets.sh
```

Compose UI uses `MaterialTheme.colorScheme` for accents and flavor `strings.xml` for visible copy (no “Beacon” in Elenii UI).
