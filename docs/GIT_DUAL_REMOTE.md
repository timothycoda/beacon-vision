# Git: Beacon + Elenii (dual remote)

One codebase, two GitHub repos. Same `main` branch on both.

| Remote   | Repository |
|----------|------------|
| `origin` | https://github.com/timothycoda/beacon-vision.git |
| `elenii` | https://github.com/timothycoda/elenii-vision.git |

## Daily workflow

```bash
# Commit on main as usual
git add …
git commit -m "…"

# Push to both (or either)
git push origin main
git push elenii main
```

`main` tracks `elenii/main` after the first `git push -u elenii main`.

## Build variants (same repo)

```bash
./gradlew :app:assembleBeaconDebug   # com.beacon.app
./gradlew :app:assembleEleniiDebug   # com.elenii.app
```

See [ELENII_PRODUCT_FLAVOR.md](./ELENII_PRODUCT_FLAVOR.md).

## Switching back to Beacon work

- Android Studio: **beaconDebug** build variant.
- Edit Beacon-only assets: `app/src/beacon/res/`.
- Shared code: `app/src/main/`, `data/`, `domain/`.

No need to change clones or remotes.
