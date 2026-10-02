# v1.2.0 final local validation — 2026-10-02

The owner identified the signed APK below as physically accepted on Yamaha R-N301. This cleanup changes documentation only, preserves the supplied screenshot bytes, and does not rebuild the accepted APK. No new physical receiver tests are claimed. Other models remain unverified.

This record supersedes intermediate v1.2.0 build hashes, test counts and pre-acceptance statements in earlier preparation notes.

## Accepted release artifact

- Application ID: `com.styl15hh1.rn301controller`
- VersionName / versionCode: **1.2.0 / 17**
- Original APK: `app/build/outputs/apk/release/app-release.apk`
- Local upload copy: `.work/release/v1.2.0/Yamaha-Receiver-Controller-v1.2.0.apk`
- Checksum file: `.work/release/v1.2.0/Yamaha-Receiver-Controller-v1.2.0.sha256`
- Size: **9,084,023 bytes**
- APK SHA-256: `E445D5FF4C24FE0B1AF35BE37D1FE8D682B39897CECED93B0B7E84B463A65FCC`
- Signing certificate SHA-256: `AB:2A:CE:5A:9A:33:D2:F9:EC:C1:D3:48:2D:FA:51:58:DD:AE:44:A5:89:79:FA:18:5E:F1:32:96:E0:1D:A0:92`
- Existing signature verified with `apksigner`; package/version verified with `aapt2`. Release is not debuggable. Signing configuration is unchanged.
- The upload copy must match the original hash exactly; it is copied, never rebuilt during this cleanup.

## Final automated validation

Command: `testDebugUnitTest lintDebug lintRelease`, using the existing local toolchain and signing-property helper. **BUILD SUCCESSFUL**, 1m 13s. No assemble task ran; the accepted APK was preserved.

- Complete unit suite: **477 tests in 50 suites; 0 failures, 0 errors, 0 skipped**.
- Debug lint: **0 errors, 9 warnings**.
- Release lint: **0 errors, 9 warnings**.
- Warnings concern the pinned target/Gradle versions, legacy local cleartext HTTP, unused strings and launcher XML/bitmap alternatives. No production code or resources were changed to suppress them.
- `git diff --check`: passed (only Git's LF-to-CRLF conversion notices).
- All application source/resource/test file hashes match the pre-cleanup baseline. Documentation links resolve locally.
- Screenshot metadata review found no GPS tags or private IP strings; images were not rewritten.

The complete unit suite is `testDebugUnitTest`; this project has no separate release unit-test task. Earlier focused-test runs and intermediate lint totals are superseded by this full run.

## Final implementation coverage

- Widget: Power / Standby, manual refresh, current-source metadata, zero-to-four favorites, centered one-to-three layout, four-tile full row, selection, resizing, light/dark, cache/offline handling and multiple instances. No continuous polling or volume controls.
- Compatibility Report: read-only GET probes, capability versus runtime readiness, model/firmware, Tuner/RDS/presets and network sources; Copy/Share export omits IP address, System_ID and other private/raw fields. Diagnostics do not enable other models' normal controls.
- Tuner presets: normalized FM/AM stored frequencies, malformed/missing title fallback, authoritative update through the existing refresh path, dynamic count and single-line smaller text. No new commands, scans, station-name cache or local frequency database.
- Tuner player: station/frequency row, compact status, one-line ellipsized Radio Text, consolidated selected-state controls and unchanged Previous/Next behavior. Program Type and clock are absent from the main card. Focused fixture measured 407 → 208 dp at 379 dp width, approximately 49% shorter.
- Existing R-N301 controls, network behavior, presets, Widget and Compatibility Report behavior were not changed by this documentation cleanup.

## Screenshots and privacy review

The three final JPEGs already use clean v1.2.0 filenames; no image edits or renames were necessary. Visual inspection of the supplied files found no legible local receiver IP in either Tuner header: both show R-N301 and Connected. This differs from the task's description of earlier IP-bearing captures. Widget content includes receiver model and track metadata; Tuner captures contain station/RDS text. See [screenshot inventory](screenshots/README.md).

## Repository and publication

Release code, resources, tests, docs and final screenshots remain unstaged for manual review. Local SDK/JDK/Gradle tooling, caches, logs, helpers and generated artifacts stay ignored. Temporary widget previews are removed after validation. No source/resources are removed. No commit, staging, tag, push, GitHub release operation or upload is performed.
