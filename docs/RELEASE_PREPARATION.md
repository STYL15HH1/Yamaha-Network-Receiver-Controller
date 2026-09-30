# v1.0.0 release preparation

## Final release polish validation - 2026-09-29

The Spotify Now Playing header retains the existing wordmark and accessible source name while removing the duplicate visible label. Artwork dimensions, metadata, playback controls and navigation remain unchanged. Signing configuration and release identity are unchanged.

- Full unit suite: **388 passed**, 39 classes, zero failures/errors/skips; includes two new Spotify header regression tests.
- All four resource validation tests pass, including all eight locales and launcher densities.
- Gradle testDebugUnitTest assembleRelease lint lintRelease: **BUILD SUCCESSFUL**, 3m 44s.
- Debug and release lint: **0 errors / 7 existing warnings** each; no new suppressions.
- Final APK: pre-rebranding signed v1.0.0 APK (historical artifact); **9,011,407 bytes**.
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
- Final APK: pre-rebranding signed v1.0.0 APK (historical artifact)
- Size: 9,011,407 bytes.
- apksigner: Verifies; one signer; v2 true; v1/v3/v3.1/v4 false.
- Certificate SHA-256: AB:2A:CE:5A:9A:33:D2:F9:EC:C1:D3:48:2D:FA:51:58:DD:AE:44:A5:89:79:FA:18:5E:F1:32:96:E0:1D:A0:92 (exact required match).
- APK metadata: versionName 1.0.0, versionCode 15, application ID com.styl15hh1.rn301controller, minSdk 26.
- APK remains ignored by Git. No commit, push, tag or GitHub Release was made.
- The previous full 386-test validation was not repeated. Signing-only build/configuration checks passed.
- Remaining publication concern: supplied artwork redistribution rights, as documented below.

The existing local build helper deliberately uses an isolated Gradle home without signing credentials. Signed builds must use the owner's global Gradle home instead; do not copy secrets into the repository or helper. Configuration caching was disabled for these signing validation builds.


### Initial release preparation record (before signing)


## Status

Prepared and validated locally; **not ready for public APK publication** until the two blockers below are resolved. No GitHub release, tag, push, signing key, or credentials were created.

- VersionName: **1.0.0**
- VersionCode: **15**
- Application ID / namespace: **com.styl15hh1.rn301controller**
- minSdk: **26**; targetSdk: **36**; compileSdk: **37**
- Approved v0.7.7 UI/functional baseline preserved. No production Kotlin, protocol, ViewModel, networking or receiver behavior edits were made during release preparation.
- Two pre-existing working-tree changes (VolumeControls.kt and VolumeValuePolishTest.kt) belong to the final v0.7.7 polish and were retained.

## Full validation

Command: Gradle **build assembleRelease lint --console=plain** through the existing local toolchain helper.

- BUILD SUCCESSFUL, 2m 7s.
- Complete unit suite: **386 tests, 38 test classes, 0 failures, 0 errors, 0 skipped**.
- ResourceValidationTest: all four cases pass, including matching resource keys/format arguments, eight language tags and icon density checks.
- Eight application locales: English (default), Spanish, German, French, Italian, Polish, Korean, Japanese.
- Debug and release APKs built.
- Additional **lintRelease signingReport**: BUILD SUCCESSFUL, 26s.
- Both debug and release lint: **0 errors / 7 warnings**, no suppressions added.
- aapt2 inspection confirms the release package, version 1.0.0/code 15, minSdk 26 and targetSdk 36. No application-debuggable flag is present. All required translated locales are packaged; dependencies also carry other locales, which are not additional app translations.

Existing warnings are retained rather than changing the frozen application:
1. OldTargetApi: targetSdk 36.
2. AndroidGradlePluginVersion: newer Gradle available; pinned 9.6.0 retained.
3. InsecureBaseConfiguration: cleartext is required by the legacy receiver; runtime address/endpoint restrictions remain.
4–6. UnusedResources: ui_selected, ui_not_selected, band_unavailable.
7. IconXmlAndPng: adaptive launcher XML plus bitmap fallbacks.
The release Kotlin compilation also reports a redundant toFloat conversion in RotaryVolumeControl.kt. This is a nonblocking pre-existing warning; interaction code was not changed.

## Initial unsigned artifact and signing requirements

Unsigned build output:
**app/build/outputs/apk/release/app-release-unsigned.apk**

This is build-validation output, **not an installable public release artifact**. It has not been renamed to the preferred public artifact name.

- Gradle signingReport: release **Config: none**.
- No persistent release keystore or signing configuration was found in the project.
- Only the development debug configuration is signed with the local Android debug key.
- apksigner verify --verbose on the release APK returned exit 1: **DOES NOT VERIFY; Missing META-INF/MANIFEST.MF**. This expected failure confirms that the APK must not be distributed as signed.
- No arbitrary key, alias or password was generated.

The owner must supply/choose:
1. A persistent release keystore, stored outside version control and securely backed up.
2. The intended signing key alias and certificate identity.
3. Keystore and private-key passwords through a secure local/CI secret mechanism, not committed files or chat.
4. A release signingConfig wired to buildTypes.release in Gradle, using the supplied key and secrets.
5. Long-term key custody and backup so future in-place upgrades use the same signing identity.

Once configured: rebuild assembleRelease, verify the signature/certificate and package/version, and copy the signed APK to an ignored distribution location as **Yamaha-Receiver-Controller-v1.0.0.apk**. Do not substitute the debug APK. Existing debug-key installations require uninstall/reinstall when switching signing identity; local preferences are then removed.

## Public repository audit

Scope: current tracked source/resource/documentation files, working tree, local project signing-file names, Gradle dependencies, manifest and network implementation. This was a pattern-based audit, not a guarantee of exhaustive secret detection or a Git-history purge.

- No credential-like assignments, private-key markers or common token signatures were found in tracked text.
- No tracked local.properties, keystores, APKs, generated build directories, local research directory, logcat file or original Image/ artwork.
- Local local.properties, diagnostic logcat and .work toolchain/research files remain on disk, ignored; technical evidence was not deleted.
- No release-key file was found in the project. SigningReport identifies only the external development debug keystore.
- The manual-address UI contains 192.168.1.55 as a placeholder, not a configured default or connection target. Saved address defaults to empty. SSDP 239.255.255.250 is a standards multicast address. Private fixture/documentation addresses were retained.
- Historical validation output paths were made project-relative. Author STYL15HH1/profile references are intentional public attribution.
- .gitignore now also covers .keystore/.p12/.pfx/private-key files, signing properties, local .env files and distribution APK/AAB/APKS artifacts. Duplicate Image/ ignore rule removed.
- No analytics/advertising/telemetry or account/authentication dependencies are present. Receiver requests are local/private IPv4; About opens the author profile in a browser when requested.
- Generated artifacts remain ignored after the full build. No production behavior was changed and no source evidence was silently deleted.
- Before publishing, commit only reviewed source/docs; do not upload the entire local project directory with ignored files.

## Asset / license blocker

The root source license is MIT. The included Material Icons Apache 2.0 license is present. Existing supplied Spotify, AirPlay and Yamaha launcher artwork is tracked as derived resources and attributed in THIRD_PARTY_NOTICES.md.

The existing evidence does **not** establish a redistribution license or permission for those supplied brand assets. Attribution and derivative hashes do not establish rights. Owner review/clearance is required before public distribution. No replacement assets were fetched and no legal permission is inferred. See [third-party notices](../THIRD_PARTY_NOTICES.md).

## Release notes and screenshots

[Proposed GitHub release notes](RELEASE_NOTES_v1.0.0.md) are prepared but unpublished. [Screenshot slots](screenshots/README.md) are ready; no final phone screenshots were supplied or fabricated.

## Physical scope

The user approved v0.7.7 as the stable baseline. Prior user-reported hardware findings remain valid; this release preparation made no receiver probes and does not claim new physical tests. No unsupported features or unresolved XML paths were enabled.
