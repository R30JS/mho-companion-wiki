# MHO Companion Wiki

Offline-first fan companion for Monster Hunter Outlanders-style hunt data.
Kotlin + Jetpack Compose Material 3, single-activity MVVM/Clean Architecture.

- **Package:** `com.mho.companion`
- **Min SDK:** 26 · **Target/Compile SDK:** 35 · **JDK:** 21 · **Gradle:** 8.9 · **AGP:** 8.7.3 · **Kotlin:** 2.0.21 · **Compose BOM:** 2024.12.01

## Features

- **Dark OLED glassmorphism theme** (`#0D1117` background, `#161B22` cards, 1dp borders)
- **Element accents:** Fire `#FF5722`, Water `#03A9F4`, Thunder `#FFC107`, Ice `#00E5FF`, Dragon `#9C27B0`
- **4 tabs:** Hub (codes, reset countdown, checklist, team synergy), Builds/Adventurers (filters, weapon deep-link to crafting, custom loadouts), Bestiary (hitzones, Hunt Mode keep-screen-on, material wishlist), Guides (accordion + ailment/recipe cheat sheet)
- **Search bar + filter chips pinned above bottom navigation**
- **Star ratings, Sever/Blunt/Ranged badges, colored hitzone indicators, haptics on copy/check**
- **Settings:** JSON backup/restore via system file picker, overlay bubble service (SYSTEM_ALERT_WINDOW permission), module sync status, local data reset, about
- **Offline-first:** six bundled JSON modules always available; optional remote version sync via Ktor; DataStore + kotlinx.serialization for user data
- **Deep-link style navigation:** `build/{element}/{weaponId}`, `adventurer/{id}`, `monster/{id}`, `hunt/{id}`

## Project layout

```
mho-companion-wiki/
├── app/
│   ├── src/main/assets/              # version.json, codes.json, builds.json,
│   │                               # characters.json, monsters.json, guides.json
│   ├── src/main/java/com/mho/companion/
│   │   ├── AppContainer.kt           # DI-ish container: repos, http client
│   │   ├── MainActivity.kt           # edge-to-edge Compose activity
│   │   ├── MhoApp.kt                 # Application + content load
│   │   ├── data/                     # DataStore, repos, backup manager
│   │   ├── domain/                   # models, enums, TeamSynergy, ResetSchedule
│   │   ├── service/                  # OverlayService (plain Views bubble)
│   │   └── ui/                       # theme, components, screens, ViewModels
│   └── build.gradle.kts
├── .github/workflows/build-apk.yml   # CI: JDK 21, Android SDK 35, assembleDebug
├── tools/validate_assets.py          # JSON asset cross-validation
├── gradlew, gradle-wrapper.*         # Gradle 8.9 wrapper
└── README.md
```

## Local checks

No local JDK/Android SDK is required to push, but you can validate the bundled assets with Python 3:

```bash
python3 tools/validate_assets.py
```

Expected output:

```
VALIDATION OK
  modules: version.json, codes.json, builds.json, characters.json, monsters.json, guides.json
  builds: 40, adventurers: 14, monsters: 7
  codes: 4, guide sections: 6, ailments: 8, recipes: 6
```

## CI / release

1. Repo: https://github.com/R30JS/mho-companion-wiki (branch `master`).
2. GitHub Actions runs `./gradlew assembleDebug --no-daemon` with JDK 21 + Android SDK 35 on every push.
3. On success, open https://github.com/R30JS/mho-companion-wiki/actions, download the `mho-companion-wiki-debug-apk` artifact, unzip it, and sideload `app-debug.apk` on a device/emulator (API 26+).

Install the APK:

```bash
adb install app-debug.apk
```

Overlay bubble requires the system **Draw over other apps** permission, granted from Settings > Field Kit > Floating hunt overlay.

## Data notes

- Daily reset is assumed at 05:00 local time; weekly reset Monday 05:00.
- Codes, builds, drops, and guide text are fan-made community references, not official data. Verify rewards in-game.
