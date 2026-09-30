# Historical v1.0.0 rebranding validation - 2026-09-30

This candidate is based solely on original commit **300f0e7b62521ed76366bf9c072d76decdf97d8a**, branch **rebrand-v1.0.0**. Only the application/project name, public links, screenshot filenames and related documentation changed. One branding resource test was added to the historical suite. No later-release source, tests, functionality or hardware-validation claims were imported.

The public name is **Yamaha Receiver Controller**. Repository links use **STYL15HH1/Yamaha-Receiver-Controller**. The README and release notes include: "Independent open-source project. Not affiliated with or endorsed by Yamaha Corporation." R-N301 remains the primary physically verified target; R-N500 and selected older RX-V/RX-A receivers remain untested potential candidates. Historical SERVER/DLNA status and all feature behavior remain as recorded for this release.

## Validation results

- Untouched historical baseline: assembleRelease succeeded in 49 seconds, before any edits.
- Rebranded candidate: testDebugUnitTest assembleRelease lintDebug lintRelease succeeded in 1m 24s.
- Full historical suite: **389 tests passed**, 39 classes, zero failures/errors/skips. This is the original 388 tests plus one resource-only branding check.
- ResourceValidationTest: **5 passed**, covering branding, eight locale sets/format arguments, product labels, language tags and launcher densities.
- Debug lint: **0 errors / 6 warnings**.
- Release lint: **0 errors / 6 warnings**.
- Reported warnings: OldTargetApi, InsecureBaseConfiguration, three UnusedResources and IconXmlAndPng. No new warning category. The previously reported dependency-version advisory was not emitted by this run.
- The unchanged RotaryVolumeControl source reports a redundant conversion warning; the baseline native packaging reports libandroidx.graphics.path.so cannot be stripped and is packaged as-is.
- git diff --check: PASS.
- Production Kotlin, manifest, application build/signing configuration and version values are unchanged from the original commit.
- Eight renamed screenshots are byte-identical to the originals, and every README image reference resolves.
- No old-name text remains in current public worktree files. Git history and the retained baseline APK are intentionally unchanged.
- No files staged; no commit, tag, push or publication performed.

## Signed artifact

File: **app/build/outputs/apk/release/Yamaha-Receiver-Controller-v1.0.0.apk**

- Size: **9,011,399 bytes**.
- APK SHA-256: **398B2EDF1357F3A33F5815387C4DBC60275BBEA11DA9AB110A8E4A285D197836**.
- Checksum file: **app/build/outputs/apk/release/Yamaha-Receiver-Controller-v1.0.0.sha256**.
- Checksum file contents:

~~~text
398B2EDF1357F3A33F5815387C4DBC60275BBEA11DA9AB110A8E4A285D197836  Yamaha-Receiver-Controller-v1.0.0.apk
~~~

apksigner verifies exactly **one signer**. Scheme **v2** is present; v1/v3/v3.1/v4 are absent, matching the unchanged historical baseline configuration.

Certificate SHA-256: **AB:2A:CE:5A:9A:33:D2:F9:EC:C1:D3:48:2D:FA:51:58:DD:AE:44:A5:89:79:FA:18:5E:F1:32:96:E0:1D:A0:92**.

aapt2 confirms **com.styl15hh1.rn301controller / 1.0.0 / 15**, no debuggable flag, and **Yamaha Receiver Controller** as the application label in every packaged locale. APK and checksum file are ignored local outputs.

## Executable comparison and limits

No originally published v1.0.0 APK was found in the project directories. Before edits, the exact historical commit was built with the existing toolchain and signing configuration and preserved as **.work/original-commit-v1.0.0.apk**.

Baseline SHA-256: **643501E220BD889E82EAA864D52BB5C41300F3E08C924FE4548668309E56E7E1**.

Both executable DEX files in the replacement are **byte-identical** to that unmodified-commit baseline. This proves unchanged executable contents relative to the reconstructed historical build; it is not a byte comparison against the originally published APK, which was unavailable locally.

No existing old-brand APK was overwritten. Physical acceptance and any public replacement remain manual follow-up work. Historical measurements below describe prior artifacts, not this rebranded candidate.

# v1.0.0 validation

## Final release polish validation - 2026-09-29

The Spotify Now Playing header retains the existing wordmark and accessible source name while removing the duplicate visible label. Artwork dimensions, metadata, playback controls and navigation remain unchanged. Signing configuration and release identity are unchanged.

- Full unit suite: **388 passed**, 39 classes, zero failures/errors/skips; includes two new Spotify header regression tests.
- All four resource validation tests pass, including all eight locales and launcher densities.
- Gradle testDebugUnitTest assembleRelease lint lintRelease: **BUILD SUCCESSFUL**, 3m 44s.
- Debug and release lint: **0 errors / 7 existing warnings** each; no new suppressions.
- Final APK: historical pre-rebranding v1.0.0 artifact; **9,011,407 bytes**.
- apksigner: **Verifies**, one signer; v2 true; v1/v3/v3.1/v4 false.
- Certificate SHA-256: **AB:2A:CE:5A:9A:33:D2:F9:EC:C1:D3:48:2D:FA:51:58:DD:AE:44:A5:89:79:FA:18:5E:F1:32:96:E0:1D:A0:92**, exact required match.
- APK identity: **1.0.0 / 15 / com.styl15hh1.rn301controller**; minSdk 26, targetSdk 36.
- README now describes the current product; five inspected, existing screenshots form its gallery. See [screenshot inventory](screenshots/README.md) for exact files and exclusions. Selected files have no GPS EXIF data.
- Repository audit found no tracked/unignored keystores, credential files, APKs or matching secret assignments; no private absolute paths were introduced into README/docs. The final APK is ignored.
- No commit, push, tag or GitHub Release was created. No new physical-hardware verification is claimed.
- No technical/signing blocker remains. The previously recorded supplied-artwork redistribution question still needs owner resolution before publication.

The records below describe earlier validation stages.

## Signing completed — 2026-09-29

The signing blocker is resolved. Release signing uses only YAMAHA_RELEASE_STORE_FILE, YAMAHA_RELEASE_KEY_ALIAS, YAMAHA_RELEASE_STORE_PASSWORD and YAMAHA_RELEASE_KEY_PASSWORD from the owner's external Gradle configuration. No credentials, aliases or private keystore paths were placed in tracked files. No new key was generated.

- Final clean assembleRelease: BUILD SUCCESSFUL, 42s.
- Missing-property release check: fails early with a clear property-name-only message, as intended.
- assembleDebug without release properties: BUILD SUCCESSFUL, 33s.
- Final APK: historical pre-rebranding v1.0.0 artifact
- Size: 9,011,407 bytes.
- apksigner: Verifies; one signer; v2 true; v1/v3/v3.1/v4 false.
- Certificate SHA-256: AB:2A:CE:5A:9A:33:D2:F9:EC:C1:D3:48:2D:FA:51:58:DD:AE:44:A5:89:79:FA:18:5E:F1:32:96:E0:1D:A0:92 (exact required match).
- APK metadata: versionName 1.0.0, versionCode 15, application ID com.styl15hh1.rn301controller, minSdk 26.
- APK remains ignored by Git. No commit, push, tag or GitHub Release was made.
- The previous full 386-test validation was not repeated. Signing-only build/configuration checks passed.
- Remaining publication concern: supplied artwork redistribution rights, as documented below.

The existing local build helper deliberately uses an isolated Gradle home without signing credentials. Signed builds must use the owner's global Gradle home instead; do not copy secrets into the repository or helper. Configuration caching was disabled for these signing validation builds.


### Initial release preparation record (before signing)


**Full milestone result:** 386 tests passed (38 classes), 0 failures/errors/skips. Gradle build, assembleRelease and lint succeeded; separate lintRelease succeeded. Debug/release lint each report 0 errors / 7 existing warnings. All eight locale sets and launcher resources passed validation. Release manifest reports the unchanged package, 1.0.0/code 15, minSdk 26, targetSdk 36.

**Release blockers:** release signingConfig is absent; apksigner confirms the release APK is unsigned (DOES NOT VERIFY). Supplied Spotify/AirPlay/Yamaha artwork redistribution permission is not established by project evidence. No key was generated. No public release or tag was created. Full details, warning inventory, unsigned output path and exact signing requirements: [release preparation](RELEASE_PREPARATION.md).

Version **1.0.0 (15)**, package **com.styl15hh1.rn301controller**. The user approved the current v0.7.7 functionality and visual design as the stable baseline. No new receiver commands or capabilities are introduced.

Later user reports confirm v0.7.5 Power, live rotary Volume, Mute, Favorites/Sources, Spotify playback/metadata, FM/AM Tuner/presets/manual tuning/seek/Auto/Mono/RDS, Net Radio/YTuner aggregation/playback/Stop and reconnection. Subsequent v0.7.7 physical visual feedback and final approval supersede older UI-pending notes. This does not invent new SERVER/DLNA edge-case or unresolved-command physical results.

The alphabet index was removed after physical feedback; current Net Radio uses ordinary scrolling and local Search. Volume/Sources expansion persists. European locales use “Net Radio”; Japanese/Korean retain their translations.

[Release preparation](RELEASE_PREPARATION.md) records current validation/signing blockers. Sections below are historical technical evidence, not the current release designation; earlier pending items are superseded only by explicit later results.

---

# v0.7.6 — UI refinement

VersionName **0.7.6**, versionCode **13**; application ID unchanged.

**v0.7.5 PHYSICAL PASS (user report):** Power; Volume/live rotary; Mute; Favorites/Sources; Spotify playback/metadata; Tuner FM/AM, preset recall, seek/manual tuning, Auto/Mono and RDS; NET RADIO with YTuner, aggregated browsing, playback, Now Playing and Stop; receiver reconnection. This supersedes previous pending v0.7.5 notes below.

v0.7.6 changes: single-row native Volume/mute header; locally persisted Volume/Sources expansion; shared Favorites-style source tiles (four columns at normal phone widths); local NET RADIO search; an A–Z scroll index for long sorted country menus; compact 48 dp radio rows with 4 dp gaps; station-first, deduplicated NET RADIO player presentation with the existing Stop action. Eight locales include new search/index accessibility strings.

No receiver XML, network timing, aggregation, YTuner behaviour or capability changes. The existing live rotary implementation and 125 ms writer remain untouched.

Validation is deliberately targeted: new presentation/UI/preferences cases plus relevant Favorites, Sources, player, NET RADIO and Volume regressions; debug APK build. No full-suite run, lint, additional render generation, signature verification or capability audit for this release.

Limitations: search covers only the currently loaded menu. The alphabet index appears only for recognized country menus with at least 12 entries with compatible A–Z ordering; only letters with matches are shown. Its separate scroll rail retains 48 dp touch targets. Narrow widths/large fonts reduce Sources columns; very long labels may ellipsize with full accessibility names.

**New v0.7.6 UI: pending physical verification.** Check:
1. Volume header native/mute updates and saved expansion after restart; rotary, +/- and Mute still work.
2. Sources expansion survives restart; four-column Sources and four Favorites fit, select and disable correctly.
3. Search countries/stations, clear/no-results and alphabet jumps; no receiver changes from filtering/scrolling.
4. Compact rows/flags and station selection; long/missing NET RADIO metadata, Stop and not-ready state.

**Result:** 67 targeted tests passed, 0 failures/errors/skips. Final targeted run: 27s. `.work/build.ps1 assembleDebug --console=plain`: BUILD SUCCESSFUL, 4s. All eight locale key/format sets pass the targeted resource check. No full suite, lint, screenshots or APK signature verification performed.

Tests selected: V076PresentationTest, V076UiTest, FavoritesTest, MaintenanceUiTest, DailyUsePresentationTest, V074PlayerTest, NetRadioRepositoryTest; the locale-key/format check; the four-Favorites layout test; five existing Home Volume regression cases. An older NET RADIO ordering assertion was updated to the intentional station → artist → song hierarchy.

APK: `app\build\outputs\apk\debug\app-debug.apk`.

Significant files: AppSettings.kt; ExpandableVolume.kt / ExpandableSources.kt / ReceiverScreen.kt; CompactSourceTile.kt / SourceTiles.kt / QuickSourcesUi.kt; MediaBrowserScreen.kt / RadioMenuList.kt / RadioMenuPresentation.kt / CountryFlags.kt; PlayerScreen.kt / PlayerPresentation.kt; eight strings.xml files, search/clear vector assets, targeted tests and app/build.gradle.kts.
---

# v0.7.5 validation — 2026-09-28

VersionName **0.7.5**, versionCode **12**; package **com.styl15hh1.rn301controller** unchanged.

## Final build result

- Command: `.work/build.ps1 test build lint --console=plain`.
- **BUILD SUCCESSFUL**: initial full build 3m27s; final rerun after correcting the screenshot harness background/margins 37s. Debug and release assembled.
- **365 tests passed, 0 failures, 0 errors, 0 skipped** (345 existing plus 20 v0.7.5 cases).
- Lint: **0 errors, 4 existing warnings**: OldTargetApi, AndroidGradlePluginVersion, InsecureBaseConfiguration, IconXmlAndPng.
- All eight locales compile and validate: en, es, de, fr, it, pl, ko, ja. ResourceValidationTest 3/3 passes, including launcher resources.
- Light/dark component renders inspected with a theme Surface and realistic 16 dp phone margins: equal-width Favorites, 68 dp height, legible icons/labels, country flags and scrollable rows, no pagination/depth UI.
- APK signature: apksigner **Verifies**, **v2**, one signer.
- aapt: versionName **0.7.5**, versionCode **12**, unchanged package, minSdk26 / targetSdk36.
- APK: `app\build\outputs\apk\debug\app-debug.apk`.
- Size: **12,586,468 bytes**.
- SHA-256: **D19CBF903F97B260950465E0C91D57FD3274CA7BE8B35ADF67B419A508FF29F0**.
- Reports: app/build/reports/tests/testDebugUnitTest/index.html and app/build/reports/lint-results-debug.html.

## Implementation and evidence

- Favorites limit is four in encode/decode, settings and UI. Existing five-entry settings are rewritten once to their first four, preserving order/key and explicit empty settings.
- Favorites use a single bounded Row. Four tiles have equal weights and retain 68 dp minimum height, Material selected colors, source artwork and existing selection callbacks. Compact four-tile labels/icons accommodate phone margins.
- AggregatingRadioBrowser wraps only NET RADIO. It retains page snapshots and original page/line row identities, rewinds/scans with exact monotonic checks, rejects changed menus and delegates selection to the unchanged legacy adapter.
- Traversal bounded to 64 pages / 512 slots and 90 seconds, including existing readiness waits. Single-page lists never issue Page commands. Station playback does not rescan an unchanged menu.
- Same-named entries are retained as distinct identities; they are not blindly deduplicated by label. No ambiguous station is chosen by name.
- Browser UI uses one LazyColumn with stable page/line keys, compact accessible Back/Home/Refresh icons, no NET RADIO page/depth labels, and optional offline ISO flags for recognized country directory names. SERVER page UI remains unchanged.
- All eight locales updated with the radio-home accessibility string; no production English labels added in Kotlin.
- Verified Yamaha XML builder/parser commands, package, dependencies, permissions and source-selection behavior unchanged. No Radio Browser API or mandatory YTuner dependency.

## Tests

V075BrowserTest adds 14 cases: four-Favorites migration/persistence, multi-page aggregation/selection, single/empty menus, later-page rewind, duplicate labels, stale target rejection, bounds, stuck/skipped pages, timeout, directory/Back/Home/Refresh paths, ISO/alias flags and unknown/non-country exclusions.

V075UiTest adds six cases: 1–4 Favorites in one row with equal widths and unchanged minimum height, toolbar/Now Playing callbacks, removed depth/page UI, scrolling/selecting an unknown country, disabled navigation, and light/dark renders.

Existing NET RADIO station playback, metadata, Stop, path navigation, connection error isolation and foreground polling tests pass alongside SERVER, Tuner, Spotify, Volume, discovery, Settings, locales and launcher regression tests.

Robolectric native component renders at 411 × 891 dp: app/build/reports/ui/v075-radio-light.png and v075-radio-dark.png. They combine the Favorites row and browser for layout inspection; production displays Favorites on Home, not above the NET RADIO toolbar. These are automated renders, not physical-phone screenshots.

## Hardware limits

No v0.7.5 physical verification claimed. Test the first/last country and station entries, duplicate labels, Back/Home/Refresh, Now Playing/Stop, five-to-four Favorites migration and four tiles on the Samsung phone. Large or very slow catalogues can hit the explicit traversal bound. Unknown country/menu names remain usable without flags.
---

# v0.7.4 validation — 2026-09-28

VersionName **0.7.4**, versionCode **11**. Package unchanged: **com.styl15hh1.rn301controller**. No subsequent release work.

## Final result

- Command: `.work/build.ps1 test build lint --console=plain`.
- **BUILD SUCCESSFUL**, 1m20s; debug and release APKs assembled.
- **345 tests, 0 failures, 0 errors, 0 skipped**: all 318 prior cases plus 27 v0.7.4 cases (11 model/protocol, 16 Compose/Home). Existing tests adjusted only where Spotify Stop assumptions conflicted with the supplied evidence.
- Lint: **0 errors, 4 existing warnings**, no new warnings: OldTargetApi, AndroidGradlePluginVersion, InsecureBaseConfiguration, IconXmlAndPng. No toolchain/target/network-policy redesign in this release.
- All eight locales compile and pass matching-key/format/language-config checks: English, Spanish, German, French, Italian, Polish, Korean, Japanese. ResourceValidationTest: 3/3 passed, including launcher densities.
- Signature: apksigner **Verifies**, APK Signature Scheme **v2**, one signer.
- aapt confirms package com.styl15hh1.rn301controller, versionName **0.7.4**, versionCode **11**, minSdk26, targetSdk36.
- APK: `app\build\outputs\apk\debug\app-debug.apk`.
- Size: **12,760,186 bytes**.
- SHA-256: **2B7356A865D8CE84356C8EA93DEABE9B1BE105AEA98F468CAC046379E85BAE15**.
- Test report: app/build/reports/tests/testDebugUnitTest/index.html.
- Lint report: app/build/reports/lint-results-debug.html.

## Implementation scope

- ExpandableVolume owns only local remember state (default false). A full-width accessible header displays live native value and mute state, with a minimum 56 dp target. Fresh Home resets to collapsed. No command callback belongs to the expansion action.
- Existing VolumeControls, RotaryVolumeControl, RotaryTouchSession, LiveVolumeWriter and receiver volume repository code are unchanged. The existing pointer cancellation lifecycle handles removal during a gesture.
- Home order is global header, Volume, approved Favorites, active Now Playing, Sources. Existing optional recent-source shortcut remains. Favorites, Tuner and Sources tiles were not redesigned.
- Spotify card and full-player metadata use generic PlayerPresentation: trimmed case-insensitive deduplication, track/artist/unique-album hierarchy, actual playback state. Explicit Open Now Playing replaces the standalone Spotify chevron.
- Three symmetric Spotify control positions: Previous, authoritative Play/Pause, Next. Unknown playback shows a disabled center control with an unavailable-state description; it does not imply paused/playing. Spotify Stop capability and builder support removed; SERVER/NET RADIO Stop preserved. Spotify Shuffle/Repeat UI excluded.
- Generic InputLogo retains optional small/medium/large source-branding paths; no image fetch, album-art substitution, external API, dependency, permission or polling change.
- New expansion/mute labels translated into all existing eight locales.

## Test coverage and render inspection

V074PlayerTest verifies the supplied physical Spotify fixture (including URL_S and empty M/L), optional metadata, all requested trim/blank/case/duplicate combinations, distinct roles, unknown playback, Spotify Stop rejection and unchanged SERVER/NET RADIO Stop.

V074HomeUiTest exercises default/expand/collapse, zero requests from expansion, live collapsed volume/mute updates, unknown mute, fresh-Home reset, real VolumeControls +/-/Mute through the fake transport, rotary gesture through the unchanged live writer, gesture cancellation on collapse, explicit player navigation, authoritative playing/paused controls, unique/missing/long metadata, button symmetry and Tuner Home.

Existing v0.1–v0.7.3 protocol, repository, discovery, locale, launcher, volume concurrency, pointer, Tuner and browser regressions remain in the suite. Earlier broad Spotify-action tests were corrected to the supported four wire actions.

Phone-sized Robolectric native renders (411 × 891 dp), in app/build/reports/ui:
- home-v074-spotify-collapsed-light.png / dark.png
- home-v074-spotify-expanded-light.png
- home-v074-tuner-collapsed-light.png
- home-v074-spotify-long-dark.png
- home-v074-hierarchy-light.png / dark.png

Visually inspected collapsed/expanded Spotify, Tuner, long metadata and both themes. Favorites remain centered, section order is correct, no control clipping, long text wraps/ellipsizes deliberately, and playback controls are symmetric. These are simulated phone renders, not screenshots from the physical S23 Ultra.

The initial run exposed three new test-harness issues: the fixture had not enabled Tuner visibility, test selectors used names different from existing localized +/- descriptions, and a paused Robolectric clock did not advance the live writer delay. Corrected without changing production volume/Tuner behavior; focused rerun passed.

## Physical evidence and limits

The user's Basic_Status / Spotify Play_Info PASS, approved Tuner and rejected GET paths are recorded in YAMAHA_PROTOCOL.md and RN301_CAPABILITY_AUDIT.md. No new hardware probes or speculative PUTs were sent. Sound/speakers/Sleep/preset STORE remain PHYSICAL_FEATURE_XML_UNRESOLVED. New Home/player UI and source-logo parser extension still require the physical checklist; automated tests do not establish hardware verification.

Final build, test totals and signature results are recorded above. All new hardware-facing UI validation remains pending the physical checklist.
---

# v0.7.3 validation — 2026-09-28

VersionName **0.7.3**, versionCode **10**. Package **com.styl15hh1.rn301controller** unchanged. Remains in the 0.7.x beta series; no 0.7.4, 0.8 or stable-release work started.

## Final result
- Full command: `.work/build.ps1 test build lint --console=plain`.
- **BUILD SUCCESSFUL**, 1m28s; debug and release assembled.
- **318 tests, 0 failures, 0 errors, 0 skipped**: all 293 prior tests plus 25 new protocol/repository/UI tests.
- Lint: **0 errors, 4 existing warnings**, no new warnings: OldTargetApi, AndroidGradlePluginVersion, InsecureBaseConfiguration, IconXmlAndPng.
- All eight locale resources compile. ResourceValidationTest: 3 tests pass, covering matching string keys/format arguments, language config and launcher resources.
- APK signature: apksigner **Verifies**, APK Signature Scheme v2, one signer.
- APK: `app\build\outputs\apk\debug\app-debug.apk`.
- Size: **12,549,675 bytes**.
- SHA-256: **B2966E6D9B3EE1D2E34570079053A636E3EA9F431A0C2AE6C28F62E52C747397**.
- aapt confirms versionName0.7.3/code10, minSdk26, targetSdk36.
- No new runtime dependency, permission, service or background timer.

## Implementation and tests
Favorites heading now fills the section width and centers its text. Tile code/dimensions/spacing/persistence unchanged. HomeUiTest checks both heading text layout alignment and position, in light and dark themes. Rendered home-v073-light.png and home-v073-dark.png in app/build/reports/ui were visually inspected; approved tiles and controls retain their layout.

TuningRange generalizes the existing FM representation with an explicit band: FM Exp2/MHz, AM Exp0/kHz. Each band's regional Config is parsed independently. New typed commands SetBand, SetFmMode, TuneAm and SeekAm use the existing receiver-hosted reference XML. Repository actions require a connected, powered-on Ready tuner, validate the band/range, serialize commands and read receiver state after ACK. UI selection is never optimistic.

V073ProtocolTest covers exact band/mode/AM/seek XML, regional AM9 and AM10 grids, invalid values, frequency formatting, independent mode/signal state, AM RDS suppression, meaningful/deduplicated/missing RDS, optional opaque Clock_Time, firmware presence/absence and malformed XML. Populated RDS/clock fixture variations are synthetic tests, not claims about a captured station.

V073RepositoryTest covers FM/AM switching, presets preserved, AM direct/step/seek, wrong-band/off-grid rejection, actual readback after ignored ACK, Auto/Mono, failed PUT without disconnection, inactive/standby rejection, optional Config failure and cached firmware without added polling.

V073UiTest covers readback-driven band/mode selection, FM-only options hidden on AM, disabled controls, shared Home metadata/compact fallback, optional clock with independent reception status, and Receiver information firmware rendering.

Firmware is the optional System/Config/Version field already requested on connect. Cached diagnostic information is shown under Settings → About; no firmware update command or Home label added.

## Physical evidence and limits
User-reported v0.7.2 **PASS**: live Volume while moving, global header, Favorites layout/dimensions, Tuner redesign, Power/Mute/source controls. Prior pending notes below are historical.

New v0.7.3 commands: **IMPLEMENTED / UNIT TESTED / PHYSICAL TEST REQUIRED**. No new tuner PUT, Sleep PUT, Sound PUT, Speaker PUT or preset-storage PUT was sent to the actual receiver.

Read-only research on the previously configured receiver:
- Existing Main_Zone/Basic_Status returned RC0 with no Sleep field.
- Exact FHEM System/Basic_Status GET returned HTTP400.
- Existing Tuner/Play_Info returned Ready FM, preset3, Auto mode, Tuned Assert/Stereo Negate and empty RDS fields including Clock_Time.
These observations establish response fields only; they do not validate the new control operations.

Sleep: **DOCUMENTED_FEATURE_XML_UNRESOLVED**, withheld. No timer icon/sheet/selector, stored active state, local countdown, alarm or service. FHEM and receiver-hosted generic code share spaced minute tokens but disagree on the path; current reads do not establish a compatible state readback. Sound/EQ/Speaker/Memory research remains documentation-only.

Required phone/receiver tests: centered Favorites heading, FM↔AM, AM direct/step/seek with regional limits, FM Auto/Mono versus actual signal, presets/FM regressions, rich and absent RDS on Tuner/Home, firmware display, plus existing controls. Full checklist: PHYSICAL_TEST_CHECKLIST.md. No120-minute waiting test applies because Sleep is not implemented.

## Principal changed files
- app/build.gradle.kts (version).
- data/model/FmTuning.kt, TunerStatus.kt, ReceiverStatus.kt.
- data/protocol/YamahaCommand.kt, YamahaXmlBuilder.kt, YamahaXmlParser.kt.
- data/repository/YamahaRepository.kt.
- ui/ReceiverViewModel.kt, ReceiverScreen.kt, TunerScreen.kt, TunerControls.kt.
- New ui/TunerOptions.kt, TunerHomeInformation.kt, ReceiverInformation.kt.
- Eight values*/strings.xml sets: new tuner/firmware labels, removed superseded FM-only labels.
- New V073ProtocolTest.kt, V073RepositoryTest.kt, V073UiTest.kt; updated HomeUiTest.kt.
- README.md and all four requested docs: YAMAHA_PROTOCOL, VALIDATION, PHYSICAL_TEST_CHECKLIST, RN301_CAPABILITY_AUDIT.

Historical validation follows; older hashes/counts describe earlier artifacts.

# v0.7.2 validation — 2026-09-28

VersionName **0.7.2**, versionCode **9**, package **com.styl15hh1.rn301controller**. Implementation complete; physical retest required.

## Final build
- `.work/build.ps1 test build lint --console=plain`: **BUILD SUCCESSFUL**, final run 30 seconds. Debug and release assembled.
- **293 tests, zero failures/errors/skips** (272 baseline + 21 new). Tests run locally with fake transports/captured XML and Robolectric; no physical receiver dependency.
- Lint: **0 errors, 4 existing warnings**: OldTargetApi, AndroidGradlePluginVersion, InsecureBaseConfiguration, IconXmlAndPng. No added warnings.
- ResourceValidationTest validates all eight locales and format arguments plus launcher resources; Android resource compilation also passed.
- APK signature: apksigner verify --verbose **Verifies**, APK Signature Scheme v2, one signer.
- APK: `app\build\outputs\apk\debug\app-debug.apk`
- APK size: **12,528,643 bytes**.
- SHA-256: **B861FBB71CF99FA1F6D232EC5E49F39A4ED1C3BB8E4F5619EB4C4D9C5ECC8F6E**.
- Manifest checked with aapt: version 0.7.2/code9, minSdk26, targetSdk36.
- About still reads BuildConfig. No permissions, runtime dependencies, telemetry or new network services added.

## Changed behavior and coverage
LiveVolumeWriter uses a named 125 ms interval, one latest target and a conflated wakeup channel. Repository sampling occurs inside the operation mutex; YamahaHttpClient keeps serialized requests and a 125 ms minimum for live-volume writes (normal request spacing unchanged). No GET per live step. Release flushes a still-needed final target, suppresses duplicates, then reads actual receiver state. A live failure preserves local preview; a bounded final attempt can recover. Persistent failure remains a command error and cannot by itself disconnect the receiver.

LiveVolumeTest covers in-gesture writes, interval boundary, rapid conflation, slow requests, direction reversal, target sampling after lock wait, duplicate release suppression, early release, return to starting value after a live write, temporary/permanent failure, cancellation and unchanged gestures. DailyUseRepositoryTest adds real ViewModel/repository preview/failure/readback regression coverage and preserves existing power, mute, volume and source behavior.

StartupTest covers saved receiver first, no unnecessary scan on success, failed saved address falling back to existing discovery, no arbitrary discovered-device auto-connect, no-saved-address flow and rapid reconnect suppression.

V072UiTest checks centered groups of 1/2/3 Favorites (68 dp minimum), order/selected state, equal-size number-only presets (allowing one physical pixel of grid rounding), accessible selection, actual tuner metadata/absence, and header Connect visibility/actions. GlobalHeaderUiTest checks Home, Tuner, Settings, About, SERVER, NET RADIO and Now Playing retain one header with Power/Settings/status. Test fake transport dispatch is deterministic; no production dispatcher changes were made for tests.

Actual Compose rotary gesture tests from v0.7.1 remain green. The 220 dp size, full hit area, outside capture, center handling, 12-degree sensitivity, haptics and +/- are preserved.

Light/dark rendered Home images at 411×891 dp were visually inspected:
- `app/build/reports/ui/home-v072-light.png`
- `app/build/reports/ui/home-v072-dark.png`
Favorites fit centrally, active Now Playing follows them, and collapsed Sources remains below. Displayed sample artist/track are test fixture data only.

## Scope and physical evidence
User-reported v0.7.1 PASS: rotary gesture, +/-, Mute, Favorites, source selection, toolbar Power, connection display, collapsed Sources, Tuner, Spotify, communication. This supersedes historical pending notes below.

v0.7.2 live volume and changed layouts: **IMPLEMENTED / PHYSICAL RETEST REQUIRED**. Slow/fast/reversing rotation, final readback, external knob synchronization, failures/backgrounding, Favorites layouts, all header routes/reconnect and tuner controls need the physical phone/receiver checklist.

The capability audit uses reference code, official manual and prior read-only captures. No experimental receiver requests/writes were made. No preset-memory/tone/speaker/system candidate command was implemented. See RN301_CAPABILITY_AUDIT.md; no v0.8 work started.

## Principal changed files
- `app/build.gradle.kts`: version.
- `data/model/LiveVolumeWriter.kt`, `RotaryVolume.kt`: live scheduling and gesture completion.
- `data/network/YamahaHttpClient.kt`, `data/repository/YamahaRepository.kt`: serialized live transport and final reconciliation.
- `ui/ReceiverViewModel.kt`: gesture orchestration, polling gate, reconnect.
- `ui/ReceiverHeader.kt`, `ReceiverScreen.kt`, `QuickSourcesUi.kt`: global shell and Favorites.
- `ui/TunerScreen.kt`, `TunerControls.kt`, preset presentation/layout model: tuner hierarchy and compact grid.
- `res/drawable/ic_connect.xml`, eight `values*/strings.xml`: connection icon and mono status.
- Tests: LiveVolumeTest, V072UiTest, GlobalHeaderUiTest, DailyUseRepositoryTest, DailyUsePresentationTest, StartupTest, HomeUiTest.
- README, YAMAHA_PROTOCOL, PHYSICAL_TEST_CHECKLIST, VALIDATION, new RN301_CAPABILITY_AUDIT.

The historical records below describe earlier builds and are not the v0.7.2 APK metadata.

# v0.7.1 maintenance validation

Version: 0.7.1, versionCode 8. Package unchanged.
Status: **BUILD SUCCESSFUL / 272 TESTS PASS / PHYSICAL RETEST REQUIRED**.

Final validation (2026-09-27):
- Full Gradle build and lint: BUILD SUCCESSFUL in 53 seconds; debug and release assembled.
- 272 tests, 0 failures/errors, 0 skipped (240 existing + 32 new).
- Lint: 0 errors, 4 existing warnings: OldTargetApi, AndroidGradlePluginVersion, InsecureBaseConfiguration, IconXmlAndPng. No new warnings. The local Yamaha cleartext policy and adaptive/legacy launcher resources are unchanged.
- All eight locale resource sets/format arguments and launcher densities pass ResourceValidationTest.
- APK signature: apksigner verifies APK Signature Scheme v2, one signer.
- APK: app\build\outputs\apk\debug\app-debug.apk
- Size: 12,695,256 bytes.
- SHA-256: AC66D04BA21624F75985860405F9F01F393A56D8F08AE4849DA7C4DD9171A166
- Manifest: com.styl15hh1.rn301controller, versionName 0.7.1, versionCode 8, minSdk 26, targetSdk 36.
- About continues to use the actual BuildConfig version.
- No v0.8 functionality or receiver protocols added.

## Rotary diagnosis and correction

The original Compose handler accepted only an annulus: radius >= 20% and <= 58% of knob width. Touches near the number never began a gesture. Entering that center region or crossing the outer boundary during movement or release broke the loop; its finally block cancelled the entire preview, so finishRotation and the volume PUT were never called.

Before changing the gesture code, three actual Compose touch tests ran inside a verticalScroll parent: inner-knob rotation and center release FAILED (no final target); outer-ring rotation PASSED. The retained before-fix report is .work/v071-rotary-before.xml. This reproduces a concrete failure mechanism; it does not capture the user's exact physical finger trace. Canvas/parent scrolling did not block the valid outer-ring test.

RotaryTouchSession now owns coordinate-to-angle and gesture lifetime. The whole knob square accepts down; movement is captured even outside bounds. Within 2.5% of width from center, angle sampling pauses and rebases on exit without inventing rotation or cancelling the preview. Compose consumes each captured movement before the scroll parent; normal up finishes once, including at the center/outside. Multitouch, disabled lifecycle, system cancellation and backgrounding abort without writes.

Existing signed delta normalization, fractional accumulator, 12-degree sensitivity, native bounds, equality suppression, ViewModel preview, single final volume write and receiver readback are preserved. No Yamaha XML/protocol or repository code changed.

## UI and preferences

- 220 dp maximum responsive knob replaces 180 dp (+22.2%); minus/Mute/plus use one row.
- Toolbar Power precedes Settings, uses receiver state, localized action/state semantics and existing busy protection.
- Model and connection/IP share the compact header.
- Favorites precede Now Playing. Same quick_sources key, encoding, order, five-item limit and editor.
- Missing preference gets available Optical/Tuner/Spotify defaults; an existing empty or customized preference always wins. Unavailable entries remain stored but hidden from the shortcut row.
- Full Sources reuses SourceTiles and vm.source; remember (not rememberSaveable) makes it collapsed for each new Home.
- Eight locales updated; Spotify shortcut reuses themed artwork.
- Light/dark Home render tests use simulated Android API 35 at 411x891 dp. Generated images are app/build/reports/ui/home-v071-light.png and home-v071-dark.png. They were visually inspected: three default shortcuts fit one row, active Now Playing precedes collapsed Sources, and header/volume controls remain legible. Sample track metadata is test-only.

## Physical status

User-reported v0.7.0 PASS: +/- volume, Quick Sources, source selection and receiver connection.
User-reported v0.7.0 FAIL: rotary gesture; knob too small.
v0.7.1 rotary correction: **IMPLEMENTED / REQUIRES PHYSICAL RETEST**.
No physical receiver writes, phone touch verification or new physical PASS occurred during this maintenance work. Historical validation follows below.

## Test scope and reproducibility

New tests: RotaryTouchTest (12 actual Compose pointer tests), RotaryCoordinateTest (4 coordinate/lifetime tests), FavoritesTest (5 preference/default tests), MaintenanceUiTest (5 expansion/Power UI tests), HomeUiTest (2 full-screen light/dark renders); DailyUseRepositoryTest adds 4 coordinate-to-ViewModel/readback, unchanged-target and rapid-power tests. Existing 240 tests are retained.

Robolectric 4.17 and Compose UI testing are test tooling, not receiver/runtime services. Compose UI test-host activity is debug-only; release runtime dependencies are unchanged. Tests run with synthetic responses and no receiver. All eight locale resource sets are checked for matching keys and formatting; launcher density checks remain in the suite.

References for test/input infrastructure:
- https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures
- https://robolectric.org/getting-started/

Commands (the project-local helper configures JDK/SDK/Gradle):
- rtk proxy powershell -NoProfile -File .work/build.ps1 build lint --console=plain
- .work/sdk/build-tools/36.0.0/apksigner.bat verify --verbose app/build/outputs/apk/debug/app-debug.apk

Changed production files: RotaryVolumeControl.kt, new RotaryTouchSession.kt, VolumeControls.kt, ReceiverScreen.kt, new PowerAction.kt and ExpandableSources.kt, QuickSourcesUi.kt, AppSettings.kt, QuickSources.kt, new ic_power.xml, all eight strings.xml files and app/build.gradle.kts. Tests and README/PHYSICAL_TEST_CHECKLIST/VALIDATION are also updated.

---
# v0.7 validation report

Validated on Windows on 2026-09-27. Package: com.styl15hh1.rn301controller. Application label: historical pre-rebranding label. Version: **0.7.0 (7)**. minSdk 26, targetSdk 36, compileSdk 37; existing dependencies and Android permissions unchanged.

## Implementation

- RotaryVolumeControl is a reusable 180 dp Compose Canvas knob with a pointer-angle gesture, center dead zone, clockwise/counter-clockwise movement, wrapped signed deltas and fractional accumulation.
- RotaryVolumeController owns local preview/active/pending state independently of XML. Sensitivity is a named **12 degrees/native step** constant. The ViewModel connects it to authoritative receiver StateFlow.
- Final-only coalescing: pointer moves issue no network writes. A changed final target is sent once on release after a fresh status check; actual readback resolves the preview. No periodic debounce timer or automatic PUT retry.
- Native rotary guardrails are 1..99, narrowed by matching advertised bounds when present. These are documented application guards, not invented runtime bounds or a dB mapping. Configured receiver maximum clamping wins readback.
- Existing precise +/- commands remain unchanged. Compact buttons retain 48 dp targets. Rotary custom accessibility actions, descriptions/state semantics and arrow keys provide alternative input.
- Step crossings request platform SegmentFrequentTick feedback. No raw-motion vibration loop, vibration permission or custom haptic service.
- Quick Sources supports zero to five IDs, selection order and move-up ordering. Existing SettingsStore/SharedPreferences persists IDs and the last confirmed explicitly selected source. Unknown/unavailable saved shortcuts are filtered and removable.
- No startup source change is issued. The complete Sources grid remains.
- Tuner uses responsive one/two/three-column preset cards, actual current station/frequency and RDS, plus expandable secondary manual tuning. Home can show a compact tuner card.
- Existing RDS parser already covered Program_Service, Radio_Text_A/B, Program_Type, frequency and Tuned/Stereo flags. It is unchanged; new fixture/presentation tests verify blank omission and current-preset-only live names.
- Generic PlayerPresentation puts station/title first, removes duplicate secondary fields and collapses empty stopped/unknown players to a small row. Existing capability-driven controls remain.
- Home prioritizes Power, Volume, Quick Sources, full Sources and meaningful Now Playing. Removed redundant technical notices and reduced spacing.
- Eight-language resources, System default, appearance, receiver settings and About are preserved.

## Test coverage

**240 JVM unit tests pass**, zero failures/errors: all prior 200 plus 40 new tests.

| New suite | Tests |
|---|---:|
| RotaryVolumeTest | 17 |
| DailyUsePresentationTest | 13 |
| DailyUseRepositoryTest | 10 |

Existing suites retained: Discovery 14; NET RADIO navigation 19, protocol 13, repository 10; Protocol 26; Repository 8; ResourceValidation 3; SERVER protocol 15/repository 17; Startup 4; Tuner protocol 11/repository 6; v0.3 protocol 19/repository 13; v0.4 15; v0.5 UI state 6; ViewModel 1.

New cases cover:
- 0→10, 10→0, 359→1, 1→359 and signed atan2 coordinates.
- Fractional/exact/multiple/rapid steps, clamping, reverse after a bound, reported zero and matching/narrower native bounds.
- Idle external updates, stable active/pending preview, final target/readback, failed final write and cancellation.
- No movement/debounce-window writes; rapid motion produces one final target, not one request per step.
- Receiver maximum clamping, command failure without disconnect/retry and no write after a failed fresh read.
- Quick Source empty/single/multiple/order/restart/limit/unknown/encoded-value behavior and recent source success/failure.
- No startup source writes.
- RDS fields using the existing captured response grammar; blank metadata; current preset highlighting and station ownership.
- Active/paused/stopped/missing player data, source capabilities, generic hierarchy, compact idle state and small/large-font preset-grid policy.
- Existing locale-key/format parity, Korean/Japanese Unicode, vectors and launcher-density tests still pass.

These are JVM/domain/repository/ViewModel/resource tests. No emulator or instrumented Compose test was run. Actual pointer feel, scrolling interaction, TalkBack focus and haptic strength require the phone checklist. Resource compilation does not prove visual polish on hardware.

## Build and artifact

- Full Gradle build: **BUILD SUCCESSFUL**, final run **34 seconds**, debug and unsigned release assembly complete.
- Tests: **240 passed**, zero failures/errors.
- Lint: **0 errors, 4 existing warnings**, no new warning.
- APK signature: **Verifies**, scheme **v2**, **one signer**.
- APK package/version/label confirmed by aapt: com.styl15hh1.rn301controller, 0.7.0 (7), historical pre-rebranding label.
- APK: **app\build\outputs\apk\debug\app-debug.apk**.
- Size: **12,496,161 bytes**.
- SHA-256: **A417E0780124E4E2457E04C4793C60E990B25310A66FC471CFCDD67E3973F6D9**.

Commands:
- rtk proxy powershell -NoProfile -File .work/build.ps1 :app:testDebugUnitTest --console=plain
- rtk proxy powershell -NoProfile -File .work/build.ps1 build --console=plain
- apksigner.bat verify --verbose app/build/outputs/apk/debug/app-debug.apk
- aapt.exe dump badging app/build/outputs/apk/debug/app-debug.apk

Initial validation caught a duplicate hide_manual localization key; the tuner-specific key is now hide_manual_tuning. Obsolete UI strings and a plural-candidate wording warning were removed. The existing four baseline lint warnings are documented: OldTargetApi (target-37 LAN migration deferred), AndroidGradlePluginVersion (validated toolchain retained), InsecureBaseConfiguration (legacy LAN HTTP) and IconXmlAndPng (adaptive/legacy launcher resources).

All eight locale sets compile with matching translatable keys/format arguments; product and language names remain non-translatable defaults. No launcher assets or supplied artwork were modified in this milestone.

## Main files

Added:
- data/model/RotaryVolume.kt and PlayerPresentation.kt.
- data/settings/QuickSources.kt.
- ui/RotaryVolumeControl.kt and QuickSourcesUi.kt.
- drawable/ic_arrow_up.xml.
- RotaryVolumeTest.kt, DailyUsePresentationTest.kt and DailyUseRepositoryTest.kt.

Updated:
- YamahaRepository.kt, ReceiverViewModel.kt, AppSettings.kt and MainActivity.kt.
- ReceiverScreen.kt, VolumeControls.kt, SettingsScreen.kt, TunerScreen.kt, TunerControls.kt and PlayerScreen.kt.
- Eight string-resource sets and app/build.gradle.kts.
- README.md, YAMAHA_PROTOCOL.md, PHYSICAL_TEST_CHECKLIST.md and this report.

Protocol builders/parsers and existing source browser/player command implementations are unchanged. No public repository, YCast, custom station URL, DLNA infrastructure or new media source was created.

## Physical status

User-confirmed PASS: discovery, connection, source selection, Power/Standby, native +/-, Mute/Unmute, Tuner/presets/manual frequency, Spotify source/artwork, Settings and language/settings navigation, v0.6 UI functionality.

NET RADIO source activation, basic Yamaha/vTuner catalogue/browser operation and station playback: **PHYSICAL PASS**. This does not assert every path/pagination/slow-service/metadata/Stop edge case was tested.

SERVER: **IMPLEMENTED / UNIT TESTED / PHYSICAL PENDING** because no DLNA music server is configured. Neither PASS nor FAIL.

New v0.7 rotary, haptics, Quick Sources, preset layout and player polish: **PHYSICAL PENDING**. No receiver control command was sent to physical hardware during development.

Remaining uncertainty:
- Numeric manual settings do not prove all runtime limits or a Max-sentinel encoding; no verified configured-Max read path is available.
- No dB mapping is known or invented.
- Haptic delivery/strength and comfortable sensitivity depend on phone/system settings.
- The receiver changes volume on release; intermediate preview is intentionally local.
- Another controller can change volume while a gesture is active; the final user target wins, then actual readback reconciles.
- Arbitrarily sparse pointer samples cannot disambiguate movement exceeding half a revolution between events.
- Optional RDS depends on broadcast, receiver readiness and firmware. No station database or frequency-to-name guess is used.

See PHYSICAL_TEST_CHECKLIST.md for exact rotary, accessibility, Quick Sources, tuner, player and regression checks.
