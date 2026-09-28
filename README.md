# Yamaha Network Receiver Controller — v0.7.5 beta

VersionName **0.7.5**, versionCode **12**; package **com.styl15hh1.rn301controller** unchanged.

- Favorites: maximum four, always one horizontal row; four entries have equal widths. Approved 68 dp minimum tile height and selected styling retained, with compact icons/type for four tiles. Existing five-entry preferences migrate to their first four, preserving order and the preference key.
- NET RADIO: complete menus are aggregated from Yamaha pages and displayed in one scrollable list. No page counter/buttons or menu-level debug labels. Compact Back / Net Radio / Home / Refresh toolbar sits beneath the unchanged receiver header.
- Country menus show optional offline Unicode flags. Unmapped names remain usable; no flag images are downloaded.
- Navigation, station selection, Now Playing, metadata and Stop use the existing receiver XML unchanged. No direct Radio Browser API and no mandatory YTuner dependency.
- Traversal stops at 64 pages or 90 seconds and rejects changed/stuck menus. Single-page menus send no page commands. Duplicate labels remain distinct, safely selected by their original page and line.
- SERVER, Tuner, Volume and Spotify behavior retained. All eight locales include the new toolbar accessibility label.

Debug APK: app/build/outputs/apk/debug/app-debug.apk. Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

**Validation:** 365 tests passed; 0 failed/errors/skipped. Full Gradle build succeeds. Lint: 0 errors / 4 existing warnings. All eight locales validated; debug APK v2 signature verified. Light/dark renders inspected.

[Validation](docs/VALIDATION.md) · [Protocol details](docs/YAMAHA_PROTOCOL.md) · [Capability audit](docs/RN301_CAPABILITY_AUDIT.md).

After installation, check migration of five Favorites, four tiles on the phone, a multi-page country menu, station selection from early/late rows, Back/Home/Refresh and NET RADIO Now Playing/Stop. New behavior is not yet physically verified.

Earlier sections are release history; this v0.7.5 section supersedes conflicting prior limits or pagination descriptions. Development stops at v0.7.5.
---

# Yamaha Network Receiver Controller — v0.7.4 beta

VersionName **0.7.4**, versionCode **11**; package **com.styl15hh1.rn301controller**.

- Home: global receiver header → collapsed Volume (live native integer and mute summary) → approved Favorites → Now Playing → collapsed Sources. Volume expansion is local session state; no request is sent by opening/closing it.
- Expanded Volume reuses the existing 220 dp rotary, 125 ms serialized/conflated live writes, reconciliation, haptics, accessibility, +/- and Mute.
- Spotify: prominent track, secondary artist, tertiary unique album and subdued actual playback state. Blank rows are omitted; whitespace/case-insensitive duplicates are removed outside Compose.
- Explicit Open Now Playing action replaces the standalone Spotify chevron. Previous, Play/Pause and Next remain; Spotify Stop/Shuffle/Repeat are excluded. SERVER/NET RADIO controls are unchanged.
- Generic NowPlaying retains optional input-logo paths as source branding only; no album-art fetch, Spotify Web API or cloud integration.
- Eight locales retained. Favorites tiles, Tuner, global header, Settings, firmware and connection architecture are preserved.

Latest user-supplied hardware evidence: **Spotify Play_Info and the Basic_Status control PASS**; tested Sound/Equalizer/Tone/Speaker and Spotify Shuffle/Repeat GET paths return HTTP 400. Bass/Treble/Balance/Speakers A/B/Sleep/preset STORE remain **PHYSICAL_FEATURE_XML_UNRESOLVED**, not absent physical features. No speculative commands or Sleep fallback.

**Validation:** 345 tests passed, 0 failed/errors/skipped; full Gradle build successful; lint 0 errors / 4 existing warnings; all eight locales validated; debug APK v2 signature verified.

[Protocol evidence](docs/YAMAHA_PROTOCOL.md) · [Capability audit](docs/RN301_CAPABILITY_AUDIT.md) · [Build validation](docs/VALIDATION.md) · [v0.7.4 phone checklist](docs/PHYSICAL_TEST_CHECKLIST.md).

Debug APK: app/build/outputs/apk/debug/app-debug.apk. Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`, or copy it to the phone and allow installation from that source. Build instructions and architecture below remain applicable. New UI changes need physical phone verification.

Earlier release sections below are historical; v0.7.4 behavior above takes precedence. Development stops at v0.7.4.
---

# Yamaha Network Receiver Controller — v0.7.3 beta

VersionName **0.7.3**, versionCode **10**; package unchanged. Development remains in **0.7.x**; 1.0.0 is reserved for a separately approved, physically validated stable release.

- Centered Favorites heading; approved tile dimensions, spacing, artwork, selection and persistence unchanged.
- Verified-reference FM/AM selection, config-driven AM tuning/seek and FM Auto/Mono controls with receiver readback.
- Shared optional RDS metadata for Tuner and Home Now Playing; deduplicated text, compact no-RDS fallback, no stale FM metadata on AM. Clock_Time is optional opaque text, not an inferred clock.
- Receiver firmware information in Settings → About, from existing optional System/Config; no new polling.
- **Sleep Timer withheld:** compatible XML remains unresolved. No speculative command, fake timer UI or Android shutdown timer was added. [Evidence and Sound research](docs/RN301_CAPABILITY_AUDIT.md).
- Approved live rotary Volume, global header and compact numbered presets preserved.

User-reported **v0.7.2 physical PASS**: live Volume while dragging, header, Favorites tiles/layout, Tuner redesign, Power/Mute/source functionality. New v0.7.3 controls need [physical testing](docs/PHYSICAL_TEST_CHECKLIST.md); reference evidence and unit tests are not physical command verification.

Validation: 318 tests pass; full Gradle build succeeds; lint 0 errors / 4 existing warnings; all eight locales pass; APK v2 signature verifies. [Detailed results](docs/VALIDATION.md) · [Protocol commands](docs/YAMAHA_PROTOCOL.md).
Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Earlier milestone descriptions below are historical; v0.7.3 behavior above takes precedence.

# v0.7.2 release

- Live rotary volume: latest target every 125 ms at most (slower when requests are slow), serialized/conflated writes, no FIFO backlog, duplicate-free final synchronization and receiver readback. Native integers remain native; no dB mapping.
- Existing 220 dp knob, 12° sensitivity, full hit area, outside capture, haptics, accessibility and +/- remain.
- Favorites: minimum 68 dp height, centered groups, 2 dp heading gap, unchanged preferences/order.
- One global receiver header across all screens: model/status/IP, Power, Settings and disconnected Connect. Connect tries the saved receiver, then reuses SSDP and explicit candidate selection; manual setup remains.
- Tuner: authoritative actual metadata and compact equal-size number-only presets with selected semantics. Manual tuning, seek and preset navigation unchanged; Refresh is secondary.
- [Capability audit](docs/RN301_CAPABILITY_AUDIT.md) separates verified XML from documented features and commands for other Yamaha models. No v0.8 controls added.

Version 0.7.2 / code 9. Package unchanged. User reports v0.7.1 rotary, +/-/Mute, Favorites, sources, header Power, connection display, collapsed Sources, Tuner, Spotify and communication PASS. New live-volume behavior and v0.7.2 layouts need [physical retesting](docs/PHYSICAL_TEST_CHECKLIST.md). See [validation](docs/VALIDATION.md) for build/test/signature results.

Historical release details below retain their original descriptions; the v0.7.2 behavior above takes precedence.

# Yamaha Network Receiver Controller — v0.7.2

A native, open-source Android controller for the **Yamaha R-N301**. The phone communicates directly with the receiver over the local legacy Yamaha HTTP/XML API. No Home Assistant server, Yamaha/Spotify account integration, cloud backend, proxy, analytics, advertising or telemetry.

## Physical hardware status

User-reported PASS: connection/model/power/current-source reads; SSDP discovery/IP detection; source switching/UI; tuner/preset retrieval and selection; native volume integer steps; manual tuner frequency controls.

**v0.4 Mute/Unmute using Main_Zone/Volume/Mute: physically verified PASS.**

SERVER browsing/playback remains physically PENDING because no DLNA server is configured. v0.5 Spotify/AirPlay artwork rendering and Settings gear/navigation are physically PASS; the v0.6 UI changes are functional. NET RADIO source activation, basic Yamaha/vTuner catalogue browsing and station playback are now physically PASS. Navigation/service edge cases are not all verified. Power/Standby commands and Mute/Unmute are physically PASS. Existing Spotify playback controls and tuner seek were not separately reported as passed. See [physical checklist](docs/PHYSICAL_TEST_CHECKLIST.md).

## Historical v0.7.1 maintenance release (physically PASS)

Physical v0.7.0 feedback: volume +/-, Quick Sources, source selection and connection **PASS**; rotary gesture **FAIL**. The rotary correction below was subsequently physically verified PASS in v0.7.1. v0.7.2 replaces its release-only write scheduling.

- Fixed rejected inner-knob touches and discarded previews when a finger reached the center or left the old annular bounds. The entire knob square accepts a starting touch; a captured finger can move beyond the knob. The tiny center singularity pauses/rebases angles without cancelling.
- Responsive knob grows from 180 to at most 220 dp (+22%). Sensitivity remains 12 degrees per native integer step. Movement previews locally; release sends at most one existing native-volume command, followed by receiver readback. Cancel/unchanged gestures send none. No dB conversion or protocol changes.
- Compact Home: receiver name plus connection/IP, Power then Settings; Volume with one minus/Mute/plus row; Favorites; Now Playing; collapsed full Sources at the bottom.
- Favorites reuse the exact Quick Sources preference key, order and settings editor. With no saved key, supported Optical/Tuner/Spotify are offered as defaults. Explicitly saved empty/custom lists are preserved. Unavailable favorites are hidden; the defaults do not send input commands.
- The full Sources grid uses existing selection logic and resets to collapsed whenever Home is recreated.
- Tests include real Compose touch injection inside a scrolling parent, running locally with Robolectric, plus domain/ViewModel/repository tests. They do not replace phone/receiver testing.

## Earlier v0.7 daily-use improvements (scheduling/layout superseded above)

- **Rotary volume:** native Compose knob, relative angular gestures at 12 degrees per integer step, local preview and a single final target write on release. No volume writes during pointer movement; normal status polling continues; the receiver’s actual readback resolves the preview.
- **Precision/accessibility:** existing +/- commands retained as small 48 dp controls. Rotary custom accessibility actions and keyboard arrows provide alternatives. Platform step haptics respect system settings; no vibration permission.
- **Favorite Sources:** choose up to five in Settings, preserve selection order or move an entry up. Saved sources that are no longer available are hidden from shortcuts and removable in Settings. The full source grid remains available in the collapsed Sources section.
- **Last used:** locally records an explicitly selected, confirmed source and offers a shortcut when useful. It never switches input on launch.
- **Tuner:** responsive large preset cards, current station/frequency/preset emphasis, actual optional RDS and secondary expandable manual tuning. A compact tuner card is available on Home.
- **Now Playing:** shared title/station-first hierarchy, deduplicated metadata and a compact idle row when no useful information is available. Source-specific playback capabilities remain unchanged.
- New UX preferences reuse SharedPreferences; new strings are translated into all eight languages.

## Preserved receiver functionality

- **NET RADIO:** dynamic receiver-owned directories/stations, multi-level navigation, Back/Home, eight-slot pages, bounded readiness, station selection and shared Now Playing. The receiver retrieves the catalogue and audio; the phone never queries a radio directory or obtains stream URLs.
- **Radio paths:** actual selected menu names form a logical list. Internal exact-name traversal searches all bounded pages and rejects duplicate/missing matches. RadioFavorite is a model only; no station-favorites UI or persistence.
- **Player capabilities:** NET RADIO exposes Stop only. Station Direct_Sel followed by Play is internal; Pause/Previous/Next are not exposed for this source.
- **Alignment/artwork:** centered Volume and player content with toolbar Power; Spotify displayed 18% smaller; symbol-only AirPlay with one label. Native volume protocol and units are unchanged.

- **SERVER/DLNA browser:** controls the R-N301’s own browser. The phone does not browse NAS devices independently, retrieve media URLs or stream audio. Media flows directly from NAS/DLNA server to receiver.
- **Navigation:** receiver-reported containers/tracks, menu name and level, Back, Home, refresh and explicit previous/next pages. Eight-slot windows follow reported cursor/count fields. No fake filesystem paths or album subtypes.
- **Playback:** verified reference commands for SERVER Play/Pause/Stop/Previous/Next, using the shared compact/full Now Playing. Missing metadata is omitted.
- **Safe loading:** confirm SERVER source before browsing; bounded Busy retries and operation deadline; stale-list checks before selection. Browser/media-server errors do not automatically disconnect the receiver.
- **Artwork:** replacement Spotify and white AirPlay transparent assets, no Spotify background rectangle, preserved proportions, theme-aware foreground and shared disabled alpha.
- **Settings:** fixed top-right Material gear; the former empty gear vector is corrected. Full-width Language/Appearance/About rows and a dedicated About page.
- **Languages:** System default plus English, Español, Deutsch, Français, Italiano, Polski, 한국어 and 日本語. Selection is indicated and persisted. Android 13+ per-app locale API; compatibility context/recreation on Android 8–12. All translations are packaged offline.
- **About:** actual build version, unofficial-controller notice, STYL15HH1 and a tappable GitHub profile.
- Version **0.7.2 (9)**; package remains **com.styl15hh1.rn301controller**.

No Spotify Web API, OAuth, AirPlay protocol, artwork fetching, favorites, widgets, background service or notifications.

## Preserved controls

- Finite SSDP discovery, explicit receiver choice, automatic reconnect to the saved receiver, manual IPv4/hostname fallback and connection test.
- Power/standby, actual state reads, mute/unmute and adaptive source tiles with safe unknown-input handling.
- Native volume minus/plus, fresh read before one integer write, readback afterward. No press-and-hold or automatic write retry.
- Tuner page, large saved-preset cards, direct preset selection, previous/next stored preset, manual FM tuning and seek using advertised range/step.
- Spotify receiver-local player and shared Now Playing, without authentication or a cloud API.
- System/light/dark appearance, launcher adaptive/themed icons, eight-language resources and persistent settings.

Coaxial fallback stays disabled unless its identifier is advertised or observed. AirPlay remains input-only; SERVER supports browser/player; NET RADIO supports receiver browsing and a Stop-only player.

## Volume remains native

The receiver reports native integers such as **40/50**, Exp=0 and empty Unit. The rotary display shows the number without a technical suffix; no dB conversion is inferred. The rotary gesture uses the current value as its starting point and does not require runtime bounds.

The inspected firmware reports no valid volume limits and returns HTTP 404 for desc.xml. The manual documents numeric Max Volume/Initial Volume settings, not a universal runtime range. The rotary therefore uses an explicit conservative application guardrail of **1–99**, narrowed by matching advertised integer limits when available. No verified configured-Max read path was found; receiver clamping/readback is authoritative. A reported zero is displayed unchanged and cannot turn upward from a counter-clockwise gesture. Existing precision +/- behavior remains unchanged. The fallback only handles native integer values 0..99; the manual's separate Max sentinel is not guessed. There is no verified atomic relative-volume command: a fresh read/SET can still race with another controller. Actual receiver readback, including maximum-volume clamping, remains authoritative.

## Build and installation

Android 8.0+ (minSdk 26), targetSdk 36, compileSdk 37. The Galaxy S23 Ultra is within the supported API range.

Pinned toolchain: JDK 17, Gradle 9.6.0, AGP 9.4.1, Kotlin/Compose compiler 2.4.20, Compose BOM 2026.09.00, Activity 1.13.0, Lifecycle 2.11.0, coroutines 1.11.0. No runtime dependency was added for v0.7.

1. Install JDK 17 / Android Studio and SDK packages platforms;android-37.0, build-tools;36.0.0 and platform-tools; accept SDK licenses.
2. Set JAVA_HOME and ANDROID_HOME or use an untracked local.properties SDK path.
3. Run ./gradlew build (Windows: gradlew.bat build). Focused tasks: :app:testDebugUnitTest :app:lintDebug :app:assembleDebug.

The original workspace has an ignored project-local toolchain and .work/build.ps1 helper; these are not runtime dependencies.

Debug APK: **app/build/outputs/apk/debug/app-debug.apk**

Install with adb install -r app/build/outputs/apk/debug/app-debug.apk, or transfer it to the phone and install through a trusted file manager. Updating with the same debug signing key preserves preferences.

Connect phone and receiver to the same LAN. Choose a discovered receiver; use Settings for scanning/manual entry if multicast is blocked. The last selected receiver is tried first on launch. Network standby must be configured on the receiver to allow wake from standby.

Select SERVER or Net Radio to open the receiver menu. Open folders, change pages, or select a track to play it on the receiver. Android Back navigates one level before leaving root; Home returns to the root menu. The Receiver action can leave an unresponsive browser. Now Playing opens the common player screen.

## Architecture

Compose -> ReceiverViewModel / StateFlow -> YamahaRepository -> protocol/network -> R-N301.

- XML builder/parser remain separate and unit-testable; Compose never builds XML.
- YamahaDiscovery abstracts finite SSDP discovery and identity verification.
- YamahaMediaBrowser exposes current-list, selection, Back/Home and paging operations. ServerMediaBrowser and NetRadioMediaBrowser share LegacyMediaBrowser navigation and validation, with source-specific command dispatch and readiness limits. RadioPathNavigator provides bounded exact-name traversal.
- YamahaPlayer dispatches distinct Spotify/SERVER/NET RADIO commands into the reusable NowPlaying model.
- All control/browser operations share a repository mutex. Transport enforces minimum 200 ms spacing.
- General status and visible player metadata refresh approximately every two seconds plus request duration. Browser lists load only on entry/actions/refresh, never in periodic polling.
- Backgrounding cancels polling/actions. Stale connection failures back off; failed media operations remain isolated.
- SharedPreferences stores receiver/settings. No library paths, media URLs or user accounts are persisted. Radio navigation paths remain in memory.

## Local networking and security

[Discovery design](docs/DISCOVERY.md): SSDP search on 239.255.255.250:1900, bounded response window/deadline, verified R-N301 candidates, Wi-Fi multicast lock, no blind LAN scan.

Permissions remain INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE and CHANGE_WIFI_MULTICAST_STATE. No new v0.7 permissions. Target 36 retains implicit LAN access through INTERNET; target 37 runtime LAN permission migration is deferred.

Legacy HTTP requires app-level cleartext allowance because static domain policy cannot describe arbitrary user-supplied LAN hosts/CIDRs. Runtime transport restricts private/link-local IPv4 destinations, fixed Yamaha port-80 endpoints, disables redirects/proxies and bounds responses. Discovery additionally accepts same-host UPnP descriptions on port 80/8080. Public URLs and arbitrary endpoints are rejected. XML DTD/entities are rejected.

Hostname resolution uses the selected local network/system resolver. Use numeric IPv4 if DNS queries are undesirable. Debug Logcat records command type, endpoint/status, menu status/layer/cursor/page, truncated selected names/path and playback/error state. Raw catalogues/payloads are not logged; catalogue names in debug logs may still be private.

## Limitations and roadmap

SERVER fixtures are synthetic, based on the inspected R-N301 integration and receiver-served control code. Confirm readiness, empty menus, page boundaries, selection/playback, metadata, deep Back/Home navigation and NAS disappearance on hardware. The legacy API has no verified transaction token: another controller may change the menu between preflight and selection.

Container metadata does not reliably identify albums/playlists; display containers without guessing from names/depth. Only the current receiver menu/window is shown. Radio breadcrumbs use actual selected names and become unknown after external navigation/errors until the root is reached. There is no filesystem path or stable catalogue ID.

The user physically confirmed the receiver’s Yamaha/vTuner catalogue and basic browsing/playback. Availability remains controlled by that legacy service. No bypass, custom station URL or YCast support is included. Ready/Busy grammar is reference-backed; Loading is tolerated conservatively. Unknown statuses fail safely. Metadata fields, station transition timing and the legacy service must be checked on hardware. Exact path replay is bounded to 32 names, 64 pages per level and 120 seconds; oversized/slow catalogues fail without guessing.

- v0.2: discovery and Tuner/presets — physical PASS.
- v0.3: native volume steps/manual FM/artwork — physical PASS; Spotify playback needs separate confirmation.
- v0.4: Mute fix/settings/localization — Mute physical PASS.
- v0.5: SERVER browser/player implemented and unit tested; physical PENDING without a DLNA server. Settings/artwork rendering physical PASS.
- v0.6: NET RADIO activation/basic browsing/playback and UI functionality — physical PASS; edge cases still pending.
- v0.7: rotary volume, Quick Sources, tuner/player UX — implemented; new phone/receiver acceptance pending.
- Later: favorites, home-screen widget, discovery refinements.
- v1.0: polished production release.

## Documentation

- [Protocol commands and exact references](docs/YAMAHA_PROTOCOL.md)
- [Build/tests/APK validation](docs/VALIDATION.md)
- [Physical findings and acceptance checklist](docs/PHYSICAL_TEST_CHECKLIST.md)
- [Third-party artwork and licenses](THIRD_PARTY_NOTICES.md)

MIT license. Unofficial and not endorsed by Yamaha. Reference repositories were inspected, not modified or included as runtime dependencies.
