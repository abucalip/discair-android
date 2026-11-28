# Architecture

Paths are relative to the repository root.

## App and Data

| Area | Start here |
| --- | --- |
| Application entry point, DI container, and session | `app/src/main/kotlin/app/discair/DiscairApplication.kt`, `MainActivity.kt`, `AppContainer.kt`, `AppViewModel.kt`, `Session.kt` |
| Repositories, caching, credentials, and acquisition hub | `app/src/main/kotlin/app/discair/data/` |
| HTTP clients (Librarian and Subsonic) and cookies | `app/src/main/kotlin/app/discair/net/` |
| Domain models and DTOs | `app/src/main/kotlin/app/discair/model/` |

## Artist Radio and Discovery Mix

- `model/RadioMix.kt`, `data/RadioMixActions.kt`, `data/Repository.kt` (under `app/src/main/kotlin/app/discair/`): preview DTOs, cancellable generation and live server actions. Focused tests: `RadioMixTest`, `RadioMixRepositoryTest`, `RadioMixActionsTest`.
- `ui/discover/RadioMixScreen.kt` and `PlaylistInboxScreen.kt`: artist/tag controls and shared ordered playback/download detail; all artist layouts use `ArtistRadioActions`. Navigation: `ui/Routes.kt`, `ui/MainShell.kt`.

## Playback and Lyrics

| Area | Start here |
| --- | --- |
| Media3 playback service, controller, queue, and scrobbling | `app/src/main/kotlin/app/discair/player/` |
| Crossfade engine | `app/src/main/kotlin/app/discair/player/crossfade/` |
| Equalizer | `app/src/main/kotlin/app/discair/player/eq/` |
| Lyrics fetching, caching, and timeline sync | `app/src/main/kotlin/app/discair/lyrics/` |

## UI

| Area | Start here |
| --- | --- |
| App shell, navigation, routes, and genre views | `app/src/main/kotlin/app/discair/ui/` |
| Home, library, album, and artist screens | `app/src/main/kotlin/app/discair/ui/home/`, `ui/library/`, `ui/album/`, `ui/artist/` |
| Discover, radio, search, downloads, and stats screens | `app/src/main/kotlin/app/discair/ui/discover/`, `ui/radio/`, `ui/search/`, `ui/downloads/`, `ui/stats/` |
| Player UI, settings, shared components, and theme | `app/src/main/kotlin/app/discair/ui/player/`, `ui/settings/`, `ui/common/`, `ui/theme/` |
| Bento theme (`Layout.Bento`) | `ui/theme/BentoMotion.kt`, `ui/common/BentoKit.kt`, `BentoIcons.kt`, `BentoShell.kt`, `ui/home/BentoHome.kt`, `BentoHomeRules.kt`, `ui/album/BentoAlbum.kt`, `BentoAlbumPaths.kt`, `ui/artist/BentoArtist.kt`, `BentoArtistPaths.kt`, `ui/BentoGenre.kt`, `ui/discover/BentoDiscover.kt`, `ui/library/BentoLibrary.kt`, `ui/search/BentoSearch.kt`, `ui/player/BentoNowPlaying.kt`, `BentoEqCrossfade.kt`, `ui/radio/BentoRadio.kt`, `ui/downloads/BentoDownloads.kt`, `ui/stats/BentoStats.kt`, `ui/settings/BentoSettings.kt` |

## Tooling and Contracts

| Area | Start here |
| --- | --- |
| Unit tests | `app/src/test/kotlin/` |
| Recorded payloads and vendored server fixtures | `contract-fixtures/` |
| Design tokens and licenses | `design-tokens/`, `licenses/` |
| Performance probes | `scripts/perf/android_cheap.sh` |
| Client contracts and design notes | `docs/contracts/`, `DESIGN.md` (shared server contracts live in [`abucalip/navi-librarian`](https://github.com/abucalip/navi-librarian/tree/main/docs/contracts)) |
