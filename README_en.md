# MzDKPlayer - Android TV Local Danmaku Media Player

![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/mzhsy1/MzDKPlayer/total)
![License](https://img.shields.io/badge/license-GPL--3.0-blue)

[中文](README.md) | English

> GitHub https://github.com/mzhsy1/MzDKPlayer | Gitee Mirror https://gitee.com/mzhsy/MzDKPlayer | Website https://mzdkplayer.pages.dev/

> MzDKPlayer is a local music and video player specifically designed for Android TV, supporting danmaku (bullet comments), multiple network protocols, and various audio/video formats.

---

## Table of Contents

- [Features](#features)
- [Format Support](#format-support)
- [App Preview](#app-preview)
- [Quick Start](#quick-start)
  - [Option 1: Download the APK](#option-1-download-the-apk)
  - [Option 2: Build from Source](#option-2-build-from-source)
  - [Common Build Issues](#common-build-issues)
- [Usage Examples](#usage-examples)
- [Remote Control Keys](#remote-control-keys)
- [Technical Architecture](#technical-architecture)
- [Development Guide](#development-guide)
- [Hardware Requirements](#hardware-requirements)
- [Project Status](#project-status)
- [Contributing](#contributing)
- [Disclaimer](#disclaimer)
- [License](#license)

---

## Features

### Core Features

- 🎬 **Video Playback** - Supports various video formats for local and network protocol playback.
- 🎵 **Audio Playback** - Supports various audio formats for local and network protocol playback. Includes lyrics, album cover display, music information, playlists, and other common features.
- 🖼️ **Image Viewer** - Supports various image formats for local and network protocol viewing.
- 🏡 **Media Library** - Includes Movie/TV/Music libraries, fetching information from TMDB, supporting batch addition.
- 🕛 **History** - Playback history for both audio and video.
- 🔍 **Search Function** - Search for movies and TV shows.
- 💬 **Danmaku Function** - Supports Bilibili-style danmaku display and customization.
- ⚙️ **Settings** - Detailed application and playback settings.
- 🌐 **Network Protocol Support**:
  - ✅ SMB protocol (Supported)
  - ✅ FTP protocol (Supported)
  - ✅ WebDAV protocol (Supported; Note: LAN WebDAV services like those from Feiniu NAS may only support HTTP, while public clouds like Aliyun support HTTPS).
  - ✅ NFS protocol (Supported)
  - ✅ HTTP protocol (Supported via NGINX servers)
- 🎚️ **Track Selection** - Supports switching audio, video, and subtitle tracks, playback speed control, and audio software/hardware decoding.
- 🔤 **Subtitle Customization** - Text-based subtitles (ExoPlayer) support custom font, size, color, background color, and bottom padding.
- 🔤 **Subtitle Timing** - Shift subtitles earlier or later (0.5 s steps, up to ±30 s). With the ExoPlayer engine this applies to text subtitles only; graphic subtitles such as PGS are not supported.
- 🧠 **Playback Preference Memory** - Remember the audio track, subtitle track, playback speed and aspect ratio per file, and restore them the next time it is played.
- 📱 **Phone Remote Control** - The app can start a LAN remote page; scan the QR code with your phone to use it as a remote.
- 🌏 **Multi-language UI** - Simplified Chinese / English / Japanese / Traditional Chinese

### 🔊 Advanced Playback: Audio Passthrough

MzDKPlayer supports audio passthrough, allowing raw audio signals (source) to be output directly to an amplifier, soundbar, or TV that supports multi-channel decoding for a cinema-grade listening experience.

* **Path**: `Settings` -> `Audio Settings` -> `Audio Passthrough`
* **Applicability**: **This toggle only affects the VLC playback engine**. The ExoPlayer engine will automatically determine this based on your device, no manual intervention needed.
* **Suggestions**:
  * **Default State**: Recommended to keep it **Off**. ExoPlayer already meets the automatic adaptation needs of most devices.
  * **Prerequisites**: Only enable this if you have an external amplifier or high-end audio decoding device and are certain it supports the audio encoding format (e.g., DTS-HD, TrueHD) of the video being played.
  * **Troubleshooting**: If you encounter **no sound** during playback after enabling, it means your audio device does not support the current video's audio track format (e.g., some TVs do not support TrueHD passthrough). **In this case, please turn off this toggle** to let the player output via PCM through software decoding.

---

## Format Support

### 📺 Video Formats

* **Common Containers**: MP4, MKV, MOV, AVI, WMV, FLV, WebM
* **Blu-ray/Professional Formats**: **ISO (Blu-ray Image)**, **M2TS**, **MTS**, TS, VOB
* **Video Encodings**: H.264 (AVC), **H.265 (HEVC)**, **AV1**, VP9, MPEG-2
* **Feature Support**: 4K/8K UHD playback, HDR10/HLG, Dolby Vision

### 🎵 Audio Formats

* **Lossless/Hi-Fi**: **FLAC**, WAV, ALAC (Apple Lossless)
* **General Formats**: MP3, AAC, OGG, Opus, WMA
* **Cinema-grade Tracks**: **DTS**, **DTS-HD**, **TrueHD**, AC3 (Dolby Digital), E-AC3

### 🖼️ Image Formats

* **Standard Formats**: JPEG (JPG), PNG, WebP, BMP
* **Modern Formats**: HEIC / HEIF
* *Note: Apple Live Photos are currently not supported.*

### 💬 Subtitle Support

* **External Subtitles**: **SRT**, **ASS**, **SSA**, VTT
* **Embedded Subtitles**: MKV Internal, **PGS (Blu-ray Subtitles)**, DVB, Teletext

---

> ⚠️ **Note**: TMDB may require a proxy or Host modification for stable access in some regions. You can also set a mirror under `Settings -> Scraping & Library -> TMDB API Address (Mirror)`.

> 💡 Tip 1: If used frequently, it's recommended to set this player as the default video player in your TV system for a smoother experience.

> 💡 Tip 2: If device performance is insufficient, enabling danmaku while playing 70-80GB Blu-ray videos may cause playback lag.

> 💡 Tip 3: If you encounter issues with the default ExoPlayer engine, you can try switching to the VLC player engine in `Settings` -> `Playback & Video` -> `Default Player Engine`.

---

## App Preview

### Main Interface & File List

![Main Interface Screenshot](screenshots/Screenshot_20260708_114700.webp)
![Main Interface Screenshot](screenshots/Screenshot_20260708_105320.webp)
![Main Interface Screenshot](screenshots/Screenshot_20251116_163213.webp)
![Main Interface Screenshot](screenshots/Screenshot_20251223_174613.webp)
![Main Interface Screenshot](screenshots/Screenshot_20260708_111419.webp)

### Playback Interface & Danmaku

![Video Playback Screenshot](screenshots/Screenshot_20251104_190350.webp)  
![Video Playback Screenshot](screenshots/Screenshot_20251104_190409.webp)
![Audio Playback Screenshot](screenshots/Screenshot_20260126_182132.webp)

### Movie/TV Details Page

![Movie Details Screenshot](screenshots/Screenshot_20251220_112824.webp)
![TV Details Screenshot](screenshots/Screenshot_20251220_112844.webp)

### Settings Page

![Settings Screenshot](screenshots/Screenshot_20260708_105422.webp)

---

## Quick Start

### Option 1: Download the APK

> For users who just want to use the app on their TV — no development environment needed.

1. Open the [Releases](https://github.com/mzhsy1/MzDKPlayer/releases) page and download the latest APK (built automatically by GitHub Actions).
2. Pick the package matching your TV's chipset (**use the universal package if unsure**):

   | Package | Target devices |
   | --- | --- |
   | `arm64-v8a` | Most recent TV boxes / TVs (Amlogic S905X3/S928X, MT9653, etc.) |
   | `armeabi-v7a` | Older low-end boxes (e.g. S905L, 32-bit systems) |
   | `universal` | Largest size, compatible with all the ABIs above |

3. Transfer the APK to your TV and install it (USB drive, app stores, or the TV's built-in file manager).

Installing from a computer via ADB is recommended:

```bash
# Enable "Developer options -> USB debugging / Network debugging" on the TV first
adb connect 192.168.1.100:5555        # replace with your TV's actual IP
adb install -r app-arm64-v8a-release.apk
```

### Option 2: Build from Source

#### 1. Requirements

| Item | Requirement | Notes |
| --- | --- | --- |
| JDK | **17 or higher** | Source and target bytecode are Java 17 |
| Kotlin toolchain | JDK 21 | The project declares `jvmToolchain(21)`; Gradle provisions it automatically if missing |
| Android SDK | **compileSdk 37** | Requires `Android SDK Platform 37` and `Build-Tools` |
| Gradle | 9.7.1 | Use the bundled `gradlew`; no separate installation needed |
| Android Studio | Latest stable | Optional — the command line can build everything |

#### 2. Clone the project

```bash
git clone https://github.com/mzhsy1/MzDKPlayer.git
cd MzDKPlayer
```

#### 3. Configure `local.properties`

Create `local.properties` in the project root (**already covered by `.gitignore`, never committed**) and fill in the SDK path and TMDB API key:

```properties
sdk.dir=D:\\Android\\Sdk
TMDB_API_KEY=your_tmdb_api_key
```

> ⚠️ **`TMDB_API_KEY` is required.** It is injected as `BuildConfig.TMDB_API_KEY`. The project still compiles without it, but TMDB scraping in the media library will not work.
> You can request a free API key at [TMDB developer settings](https://www.themoviedb.org/settings/api), then point the API address at a reachable mirror in the settings page.

#### 4. Build

```bash
# Use gradlew.bat on Windows, ./gradlew on macOS / Linux

# Debug build (debug-signed, installable directly — best for local testing)
./gradlew :app:assembleTvDebug        # TV
./gradlew :app:assemblePhoneDebug     # phone

# Release build (minification and resource shrinking enabled, unsigned)
./gradlew :app:assembleTvRelease

# Compile-only check, the fastest option
./gradlew :app:compileTvDebugKotlin   # TV; swap Tv for Phone for the phone build
```

> The TV and phone apps are two **product flavors** (`tv` / `phone`) of the same module:
> sources live in `app/src/tv/` and `app/src/phone/`, and the shared business layer lives in the separate `:core` module.
> They use different `applicationId`s (`org.mz.mzdkplayer` / `org.mz.mzdkplayer.phone`), so both can be installed on one device.

Output locations:

| Command | Output |
| --- | --- |
| `assembleTvDebug` / `assemblePhoneDebug` | `app/build/outputs/apk/tv/debug/app-tv-debug.apk`, `app/build/outputs/apk/phone/debug/app-phone-debug.apk` |
| `assembleTvRelease` | `app/build/outputs/apk/tv/release/app-tv-<abi>-release.apk`, `app-tv-universal-release.apk` |

> Release builds are **not signed** by default. Sign them with `apksigner` or Android Studio's *Generate Signed Bundle or APK* before distributing.
> ABI splits are enabled (`armeabi-v7a`, `arm64-v8a`, plus a universal APK), so the release directory contains several APKs.

#### 5. Install on your TV

```bash
adb connect 192.168.1.100:5555
adb install -r app/build/outputs/apk/tv/debug/app-tv-debug.apk
```

#### 6. Run the unit tests (to verify your environment)

```bash
./gradlew :core:testDebugUnitTest
```

> All unit tests live with the business layer in `:core`, so a single `:core:testDebugUnitTest` run covers both apps.

### Common Build Issues

| Symptom | Cause and fix |
| --- | --- |
| `Unable to delete directory ... a process has files open` | Android Studio is running and holding `app/build`. Close Studio and retry from the command line — **do not delete `app/build` manually** |
| `TMDB_API_KEY` missing / scraping does not work | `local.properties` is missing or the key name is wrong (do not write `TMDB_KEY`) |
| `SDK location not found` | `sdk.dir` is missing in `local.properties`, or set the SDK path in Android Studio |
| Kotlin toolchain download fails | `jvmToolchain(21)` downloads JDK 21; installing JDK 21 locally lets Gradle reuse it |
| Dependency downloads time out | `settings.gradle.kts` already configures Aliyun/Tencent Cloud mirrors; adjust repository order as needed |
| `fileHashes.lock access denied` | Run `./gradlew --stop` to confirm the daemon exited, then delete `.gradle/9.7.1/fileHashes/fileHashes.lock` |

---

## Usage Examples

### Example 1: Play a local file

1. The app requests storage permission on first launch. On Android 11+, choosing **"Allow management of all files"** gives the best experience (otherwise some directories are not scanned).
2. Home / File Browsing -> pick a category (video / audio / image).
3. Tap a file inside a directory to play it. The player automatically loads a same-named `.xml` danmaku file and same-named subtitles from the same directory.

### Example 2: Add network storage (SMB / FTP / WebDAV / NFS / HTTP)

1. Open **"Network Storage"** from the sidebar and pick the protocol.
2. Tap **"New Connection"** in the top-right corner and fill in the address, username, and password (SMB needs a share name; FTP needs a port).
3. Saved storages appear in the connection list — browse and play them like a local directory.
4. Supported URI forms (useful when troubleshooting):

   | Protocol | Example |
   | --- | --- |
   | SMB | `smb://user:pass@host/share/path` |
   | FTP | `ftp://user:pass@host:21/path` |
   | WebDAV | `https://user:pass@host/path` |
   | NFS | `nfs://host:/export:path` |
   | HTTP | `http://host/path` (NGINX directory listing) |
   | Local | `file:///storage/emulated/0/Movies/a.mkv` |

> All network protocol connections have timeouts and degrade gracefully on failure, so they never hang or crash the app.

### Example 3: Danmaku

- Danmaku files are Bilibili-style `.xml`, placed in the same directory as the video with the **same file name** to be loaded automatically.
- Press the **Up** key on the remote during playback to open danmaku settings and adjust font size, speed, opacity, and display area.

### Example 4: TMDB scraping and local NFO

- Movie / TV detail pages pull data from TMDB and need working network access plus an API key.
- If a same-named `.nfo` file exists in the media directory, enable `Settings -> Scraping & Library -> Prefer local NFO files` to read it offline instead.
- Scraping results are cached in the local database. The player title prefers the scraped name (episodes automatically get `SxxExx` and the year) and falls back to the file name when no scraped record exists.
- Adjust the recursion depth for batch scanning under `Settings -> Scraping & Library -> Batch scan subfolder depth`.

### Example 5: Subtitles

- Press the **Menu** key during playback to bring up the control bar, then open the subtitle menu to switch tracks.
- With `Settings -> Subtitles -> Auto-load same-name subtitles` enabled, the player scans the video's directory and loads matching subtitles automatically.
- Text-based subtitles (rendered by ExoPlayer) support custom font, size, color, background color, and bottom padding; fonts can be picked from a local folder.
- When subtitles are out of sync, use **Subtitle timing** in the playback overlay to shift them in 0.5 s steps (up to ±30 s); `Settings -> Subtitles -> Subtitle timing` edits the very same value. Note that with the ExoPlayer engine this only works for text subtitles — switch to the VLC engine for PGS and other graphic subtitles.

### Example 6: Phone remote control by QR code

1. Open the remote entry in the UI; the app starts a lightweight HTTP service on the LAN and shows a QR code.
2. Connect your phone to the same Wi-Fi and scan the code to use the web page as a remote.

### Example 7: Playlists and history

- Video / audio playback screens support playlists and post-playback actions (e.g. auto-advance to the next episode).
- Playback progress is saved automatically (flushed every 10 seconds and again when leaving the player), so you can resume from **History**.

---

## Remote Control Keys

| Key | Function on the playback screen |
| --- | --- |
| Left / Right | Rewind / Fast forward (long-press for continuous seeking) |
| OK | Pause / Play |
| Menu | Show / hide the control bar (tracks, subtitles, speed, danmaku, etc.) |
| Up | Danmaku settings (configurable) |
| Down | Audio track selection (configurable) |
| Back | Exit playback / collapse the control bar |

> The Up and Down key behaviors can be customized under `Settings -> Remote & Input -> Remote Up Key / Remote Down Key`.

---

## Technical Architecture

### Key Tech Stack

| Category | Choice | Version |
| --- | --- | --- |
| Language | Kotlin | 2.4.20 |
| Build | Gradle / AGP / KSP | 9.7.1 / 9.4.0 / 2.3.9 |
| UI | Jetpack Compose for TV (`tv-foundation` / `tv-material`) + Navigation Compose | Compose BOM 2026.09.00 / Navigation 2.9.8 |
| Playback engines | Media3 ExoPlayer + libVLC (dual engine behind a shared abstraction) | 1.11.1 / 3.7.6 |
| Danmaku | AKDanmaku + libGDX / Ashley | — |
| Database | Room | 2.8.5 |
| Preferences | DataStore Preferences | 1.2.1 |
| Networking | smbj (SMB) / commons-net (FTP) / sardine (WebDAV) / nfs-client (NFS) / OkHttp | — |
| Image loading | Coil 3 | 3.6.2 |
| Built-in services | NanoHTTPD (local proxy + phone remote page), ZXing (QR code) | — |
| Metadata | Retrofit + Gson (TMDB), jaudiotagger (audio tags) | — |

### Module Structure

The project is one business-layer module (`:core`) plus an application module (`:app`) with two product flavors:

```
core/src/main/java/org/mz/mzdkplayer/     # :core — business layer, shared by tv / phone
├── danmaku/          # Danmaku parsing
├── data/
│   ├── api/          # TMDB API (Retrofit)
│   ├── local/        # Room: AppDatabase, MediaCacheEntity, AudioCacheEntity, MediaHistoryEntity
│   ├── model/        # Data models
│   └── repository/   # Repository layer hiding data sources
├── di/               # RepositoryProvider (DAO injection via viewModelWithFactory), AppContext
├── player/
│   ├── core/         # IMzPlayer abstraction, track models, data source factories
│   ├── exo/          # MzExoPlayer implementation
│   └── vlc/          # MzVlcPlayer implementation
├── tool/             # Utilities: protocol data sources, subtitle scanning, time parsing, LAN proxy and remote services
└── viewmodel/        # ViewModels for every screen

app/src/main/java/org/mz/mzdkplayer/
└── MzDkPlayerApplication.kt              # Application shared by both flavors

app/src/tv/java/org/mz/mzdkplayer/        # TV flavor (androidx.tv.material3 only)
├── MainActivity.kt / LaunchScreen.kt     # LEANBACK_LAUNCHER entry point
└── ui/
    ├── MzDKPlayerAPP.kt      # Navigation graph and app shell
    ├── videoplayer/          # Player screen (components/ holds controls, title, overlays)
    ├── audioplayer/          # Audio player screen
    ├── picviewer/            # Image viewer
    ├── screen/               # Pages: filehome / localfile / smbfile / ftp / webdavfile / nfs /
    │                         #        httplink / library / movie / tv / history / search / setting
    └── theme/                # TV theme

app/src/phone/java/org/mz/mzdkplayer/ui/phone/   # Phone flavor (androidx.compose.material3 only)
├── PhoneMainActivity.kt    # LAUNCHER entry point (applicationId = org.mz.mzdkplayer.phone)
├── PhoneApp.kt             # Bottom navigation + navigation graph
├── PhoneTheme.kt / PhoneIcons.kt / PhoneRoutes.kt
└── screen/                 # Home / Files / SMB browser / Player / Settings
```

The two design systems never cross: `:core` depends on no Material library at all, the TV flavor pulls only
`androidx.tv.material3` (compose 1.12.x) and the phone flavor only `androidx.compose.material3` 1.5.0-alpha
(which drags in compose 1.13.0-alpha). Dependencies resolve per variant, so neither pollutes the other.

Pure JVM unit tests live in `core/src/test/java/org/mz/mzdkplayer/`.

### Dual Playback Engines

Playback is abstracted behind `player/core/IMzPlayer.kt`, implemented by `MzExoPlayer` (default: hardware-decoding friendly, fast startup) and `MzVlcPlayer` (broader format compatibility, supports passthrough). The UI depends only on the interface, so you can switch engines at any time under `Settings -> Playback & Video -> Default Player Engine`, and plugging in a new engine stays straightforward.

### Core Components

- `VideoPlayerScreen` - Main player interface
- `BuilderMzPlayer` - Player construction and configuration
- `AkDanmakuPlayer` - Danmaku playback component
- `MovieDetailsScreen` / `TVSeriesDetailsScreen` - Movie/TV show details pages
- `FullDescriptionDialog` - Detailed description popup dialog
- `LocalProxyServer` / `ProxyManager` - Local HTTP proxy that works around compatibility issues between certain protocols and ExoPlayer
- `RemoteInputServer` / `RemoteInputQRPanel` - Phone remote control via QR code

### Data Layer Notes

- The Room database `AppDatabase` holds three tables: `media_cache` (scraped media cache keyed by `videoUri`), an audio cache, and playback history.
- The URI passed from list screens to the player matches `media_cache.videoUri`, so the player can hit the scraped cache directly without re-fetching.
- Navigation arguments (URI, file name, connection name, etc.) are Base64-encoded to keep special characters from breaking the routes.

---

## Development Guide

### Common Commands

```bash
./gradlew :app:compileTvDebugKotlin          # Compile only — fastest syntax check
./gradlew :app:assembleTvDebug               # Build a debug APK
./gradlew :app:assembleTvRelease             # Build a release APK (unsigned)
./gradlew :core:testDebugUnitTest           # Run all JVM unit tests
./gradlew :core:testDebugUnitTest --tests "org.mz.mzdkplayer.tool.PlayerMediaTextTest"   # Run one test class
./gradlew clean                            # Clean build outputs
./gradlew --stop                           # Stop the Gradle daemon (when lock files conflict)
```

### Unit Tests

- Tests depend on **JUnit 4 only** — no mockito, no robolectric, and `returnDefaultValues` is **not** enabled.
  Tests must therefore be pure JVM tests: calling Android types such as `android.net.Uri`, `android.util.Log`, `android.util.Base64`, or `Context` throws `RuntimeException("Stub!")`.
- Extract testable pure logic into `object` / `internal object` declarations that do not depend on Android; tests and the code under test both live in `:core` (`internal` is deliberately invisible to the app module).
- Naming convention: class names end with `Test`, and test functions use backtick-quoted descriptions, e.g. `` `Series - title with season/episode/year` ``.
- Existing tests: `MediaInfoExtractorFormFileNameTest` (file name parsing), `PlayerMediaTextTest` (player title and date), `FileTimeParseTest` (HTTP date / protocol inference / credentials / NFS path splitting).

### Logging

The project uses logback-android. All network protocol implementations (SMB / FTP / WebDAV / NFS) configure timeouts and degrade gracefully on failure, returning an empty list or falling back to local reads instead of interrupting playback.

---

## Hardware Requirements

### Recommended

- **Chipset**: Amlogic S928X-J
- **RAM**: 4GB and above
- **System**: Android TV 11 and above

### Balanced

- **Chipset**: MT9653 or equivalent performance chipset
- **RAM**: 2GB RAM
- **System**: Android TV 7 and above

### Minimum

- **Chipset**: Amlogic S905L or equivalent performance chipset
- **RAM**: 1GB RAM
- **System**: Android 6.0 (API 23) and above (`minSdk = 23`); remote operation requires D-pad support on the TV or box

> ⚠️ **Note**: The code is not well-optimized; it's a success if it runs. There are bugs. Insufficient device performance may cause video and danmaku playback lag, or failure to play high-bitrate videos.

---

## Project Status

⚠️ **Development Phase**: Initial stage, known bugs exist

### Recent Development Plans

- [x] FTP protocol support
- [x] WebDAV protocol support
- [x] NFS protocol support
- [x] Audio file and image file support
- [x] Playlist management
- [x] Movie/TV series details page
- [ ] Online danmaku loading function
- [ ] Settings interface optimization

See [CHANGELOG.md](CHANGELOG.md) for the full version history.

---

## Contributing

Contributions of any kind are welcome — contributions to **player stability** are especially welcome!

### Reporting Issues

When filing an issue, please include: device model and chipset, system version, app version (`Settings -> About`), the protocol in use (SMB / FTP / WebDAV / NFS / HTTP or local), reproduction steps, and a captured `adb logcat` snippet. Issues with logs are usually resolved much faster.

### Code Contribution Workflow

1. **Fork** this repository and branch off `main`. Suggested branch names:

   | Prefix | Purpose | Example |
   | --- | --- | --- |
   | `feat/` | New feature | `feat/nfs-reconnect` |
   | `fix/` | Bug fix | `fix/vlc-audio-crackle` |
   | `docs/` | Documentation | `docs/readme-usage` |
   | `refactor/` | Refactoring | `refactor/player-engine-interface` |

2. Verify compilation and tests locally before committing:

   ```bash
   ./gradlew :app:compileTvDebugKotlin
   ./gradlew :core:testDebugUnitTest
   ```

3. Commit and open a Pull Request against `main`, describing what was fixed, how it was verified, and which modules are affected.

### Commit Message Convention

This repository uses one-line descriptive commit messages: state what changed, and separate multiple changes with commas.

```
Show scraped title and file date on the player screen, flush playback progress periodically, rewrite file name parsing
```

### Code Style and Conventions

- Follow the official Kotlin code style (`kotlin.code.style=official`).
- Keep all business logic in `:core` and never pull Material into it: the TV side only uses `androidx.tv.material3` and the phone side only `androidx.compose.material3`; the two must not cross.
- TV pages belong in their own package under `app/src/tv/java/org/mz/mzdkplayer/ui/screen/` and are registered as routes in `MzDKPlayerAPP.kt`; phone pages live in `app/src/phone/java/org/mz/mzdkplayer/ui/phone/screen/` with routes in `PhoneRoutes.kt`.
- Inject DAOs into ViewModels via `viewModelWithFactory { RepositoryProvider.xxx() }`; never fetch a DAO directly inside a composable.
- Network protocol code **must set timeouts** and degrade gracefully on failure — never block playback or throw onto the UI thread.
- Implement player-engine changes against `IMzPlayer`; do not write `if (exo) ... else ...` in the UI layer.
- Do not hardcode user-facing strings — add them to `res/values/strings.xml` and mirror them into `values-en` / `values-ja` / `values-zh-rTW`.
- For user-visible changes, update [CHANGELOG.md](CHANGELOG.md) and the `versionName` in `app/build.gradle.kts`.

### Pull Request Checklist

- [ ] `./gradlew :app:compileTvDebugKotlin` passes
- [ ] `./gradlew :core:testDebugUnitTest` passes (add tests for new pure logic)
- [ ] No local files committed (`local.properties`, `app/build/`, `.gradle/`, `.idea/`, etc.)
- [ ] New strings added to every language's `strings.xml`
- [ ] User-visible changes reflected in `CHANGELOG.md` (and `versionName` when appropriate)
- [ ] Verified on a real device / TV (state the device and system version)

### About the Binary Dependencies in This Repository

`akdanmaku.aar` and `lib-decoder-ffmpeg-release*.aar` under `app/libs/` are prebuilt local libraries required by the player. They are shipped with the repository — **no need to build them yourself, and do not delete them**.

### Automated Build and Release (GitHub Actions)

Two workflows ship with the repository, so you never have to package a build by hand:

| Workflow | Trigger | What it does |
| --- | --- | --- |
| `ci.yml` | Push to `main`, every PR | Compiles and runs the JVM unit tests, uploads the test report as an artifact |
| `release.yml` | Push to `main` (when the version is new), `V*` tag push, manual run | Runs the tests → builds signed APKs → creates the Release with all three APKs attached |

**Releases are version-driven**: `release.yml` reads `versionName` from `app/build.gradle.kts` and, if there is no `V<version>` Release yet, builds and publishes automatically. In other words, bump `versionName` as usual, push, and the Release appears — no manual tagging needed (the workflow creates the tag, keeping the historical uppercase `V` prefix such as `V1.17.4`).

The release body is taken from the "未发布" (unreleased) section of [CHANGELOG.md](CHANGELOG.md), so **write the changelog entry before releasing**.

Publishing needs these Secrets (`Settings → Secrets and variables → Actions`). If any is missing the workflow skips publishing and lists what is missing in the run summary:

| Secret | Purpose |
| --- | --- |
| `TMDB_API_KEY` | Written into `BuildConfig.TMDB_API_KEY` at build time; an empty value silently breaks TMDB scraping |
| `RELEASE_KEYSTORE_BASE64` | base64 of the signing keystore: `base64 -w0 release.jks` |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |

Signing material is passed to Gradle only through environment variables (`MZDK_KEYSTORE_FILE` and friends) — **never commit the keystore**. Without those variables, `assembleTvRelease` keeps producing `app-tv-*-release-unsigned.apk` exactly as before.

To republish the same version, run the `Release` workflow manually and tick `force`.

---

## Disclaimer

This software is for learning and exchange purposes only; please do not use it for commercial purposes. The developer is not responsible for any issues caused by the use of this software.

---

## License

This project is released under the [GNU General Public License v3.0](LICENSE).

---

**Note**: Normal use of features like Dolby Vision, Dolby Atmos, and DTS-HD requires device hardware support. Some features may require specific audio/video equipment for the best experience.
