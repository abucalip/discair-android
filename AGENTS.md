# Discair (Android) — Agent Instructions

This repository (`abucalip/discair-android`) holds the native Android client for Discair.

## Contracts & shared references

- Client contracts: [`docs/contracts/`](docs/contracts/) (`ANDROID_CLIENT_CONTRACT.md`, `ANDROID_CROSSFADE_CONTRACT.md`, `ANDROID_EQ_CONTRACT.md`, `ANDROID_LYRICS_CONTRACT.md`, `ANDROID_PLAYER_CONTRACT.md`)
- Design notes: [`DESIGN.md`](DESIGN.md)
- Recorded payloads and vendored server fixtures: [`contract-fixtures/`](contract-fixtures/README.md) and `design-tokens/`
- Shared server API contracts (`SERVER_V2_CONTRACT.md`, `PLAYLIST_INBOX_CONTRACT.md`, `RADIO_PLAYBACK_CONTRACT.md`, `SEARCH_CONTRACT.md`) live in [`abucalip/navi-librarian`](https://github.com/abucalip/navi-librarian/tree/main/docs/contracts).

## Hard rules

- Call the person directing the project **"the owner"** in docs, commits, PRs, and comments. Never use their name or handle.
- Never print, log, or commit credentials, signing keys, `*.jks`, `keystore.properties`, or `local.properties`.
- Keep domain logic server-side; the Android UI only presents it.
- Measure client performance with `scripts/perf/android_cheap.sh` on a real USB-connected device (no emulators).

## Testing (Gradle + JDK 17)

- **JDK 17 is required:** set `JAVA_HOME=/opt/homebrew/opt/openjdk@17`.
- **Never run the full unit suite.** Run only tests for code you changed:
  ```sh
  JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testDebugUnitTest --tests '<filter>'
  ```
- Report exactly what ran and what did not. Never claim a pass that was not run.

## Change cycle

- Branch from current `main`, make the smallest reviewable change with regression tests, and open a focused PR.
- End the task after pushing and creating/updating the PR; do not wait for or poll CI unless asked.
