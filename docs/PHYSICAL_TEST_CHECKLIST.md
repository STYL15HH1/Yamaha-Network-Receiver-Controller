# v1.1.0 physical validation

## Confirmed hardware findings - 2026-09-30

User-confirmed Yamaha R-N301 PASS: DLNA server discovery through the receiver, SERVER browsing, multi-level directory navigation, folders, track selection and actual end-to-end receiver playback. This supersedes earlier SERVER physical-pending notes. It does not mark every playback control, metadata edge case or new v1.1.0 behavior physically verified.

## Test the exact signed v1.1.0 APK

Use the APK and SHA-256 recorded in VALIDATION.md. After successful testing, publish that same file without rebuilding.

- [ ] Browse SERVER lists of 0, 1, 8, 9, 16, 17 and many entries. Confirm one continuous list with no menu-level/page controls.
- [ ] Select folders and tracks from early, middle and final windows, including duplicate names; verify correct selection and playback.
- [ ] Use local Search and Clear; select a filtered track. Search must not alter the receiver menu until selection.
- [ ] Test SERVER Back, Server root and Refresh; slow/busy/unavailable media and stale menus must terminate safely.
- [ ] App Home is absent on Receiver and visible on other screens. It returns directly without changing input, playback or receiver catalogue location.
- [ ] Distinguish App Home from Server root / Radio root by icon and spoken accessibility label.
- [ ] Regress Net Radio aggregation, local Search, station selection, Radio root, Back, Refresh, Now Playing and Stop.
- [ ] Retain SERVER Receiver / Now Playing navigation, metadata and Play/Pause/Stop/Previous/Next behavior.
- [ ] Check all eight languages, larger font sizes and disconnected states.
- [ ] Capture a new SERVER browser image and replace the Net Radio and other non-Home screenshots to show the new header/root actions. Keep existing captures until replacements are supplied.

Earlier milestone records follow.

# v1.0.0 release baseline

Version **1.0.0 (15)**, package **com.styl15hh1.rn301controller**. The user approved the current v0.7.7 functionality and visual design as the stable baseline. No new receiver commands or capabilities are introduced.

Later user reports confirm v0.7.5 Power, live rotary Volume, Mute, Favorites/Sources, Spotify playback/metadata, FM/AM Tuner/presets/manual tuning/seek/Auto/Mono/RDS, Net Radio/YTuner aggregation/playback/Stop and reconnection. Subsequent v0.7.7 physical visual feedback and final approval supersede older UI-pending notes. This does not invent new SERVER/DLNA edge-case or unresolved-command physical results.

The alphabet index was removed after physical feedback; current Net Radio uses ordinary scrolling and local Search. Volume/Sources expansion persists. European locales use “Net Radio”; Japanese/Korean retain their translations.

[Release preparation](RELEASE_PREPARATION.md) records current validation/signing blockers. Sections below are historical technical evidence, not the current release designation; earlier pending items are superseded only by explicit later results.

---

# v0.7.5 — pending phone/receiver checks

No new physical PASS is claimed for this release.

1. Upgrade with five saved Favorites: first four remain in the original order after restart. Check one through four tiles stay on one row, with equal widths for four; source selection remains correct.
2. Open NET RADIO and a multi-page country catalogue. Check the complete menu appears in one scrollable list, with no page/depth controls. Confirm known country flags and usable unknown names.
3. Select directories and stations from the beginning, middle and end of an aggregated list, including any same-named entries. Verify the intended receiver selection, playback metadata and Stop.
4. Use toolbar Back, Home and Refresh, then leave to Home/Now Playing. Check receiver header remains present.
5. Open a single-page menu, an empty menu, and an unavailable/slow catalogue. Confirm no endless loading, no misleading connection loss, and a usable Refresh action. Very large/slow menus may reach the documented 64-page/90-second bound.
6. Check light/dark themes, larger text and the selected app language. Briefly regress Tuner, Volume/Mute, Spotify, SERVER and saved-receiver connection.

All earlier physical findings remain valid historical evidence; v0.7.5 introduces no new XML commands.
---

# v0.7.4 physical verification

## Supplied hardware results (user report)

Receiver: Yamaha R-N301, 192.168.1.55.
- PASS: Main_Zone/Basic_Status HTTP 200 / RC=0; On, native 40, Mute Off, TUNER / Tuner / Src_Number 1.
- PASS: Spotify Play_Info HTTP 200 / RC=0; Ready, Play, KSK Mix House / Give Me More (both Track and Album); source Input_Logo URL_S populated, M/L empty.
- PASS: existing Tuner implementation approved by user. Preserve FM/AM, frequency, RDS/optional Clock_Time, signal, presets, Auto/Mono, manual tuning and seek.
- Working Spotify controls reported by user: Previous, Play/Pause, Next.
- PHYSICAL REJECTED PATH (HTTP 400): Spotify Shuffle and Repeat GET candidates; System/Sound/Balance and Equalizer Low/Mid/High; Main_Zone/Sound_Video/Tone Bass/Treble; Main_Zone/Speaker_Preout/Speaker_AB.
- XML UNRESOLVED: Bass, Treble, Balance, Speakers A/B, Sleep, preset STORE/MEMORY. Rejected paths do not mean absent physical features. User reports the original NP Controller also lacks these controls.

## After installing v0.7.4 — pending, not marked passed

1. Cold-start/reconnect: global model, connection and IP remain correct. Volume and Sources start collapsed; Favorites and active Now Playing appear between them.
2. Change volume/mute using the receiver while Volume stays collapsed: native value and mute summary update without opening it. Tapping expand/collapse alone must not change receiver sound.
3. Expand Volume: rotate slowly/quickly in both directions; confirm live steps, haptics, final reconciliation, +/- and Mute/Unmute. Collapse/reopen and navigate away/back; no stuck gesture or unexpected write.
4. Play Spotify with Track=Album: only one title row. Then a unique album: track, artist, album in order. Check long/missing metadata, dark/light mode and larger system fonts.
5. Verify Previous, Pause, Play and Next, receiver-authoritative icons/state, explicit Open Now Playing navigation and Back. No Spotify Stop/Shuffle/Repeat controls or fake album cover.
6. Brief regressions: Favorites/source switching, Tuner FM/AM/Auto/Mono/RDS/presets/manual tuning/seek, SERVER/NET RADIO player/browser, AirPlay source, Power, Settings/firmware and saved receiver/discovery fallback.

No new UI, rotary composition behavior or parser extension is claimed physically verified until this checklist is run. Do not probe unresolved sound/Sleep/preset-write paths.
---

# v0.7.3 physical checklist

## User-reported v0.7.2 PASS
- Live rotary Volume, including changes while the finger is moving.
- Global application header.
- Favorites layout and tile dimensions approved.
- Revised Tuner layout approved.
- Existing Power, Mute and source controls remain working.

These results supersede v0.7.2 pending notes below. Unreported edge cases are not automatically marked PASS.

## v0.7.3 — new controls IMPLEMENTED / PHYSICAL TEST REQUIRED
- [ ] Favorites heading centered for 1–5 favorites; tile size, spacing, icons, selection and persistence unchanged.
- [ ] Tuner FM → AM → FM selection; highlight follows receiver readback.
- [ ] AM frequency displayed in kHz; receiver-advertised regional range and step.
- [ ] AM direct frequency, one-step up/down and seek up/down.
- [ ] FM tuning, seek, preset list/recall and selected preset sizing still work.
- [ ] FM Auto / Mono changes receiver mode; actual Tuned/Stereo indication stays independent.
- [ ] AM hides FM reception options and old RDS information.
- [ ] FM station with RDS: Program Service, Radio Text A/B and Program Type appear when available.
- [ ] No-RDS station: compact fallback, no fabricated station/program labels.
- [ ] If any station returns Clock_Time, compare its opaque displayed text; capture populated response for future format verification.
- [ ] Home Tuner card reuses station/frequency/program metadata without duplicates.
- [ ] Settings → About → Receiver information: real firmware version, model/address and connection state.
- [ ] Existing discovery, reconnect, Power/Mute, live Volume, Favorites, Spotify, SERVER, NET RADIO, AirPlay selection, themes and languages regressions.

Sleep Timer: **NOT IMPLEMENTED — DOCUMENTED_FEATURE_XML_UNRESOLVED**. No timer UI or app-owned shutdown fallback. Do not test guessed PUTs. If compatible protocol is established in a future requested build, test read state, set30/cancel/set60, close/reopen app, receiver-owned expiry and manual power interaction; no routine120-minute wait required. These are future tests, not v0.7.3 controls.

## Read-only research observations, not new control PASS
2026-09-28: existing Main_Zone/Basic_Status returned RC0 without Sleep; family System/Basic_Status returned HTTP400; existing Tuner/Play_Info returned FM status with empty RDS Clock_Time. No receiver PUT was sent during development.

Historical checklists follow.

# v0.7.2 physical retest checklist

## User-reported v0.7.1 results
PASS: rotary gesture; volume +/-; Mute; Favorites; source selection; toolbar Power; connection display; collapsed Sources; Tuner; Spotify; receiver communication. These are user-reported results, not automated test claims. They supersede the historical v0.7.1 pending status below.

## v0.7.2 — IMPLEMENTED / PHYSICAL RETEST REQUIRED
All boxes below remain unverified on the real phone/receiver:
- [ ] Slowly rotate native 40 → 50 without lifting: receiver changes during movement.
- [ ] Fast 40 → 60: follows without a long backlog; reverse before release with no stale queued targets afterward.
- [ ] Release: final requested value and receiver readback match (or reflect configured receiver clamping).
- [ ] External physical volume knob: display syncs after gesture/readback.
- [ ] Temporary live-write failure: preview remains responsive, bounded final sync, useful nonintrusive error.
- [ ] Background/cancel: no remaining queued writes; return resynchronizes actual volume.
- [ ] Existing +/- and Mute still work; 220 dp knob hit area, outside capture, center crossings and haptics remain usable.
- [ ] Favorites groups of 1, 2 and 3: centered, taller, order/selection/persistence unchanged, light/dark and large fonts.
- [ ] Home, Tuner, SERVER, NET RADIO, Settings, About and Now Playing: one receiver header, correct model/status/IP, working Power and Settings.
- [ ] Connect with saved receiver: reconnects without entering Settings.
- [ ] Saved receiver unavailable: finite existing SSDP scan and explicit candidate selection; no arbitrary automatic connection.
- [ ] Manual address fallback, first launch and multiple discovered receivers still work.
- [ ] Tuner current band/frequency/preset and available RDS are in the information area; missing metadata stays absent.
- [ ] Preset buttons: equal size, number-only display, selected highlight and TalkBack selected state.
- [ ] Previous/next preset, manual FM tuning, seek and secondary Refresh still work.

No v0.7.2 physical PASS is claimed. Audit-only capabilities are not exposed as controls. Preset store and v0.8 settings remain unimplemented.

Historical results follow:

## v0.7.0 physical feedback and v0.7.1 retest

User-reported v0.7.0 results:
- PASS: native volume +/- physically changes receiver volume.
- PASS: Quick Sources, existing source selection and receiver connection.
- FAIL: rotary gesture does not change receiver volume; knob too small.

v0.7.1 rotary correction: **IMPLEMENTED / REQUIRES PHYSICAL RETEST**.

Retest on the real Samsung phone and R-N301:
1. Upgrade over v0.7.0: saved receiver, theme, language and customized Quick Sources/order remain.
2. Start rotary gestures near the number/center, inner face and outer tick area; clockwise increases and counter-clockwise decreases at 12 degrees/step.
3. Cross 359/0 degrees both ways, pass through the center, move slightly outside the knob and release at the center/outside. No jump, lost preview or Home scrolling during a captured drag.
4. A drag previews locally. Release writes once, then shows actual receiver volume. A tap/unchanged drag writes nothing. Backgrounding or multitouch cancels without a write.
5. Verify receiver-clamped maximum, reported zero, +/- precision, Mute and system haptics. No dB label is inferred.
6. Toolbar Power sends Standby from ON and On from STANDBY; rapid taps do not queue commands. Check TalkBack labels/state and disabled state when unavailable/busy.
7. Header groups model and Connected/IP without a large Power/connection card.
8. Favorites show existing saved order, including intentionally empty preferences. On a clean install, only available Optical/Tuner/Spotify defaults appear. Customize/reorder/restart and check unavailable-source filtering.
9. Three typical favorites fit horizontally at normal text size; larger text wraps accessibly. Spotify branding dims when unavailable in both themes.
10. Full Sources starts collapsed beneath Now Playing; expand, select an input, collapse, leave/re-enter Home and confirm it resets collapsed.
11. Recheck Tuner/presets/manual tuning, Spotify controls/metadata, working NET RADIO browsing/playback, Settings/languages/About and background/foreground polling.
12. SERVER core browsing and end-to-end playback are now user-confirmed PASS (2026-09-30); new v1.1.0 browser UX checks remain pending.
No v0.7.1 physical PASS is claimed.
# Physical validation: Yamaha Receiver Controller v0.7

## Latest physical results supplied for v0.7

PASS on the real R-N301: discovery, connection, source selection, Power/Standby, native +/- volume, Mute/Unmute, Tuner/presets/manual frequency, Spotify source/artwork, Settings and language/settings navigation, and v0.6 UI functionality.

NET RADIO source activation: **PASS**.
Yamaha/vTuner catalogue/basic browser operation: **PASS**.
NET RADIO station playback: **PASS**.
The physical receiver currently reaches its Yamaha/vTuner service. This does not verify every pagination/path/slow-service/metadata/Stop edge case in the historical checklist below.

SERVER: **VERIFIED** for receiver-mediated discovery, browsing, multi-level folders, track selection and end-to-end playback (user report, 2026-09-30).

## v0.7 acceptance — new UX physically pending

Install version 0.7.0 (7) over the existing signed debug build.

### Rotary volume

- Start at a low comfortable volume. Clockwise increases; counter-clockwise decreases.
- Try slow, fast, fractional and reversing movement; sensitivity is initially 12 degrees per step.
- Cross the angle boundary in both directions; no large jump.
- Center touches, leaving the ring, a second finger, screen navigation and backgrounding cancel safely without a late write.
- Step haptics are subtle, absent for sub-step/clamped movement and respect Android feedback settings.
- During rotation, local preview is stable despite polling. The receiver changes on release, not every pointer event.
- Final receiver/front-panel volume matches readback; repeated gestures do not queue targets.
- Test the configured receiver Max Volume at a low setting: actual receiver clamping wins.
- Reported zero, numeric guardrails and the separate Max sentinel do not cause unexpected upward jumps.
- Physical knob and remote changes appear while idle; after an active gesture the app reconciles with the actual receiver.
- Small +/- buttons still perform one native step; Mute/Unmute remains correct.
- Failure/offline during release: one command error, no automatic PUT retry/discovery, preview cleared; refresh/reconnect restores actual state.
- TalkBack custom increase/decrease actions, hardware keyboard arrows and 48 dp +/- alternatives work.
- Check phone portrait, narrow width, landscape/large text and both themes.

### Quick Sources

- Configure zero, one and up to five sources; only selectable sources can be added.
- Selection order persists; move-up changes order; removal works.
- Restart and change language/theme; shortcuts retain their order.
- An unavailable saved source is hidden from Home and removable in Settings.
- Quick and full-grid selection use the same receiver path.
- Last used updates only after confirmed successful selection; failed selection does not replace it.
- Restart never switches the receiver input automatically.

### Tuner

- Responsive preset cards are readable and easy to tap; current preset is highlighted.
- Preset selection, previous/next, frequency and actual current station remain correct.
- RDS Program Service, Radio Text A/B and Program Type appear only when provided.
- Live RDS from the current preset is never copied onto another preset.
- Manual tuning is accessible below presets through the manual-tuning action; all working tuning controls remain unchanged.
- Home tuner card updates with external tuning and opens Tuner without unnecessarily selecting the source again.

### Now Playing

- Spotify and NET RADIO active metadata/controls follow actual receiver state.
- Station/title is prominent; secondary metadata is not duplicated.
- Empty stopped/unknown player is a small row; full player remains accessible.
- Tuner shows station when available, frequency and current preset.
- Background/Settings stop unnecessary player/tuner reads.
- SERVER actual browsing and playback are now user-confirmed PASS (2026-09-30); the new browser presentation requires v1.1.0 acceptance.

Record phone/Android version, firmware, rotary sensitivity preference, haptic feel, configured-Max readback, language/theme and any failures.

---

Historical milestone findings follow; the latest PASS declarations above supersede only the specifically confirmed basic functionality.


## User-reported hardware results

Physical Yamaha R-N301 and Android phone:

| Release / check | Result |
|---|---|
| v0.1 connection, model detection, power-state/current-source reads, Spotify detection | PASS |
| v0.2 SSDP discovery, automatic IP detection and R-N301 identification | PASS |
| v0.2 source selection, Tuner screen, preset retrieval and selection | PASS |
| v0.3 volume minus/plus and native integer stepping | PASS |
| v0.3 manual tuner frequency controls, presets, source icons and Spotify artwork | PASS |
| v0.3 Mute | Historical FAIL: misleading Receiver Not Found state |
| v0.4 corrected Mute / Unmute, Main_Zone/Volume/Mute | **PASS — user physically verified** |

The successful v0.4 Mute result supersedes its earlier pending status. Volume examples remain 40/50 (native); dB mapping remains unresolved. No hardware-derived slider bounds are available. Earlier Spotify playback controls and seek behavior were not separately reported as passed.

v0.4 UI issues reported for this milestone: Spotify artwork inset/outline; Settings entry insufficiently visible. Investigation found the Settings vector had an empty path. v0.5 replaces it with the complete Material gear and fixes the top app bar placement.

## v0.5 results supplied for v0.6

- Power / Standby commands: **PASS**.
- Mute / Unmute: **PASS**.
- Discovery, connection, source selection, native volume integer +/- and tuner/presets/manual controls: **PASS**.
- Spotify source and clean replacement artwork: **PASS**. Artwork was slightly large; v0.6 reduces display size by 18%, pending visual verification.
- AirPlay artwork rendering: **PASS**. Duplicate embedded/generic label observed; v0.6 uses a symbol-only derived resource, pending visual verification.
- Settings gear visibility and navigation: **PASS**.
- **SERVER: VERIFIED** for core browsing and end-to-end playback (user report, 2026-09-30).
- These findings do not verify Spotify playback commands, automatic tuner seek, runtime locale switching, or new NET RADIO behavior.

## Historical v0.6 acceptance — basic operation and UI now PASS; unreported edge cases pending

Install 0.6.0 (6). Record phone/Android version, receiver firmware and service availability.

### UI and regression

- Power heading and state/action group centered; both On and Standby remain functional.
- Volume heading, native value, limit notice and Mute centered; equal-width +/- buttons. No guessed bounds or dB conversion.
- Compact and full Now Playing: source, metadata, state and controls centered; long strings/large fonts remain usable.
- Spotify is 18% smaller with unchanged transparency and selected/disabled treatment.
- AirPlay symbol has correct proportions and exactly one AirPlay label; check light/dark/disabled states.
- Gear/settings/locales/About remain functional; actual version is 0.6.0.
- Recheck already-passed discovery/connection/power/volume/mute/tuner/source operations.

### NET RADIO

1. Select from another source; confirm Basic_Status switches before list requests and initial loading is visible.
2. Record the actual dynamic root menu. Check empty/unavailable service without misleading receiver-not-found errors.
3. Enter multiple directory levels; verify exact row names, item types and breadcrumb path. Unknown rows must not play.
4. Test more than eight entries and partial last pages. Select correct stations on first, second and later pages.
5. Back button and Android Back return one receiver level; root Back leaves the screen. Home reaches root with bounded Return.
6. Station Direct_Sel + Play starts the intended station; actual Play_Info drives state. Confirm metadata fields and missing metadata.
7. Compact/full Now Playing show station/program/title/artist only when returned. Only Stop is exposed; verify Stop/readback.
8. Slow menus, unchanged Ready parent, Busy/loading and delayed playback terminate safely and allow refresh.
9. Interrupt Internet/catalogue access while LAN remains available: radio error must not disconnect the receiver or start SSDP discovery.
10. Switch input or menu with another controller before selection: stale list/source must be rejected. Record any remaining race.
11. Background during loading/navigation: work stops and no PUT repeats. Reopen/refresh current receiver menu.
12. Debug path-engine check (no Favorites UI): use a path captured from actual names; test later-page station, missing/duplicate component and finite timeout. No fuzzy station substitution.
13. Check Logcat: source activation, list status/layer/cursor/page, selection/path and Play_Info state. Avoid publishing private catalogue names.
14. Check actual receiver outage separately: status polling eventually marks unavailable and controls disable.

### Actual receiver catalogue observations

- Firmware:
- Root Menu_Status / Menu_Layer / Menu_Name:
- Root entries and attributes:
- Example complete station path as a list of exact names:
- Eight-slot paging / Current_Line / Max_Line:
- Loading duration and statuses:
- Play_Info fields returned:
- Direct_Sel + Play / Stop results:
- Service error response and receiver connectivity:
- Date / tester / pass-fail notes:

## Historical v0.5 implementation checklist

Artwork rendering and gear/navigation items below are superseded by the PASS results above; additional theme/locale/runtime checks remain pending. SERVER core discovery, browsing and end-to-end playback are now user-confirmed PASS (2026-09-30); unreported edge cases remain pending.

No SERVER state-changing requests were issued to the real receiver during this milestone. Protocol evidence comes from the inspected references and previously captured receiver-served controller code; JVM tests use synthetic fixtures.

### Artwork, settings and regression checks

- Install 0.5.0 (5) over the previous debug build; receiver preferences remain.
- Gear is visible at the top right without scrolling, including disconnected state and large text.
- Spotify has no separate background/inset or rectangular outline. Check normal, selected and disabled states in both themes.
- AirPlay uses the supplied white transparent graphic plus its label; shape/size and disabled dimming remain legible.
- Language and Appearance rows are obvious and tappable. Language dialog shows System default, eight native names and the current selection indicator.
- Select every language, restart the app and confirm persistence. Select System default, change Android system language and confirm the app follows it. Test Android 13+ platform settings and the API 26–32 compatibility path where available.
- About opens as a separate page, Back returns to Settings, version reads 0.5.0, and the GitHub profile opens externally.
- Recheck discovery, connection, power, native volume buttons, Mute/Unmute, source switching, tuner/presets and existing Spotify controls.

### SERVER acceptance tests - core browsing/playback verified; remaining edge cases pending

1. Select SERVER while another input is active: confirm source first, then load the receiver menu. Test slow startup and unavailable source.
2. No NAS/media server: show an honest empty/not-ready/error state, not Receiver Not Found.
3. One and multiple DLNA servers: display exactly the receiver's entries, with Unicode titles intact.
4. Enter folders several levels deep. Confirm container rows navigate and track rows play on the R-N301.
5. Albums/playlists are rendered as containers unless the protocol supplies a verified subtype; no fake album categorization.
6. Test lists of 0, 1, 8, 9, 16, 17 and many items. Confirm next/previous page boundaries, partial last windows, and correct page-relative item selection.
7. System Back returns one menu level; at root it leaves the browser. Home returns to layer 1. Verify no infinite loop if Return does not change the menu.
8. Select tracks on several pages. Confirm Direct_Sel followed by Play starts the selected track, not a neighboring one.
9. Play, Pause, Stop, Previous and Next: confirm actual receiver state and metadata readback.
10. Verify title/artist/album, missing metadata and unusual playback states in shared compact/full Now Playing.
11. Change sources using the physical remote or another controller: stale metadata clears and browser writes refuse a changed input.
12. Change receiver menu externally before tapping a row: refresh warning instead of selecting a stale slot. A race after preflight remains possible.
13. Stop/disconnect NAS while browsing or playing; reconnect NAS and refresh. Receiver connection must remain distinct from a failed media operation.
14. Test Busy responses, malformed/unsupported replies and slow timeouts: finite loading, retry available, controls do not freeze.
15. Background/reopen during browsing/navigation. No automatic replay of a pending PUT; refresh obtains the receiver's current menu.
16. Check Logcat/network behavior: no periodic list polling, conservative visible metadata reads, no phone-to-NAS media streaming, no cloud/API calls.
17. If the receiver itself disconnects, stale controls disable and normal status polling detects unavailability.

Record exact firmware, Menu_Status, layer/cursor/count semantics and response shapes for failures. Do not record private track libraries in public logs.
