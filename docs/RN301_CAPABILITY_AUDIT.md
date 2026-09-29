# v1.0.0 release baseline

Version **1.0.0 (15)**, package **com.styl15hh1.rn301controller**. The user approved the current v0.7.7 functionality and visual design as the stable baseline. No new receiver commands or capabilities are introduced.

Later user reports confirm v0.7.5 Power, live rotary Volume, Mute, Favorites/Sources, Spotify playback/metadata, FM/AM Tuner/presets/manual tuning/seek/Auto/Mono/RDS, Net Radio/YTuner aggregation/playback/Stop and reconnection. Subsequent v0.7.7 physical visual feedback and final approval supersede older UI-pending notes. This does not invent new SERVER/DLNA edge-case or unresolved-command physical results.

The alphabet index was removed after physical feedback; current Net Radio uses ordinary scrolling and local Search. Volume/Sources expansion persists. European locales use “Net Radio”; Japanese/Korean retain their translations.

[Release preparation](RELEASE_PREPARATION.md) records current validation/signing blockers. Sections below are historical technical evidence, not the current release designation; earlier pending items are superseded only by explicit later results.

---

# v0.7.5 scope — 2026-09-28

No protocol capability is added or reclassified. The existing NET_RADIO List_Info, Page Up/Down, Direct_Sel, Return, Play_Info and Playback controls are reused unchanged.

The new application aggregation layer hides Yamaha pages from the user, retaining original page/line identity for revalidated selection. It is bounded to 64 pages and 90 seconds, including existing readiness waits. Large catalogues beyond that bound fail safely. Single-page menus require no page command. Duplicate labels are retained because the protocol does not provide a unique catalogue ID.

Catalogue access remains exclusively through the receiver. Compatible native catalogues or optional replacements such as YTuner can work through the same XML; YTuner is not an application dependency. No direct Radio Browser service, flag-image download, cloud credentials or protocol probing.

Country flags are optional local presentation; unsupported menu titles/country aliases simply show ordinary usable entries. Favorites maximum becomes four with ordered migration of existing five-entry settings. Source-selection commands are unchanged.

All prior physical findings and unresolved capabilities below remain unchanged. v0.7.5 aggregation, toolbar and four-tile layout await hardware verification.
---

# v0.7.4 physical evidence update — 2026-09-28

This section supersedes conflicting historical classifications below. Evidence is the user's supplied physical R-N301 test report at 192.168.1.55; no new probes or PUT experiments were run for this release.

| Capability / tested GET path | Physical result | Current classification / decision |
|---|---|---|
| Main_Zone/Basic_Status | HTTP 200, RC=0; On, native Volume 40, Mute Off, Input TUNER, Src_Name Tuner, Src_Number 1 | PHYSICAL PASS; control verifies the test environment |
| Spotify/Play_Info | HTTP 200, RC=0; Ready, Play, Track, Artist, Album, Input_Logo/URL_S | PHYSICAL PASS |
| Spotify/Play_Control/Shuffle | HTTP 400 | PHYSICAL REJECTED PATH; no Shuffle control |
| Spotify/Play_Control/Repeat | HTTP 400 | PHYSICAL REJECTED PATH; no Repeat control |
| System/Sound/Balance | HTTP 400 | PHYSICAL REJECTED PATH |
| System/Sound/Equalizer/Low | HTTP 400 | PHYSICAL REJECTED PATH |
| System/Sound/Equalizer/Mid | HTTP 400 | PHYSICAL REJECTED PATH |
| System/Sound/Equalizer/High | HTTP 400 | PHYSICAL REJECTED PATH |
| Main_Zone/Sound_Video/Tone/Bass | HTTP 400 | PHYSICAL REJECTED PATH |
| Main_Zone/Sound_Video/Tone/Treble | HTTP 400 | PHYSICAL REJECTED PATH |
| Main_Zone/Speaker_Preout/Speaker_AB | HTTP 400 | PHYSICAL REJECTED PATH |

HTTP 400 establishes rejection of the tested XML path only, **not absence of the physical feature**. In particular, three-band Equalizer paths do not establish R-N301 Bass/Treble wire commands.

| Physical feature | Current classification |
|---|---|
| Bass | PHYSICAL_FEATURE_XML_UNRESOLVED |
| Treble | PHYSICAL_FEATURE_XML_UNRESOLVED |
| Balance | PHYSICAL_FEATURE_XML_UNRESOLVED |
| Speakers A/B | PHYSICAL_FEATURE_XML_UNRESOLVED |
| Sleep Timer | PHYSICAL_FEATURE_XML_UNRESOLVED |
| Tuner preset STORE / MEMORY | PHYSICAL_FEATURE_XML_UNRESOLVED |

The user reports that the original Yamaha NP Controller also exposes none of these six controls. No alternative commands, Android-side Sleep fallback, or preset writes are implemented.

Spotify Previous / Play / Pause / Next are retained as the user's working set. Spotify Stop has insufficient R-N301 physical evidence: removed from the pre-existing capability exposure and rejected by the XML builder. SERVER and NET RADIO Stop remain unchanged. Shuffle/Repeat candidates are rejected; no alternative probing.

Input_Logo URL_S is source branding, not album art: /YamahaRemoteControl/Logos/logo0065.png. URL_M/L are empty. Generic NowPlaying now retains these optional paths without downloading them. See the supplied-response fixture and exact existing requests in [YAMAHA_PROTOCOL.md](YAMAHA_PROTOCOL.md).

The user reports the existing Tuner implementation physically verified and approved. This does not imply every optional station RDS field will be populated. v0.7.4 Home/player presentation still requires phone verification.
---

# v0.7.3 research update — 2026-09-28

Version policy: remain in the 0.7.x development/beta series. Future candidates below are research options for a separately requested 0.7.x build, not authorization to start 0.8 or 1.0. Historical v0.8 headings below reflect the earlier request only.

## Sleep Timer decision: DOCUMENTED_FEATURE_XML_UNRESOLVED

**Withheld:** no Sleep PUT, timer button/sheet, artificial timer state, local countdown, WorkManager, alarm or background shutdown service added.

Evidence reviewed:
- Official R-N301 manual [R-N301 manual, printed p17](https://th.yamaha.com/files/download/other_assets/6/332426/R-N301_om_RL.pdf): physical Sleep options Off,30,60,90,120 minutes.
- R1 R-N301 integration (pinned below) has no Sleep implementation.
- R2 FHEM 71_YAMAHA_NP.pm lines 1352–1363 sends **System/Power_Control/Sleep**, tokens **Off,30 min,60 min,90 min,120 min**. Its GET path is **System/Basic_Status** (line373); Sleep parser at1961. Lines1963–1964 even note a family-firmware issue retaining Sleep after manual power-off. This is family evidence, not an R-N301-specific successful exchange.
- D receiver-hosted generic web code ajxSetSleep, lines436–439, instead uses **Main_Zone/Power_Control/Sleep** for the main zone, with the same spaced minute tokens. The earlier audit incorrectly implied the token spacing differed: it does NOT; the incompatible/unconfirmed path is the issue. D includes generic AVR commands, so presence alone cannot certify a setting.
- Public GitHub searches for R-N301 Sleep/YAMAHA_AV, compatible Yamaha NP Sleep and R-N301 Sound/preset Memory produced no stronger model-specific request/response evidence. MusicCast/YamahaExtendedControl examples were excluded as incompatible generation evidence.
- A bounded, read-only check on the previously configured R-N301 address succeeded for the existing Main_Zone/Basic_Status request. Actual response has Power but **no Sleep field**. Missing is not equivalent to Off.
- The exact FHEM **GET System/Basic_Status** returned **HTTP400** on this device. It does not establish a usable Sleep readback path. No guessed GET paths or any PUT probes followed.

Exact research GETs (HTTP POST /YamahaRemoteControl/ctrl):
```xml
<YAMAHA_AV cmd="GET"><Main_Zone><Basic_Status>GetParam</Basic_Status></Main_Zone></YAMAHA_AV>
<YAMAHA_AV cmd="GET"><System><Basic_Status>GetParam</Basic_Status></System></YAMAHA_AV>
```
First: RC0, no Sleep. Second: HTTP400, empty body. Local evidence: .work/v073-get-status.xml, v073-basic-status.xml, v073-get-system-basic.xml, v073-system-basic.xml. These checks did not change power, source, volume, presets or settings.

No R-N301 Sleep GET or PUT is classified verified. Reference-only candidate paths above are **not safe-to-send recommendations**. Neither configured duration nor remaining duration is verified remotely; no exact remaining seconds are claimed. The next useful evidence would be a captured exchange from a known compatible controller while the user deliberately changes Sleep, or receiver-specific protocol documentation. Do not test either candidate PUT automatically.

## Tuner and firmware additions

Existing D callbacks establish SetBand (1690–1692), FM_Mode (1686–1688), AM/FM absolute frequency (1708–1762) and band-dependent seek (1678–1684). Existing captured regional Config defines the actual limits and steps; production does not hardcode the captured AM grid.

v0.7.3 implements these controls with GET reconciliation. Classification remains VERIFIED_RN301_XML based on references; new PUT behavior is **PHYSICAL TEST REQUIRED**.

A fresh existing Tuner/Play_Info GET returned Ready, FM, 10120/Exp2/MHz, preset3, FM_Mode Auto, Tuned Assert, Stereo Negate, and empty RDS fields including Clock_Time. Saved .work/v073-tuner.xml. This is read-only evidence of field existence, not proof that any new control worked. Clock_Time is parsed as optional opaque text; populated syntax, timezone and station availability remain unknown. No station was changed to obtain metadata.

Firmware comes from existing System/Config/Version. The earlier actual capture is 1.13/0.05. It is optional, not a connectivity requirement; no update command is introduced.

## Sound controls: separate feature and wire evidence

R2 is primarily derived from CRX-N560(D) (file header lines1–12). Generic commands are not gated by a verified R-N301 capability check. The model family list includes R-N301, but provides no per-command success evidence. R2 slider definitions564–567 use -10,1,10; GET375–378; PUT1479–1516; parsing1976–1990. Wire values are direct scalars, not Val/Exp/Unit.

| Feature | Physical R-N301 feature | R-N301 GET / PUT | Other-model reference GET / PUT | Wire/range/step evidence | Classification |
|---|---|---|---|---|---|
| Bass | Documented | Unknown / unknown | R3 AVR Sound_Video/Tone/Bass is not R-N301 proof | Physical ±10; R-N301 wire, step and Val/Exp/Unit unresolved | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN |
| Treble | Documented | Unknown / unknown | R3 AVR Sound_Video/Tone/Treble is not R-N301 proof | Physical ±10; R-N301 wire/step unresolved | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN |
| Balance | Documented | Unknown / unknown | R2 System/Sound/Balance: GET GetParam, PUT scalar | R2 -10…10 step1; R-N301 center/sign/step mapping unverified; physical L+10…R+10 | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN |
| Equalizer Low | Separate 3-band EQ not established | Unknown / unknown | R2 System/Sound/Equalizer/Low: GET GetParam, PUT scalar | R2 -10…10 step1, no units/exponent | OTHER_YAMAHA_MODELS_ONLY |
| Equalizer Mid | Not established | Unknown / unknown | R2 System/Sound/Equalizer/Mid: GET GetParam, PUT scalar | R2 -10…10 step1, no units/exponent | OTHER_YAMAHA_MODELS_ONLY |
| Equalizer High | Separate 3-band EQ not established | Unknown / unknown | R2 System/Sound/Equalizer/High: GET GetParam, PUT scalar | R2 -10…10 step1, no units/exponent | OTHER_YAMAHA_MODELS_ONLY |

Low is not asserted to be Bass; High is not asserted to be Treble. A three-band EQ including Mid is materially different from the documented two tone controls. No Sound GET probing or Sound PUT implementation was performed. These findings improve provenance/range evidence without upgrading R-N301 compatibility.

Speakers A/B: no stronger compatible XML found; still DOCUMENTED_RN301_FEATURE_XML_UNKNOWN. Preset Memory/store: no stronger XML found; still documented but unresolved. R1 Preset_Sel is recall; FHEM StorePreset variable is list parsing; D Scene_Load is scene recall. None is evidence for saving a tuned station.

Sources/pins and the full prior matrix follow. New findings above supersede conflicting historical notes.

# R-N301 capability audit — v0.7.2

Research date: 2026-09-28. This is an evidence audit, not a v0.8 implementation. No experimental PUT, preset overwrite, network setting change or firmware update was sent. Existing local captures were inspected; no new receiver requests were needed.

## Classification and evidence limits

Every capability below has exactly one classification:
- **A — VERIFIED_RN301_XML:** exact/sufficiently strong R-N301 API evidence; does NOT mean every command has been physically tested.
- **B — DOCUMENTED_RN301_FEATURE_XML_UNKNOWN:** receiver feature documented, compatible remote command unresolved.
- **C — OTHER_YAMAHA_MODELS_ONLY:** a command exists elsewhere; R-N301 applicability unestablished.
- **D — UNSUPPORTED_OR_NO_EVIDENCE:** insufficient evidence, not proof of physical impossibility.

The classification column uses the full classification names; prose also uses A/B/C/D shorthand. GET/PUT “unknown” means no R-N301 request is proposed. Existing receiver-hosted JavaScript includes generic AVR code: its mere presence is insufficient proof. Matching R-N301 response fields, configuration, UI and manual evidence are distinguished below. Never copy AVR paths or numeric encodings solely from that script.

## Sources

**[R1] Primary R-N301 implementation:** [rihokirss media_player.py, pinned 87c1f789](https://github.com/rihokirss/homeasisstant-rn301/blob/87c1f789bd2d13f8e965913108524f4f39c9ed26/custom_components/yamaha_rn301/media_player.py). Local snapshot: `.work/rn301-media.py`. Volume lines 262–265; preset recall 303–306; tuner parsing 371–435; previous/next numeric selection 446–474. Its separate API notes conflict with actual code on some paths/units; implementation and captured responses take priority.

**[R2] FHEM multi-model implementation:** [71_YAMAHA_NP.pm, pinned 04580b98](https://github.com/mhop/fhem-mirror/blob/04580b9847ce3f8fced54196ea2f943b2d8d2d80/fhem/FHEM/71_YAMAHA_NP.pm). Local snapshot `.work/71_YAMAHA_NP.pm`. Supports an R-N301 family but many commands originate in CRX-N560 and other players. Lines 375–378 sound reads; 1262 dimmer; 1352 sleep; 1463 power saving; 1479–1510 balance/equalizer; 1608 update status; 1616 network name; 1663 tuner. Model support is not command-by-command certification.

**[R3] Secondary AVR description:** [tryptophane RX-V475 desc.xml, pinned 2664e33](https://github.com/tryptophane/yamaha-remote/blob/2664e33af839dbd7ca9f7f8165e8120ca32bdfc3/resources/remote-api/YamahaRemoteControl.desc.xml). Local `.work/reference-desc.xml`, explicitly RX-V475. Tone parameter definitions 286–343, mappings 394–397, Sleep mapping 406. This is not an R-N301 descriptor.

**[R4] Home Assistant generic Yamaha integration:** [media_player.py](https://github.com/home-assistant/core/blob/dev/homeassistant/components/yamaha/media_player.py), reviewed 2026-09-27. Delegates through generic rxv device/zone APIs; does not establish the missing R-N301 tone, speaker or preset-memory wire commands. The dev URL is mutable. R1 remains the model-specific reference.

**[M] Official Yamaha:** [R-N301 owner's manual, RL edition](https://th.yamaha.com/files/download/other_assets/6/332426/R-N301_om_RL.pdf), English printed page numbers below; [official downloads](https://usa.yamaha.com/products/audio_visual/hifi_components/r-n301/downloads.html). Manual feature support does not prove network API support.

**[D] Previously captured R-N301 web interface:** `.work/receiver-home.html`, `.work/receiver-scr0.js`, `.work/receiver-scr1.js`, fetched read-only in earlier development from the user's receiver, paths `/JavaScripts/scr0.js`, `/JavaScripts/scr1.js`. Not external runtime dependencies.
- scr0 SHA-256: B8C0CED473BCBF2D243765F945721AE0491FF171D4DFB2CD5E2BD3DC6F38E863.
- scr1 SHA-256: FBFE19BEB7E2E26F31963BBC8EF4F5158F3023EE5B9AD26820EED76B49A61DBB.
- Tuner callbacks scr1 1678–1762; tuner config 1816–1848; tuner status/RDS 1956–2006; config GET 2022.
- Network standby GET/PUT 1478–1484; name GET/PUT 2043–2049, corresponding explicit settings in receiver-home.html.
- Sleep 436–439 and generic zone/scene/surround functions require additional caution.

**[C] Previously captured receiver XML:** `.work/physical-config.xml` identifies R-N301, version 1.13/0.05. Sanitized unit test fixtures `app/src/test/resources/rn301-tuner-config.xml`, `rn301-tuner-not-ready.xml`, `rn301-presets.xml` preserve tuner fields, regional tuning range and preset list. Capture filenames/values are evidence, not a reason to hardcode a receiver address, station or regional range. Earlier desc.xml returned 404 and Main_Zone/Config returned HTTP 400; neither is a dependable capability source.

## Tuner matrix

Paths below omit YAMAHA_AV. Play_Info means Tuner/Play_Info GET GetParam; configuration means Tuner/Config GET GetParam.

| Capability | Evidence / GET | Verified PUT / format or limitation | Class | v0.8 candidate |
|---|---|---|---|---|
| Current band/frequency | C, R1, D: Play_Info/Tuning/Band and Freq/Current (Val, Exp, Unit) | Read only; decimal = Val / 10^Exp | VERIFIED_RN301_XML | Existing |
| Direct FM frequency | C config + D ajxSetTunerFreq | Tuner/Play_Control/Tuning: Band FM, Freq/FM/{Val,Exp,Unit}; region-reported grid | VERIFIED_RN301_XML | Existing |
| Direct AM frequency | C config + D same callback | Band AM, Freq/AM/{Val,Exp,Unit}; captured 531–1611, step 9, Exp 0, kHz | VERIFIED_RN301_XML | Yes, physical test first |
| FM tune up/down | C regional step, existing physically working app | Read current/config, calculate one valid step, use verified absolute FM PUT; no invented literal Up/Down | VERIFIED_RN301_XML | Existing |
| AM tune up/down | C regional step + D absolute AM | Same derived one-step operation, using AM config | VERIFIED_RN301_XML | Yes, physical test first |
| Seek up/down | D scr0 Auto Up/Auto Down callbacks, scr1 1678 | Tuner/Play_Control/Tuning/Freq/FM/Val = Auto Up or Auto Down; AM branch exists too | VERIFIED_RN301_XML | FM existing; AM retest |
| FM/AM selection | C Band field, D 1690–1692 | Tuner/Play_Control/Tuning/Band = FM or AM | VERIFIED_RN301_XML | Yes |
| Saved preset list | C rn301-presets.xml, existing app | GET Tuner/Play_Control/Preset/Preset_Sel_Item; advertised item numbers/titles/RW, omit non-recallable entries | VERIFIED_RN301_XML | Existing |
| Current preset / recall | C Play_Info/Preset/Preset_Sel, R1 303–306, D 1694 | PUT Tuner/Play_Control/Preset/Preset_Sel = decimal preset number; receiver list authoritative, capacity up to 40 | VERIFIED_RN301_XML | Existing |
| Next/previous saved preset | R1 numeric selection, C list, physical app PASS | Derived adjacent advertised number, then numeric recall; no verified native Next/Previous literal required | VERIFIED_RN301_XML | Existing |
| Store / Memory current station | M pp20–21 | GET/PUT unknown; no trustworthy R-N301 store path | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| Overwrite preset | M p20 | GET/PUT unknown; no safe proposed command | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| Clear/delete preset | M p21 | GET/PUT unknown | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| Auto preset | M p19 | GET/PUT unknown; scan/storage can replace presets | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| FM Auto/Mono mode | C Play_Info/FM_Mode=Auto; D 1686–1688 and scr0 Auto/Mono | PUT Tuner/Play_Control/FM_Mode = Auto or Mono | VERIFIED_RN301_XML | Yes, retest |
| Tuned flag | C Play_Info/Signal_Info/Tuned | GET Assert/Negate; absence remains unknown | VERIFIED_RN301_XML | Existing |
| Stereo flag | C Play_Info/Signal_Info/Stereo | GET Assert/Negate; reception status differs from configured FM_Mode | VERIFIED_RN301_XML | Existing |
| Signal strength/quality level | No numeric field in captures/references | GET/PUT unknown; Tuned is not a signal-strength meter | UNSUPPORTED_OR_NO_EVIDENCE | No |
| RDS Program Service | C Meta_Info/Program_Service, R1, D 1997 | GET optional string; never synthesize a station name | VERIFIED_RN301_XML | Existing |
| RDS Radio Text A | C Meta_Info/Radio_Text_A, D 1999 | GET optional string | VERIFIED_RN301_XML | Existing |
| RDS Radio Text B | C Meta_Info/Radio_Text_B, D 2001 | GET optional string | VERIFIED_RN301_XML | Existing |
| RDS Program Type | C Meta_Info/Program_Type, D 1995 | GET optional string | VERIFIED_RN301_XML | Existing |
| RDS Clock Time | C Meta_Info/Clock_Time exists empty, D 2003 | GET field verified; populated format/timezone not captured | VERIFIED_RN301_XML | Optional raw read first |
| Additional PI, AF, RT+, numeric signal/RDS editing | No sufficient R-N301 evidence in inspected sources | No request proposed | UNSUPPORTED_OR_NO_EVIDENCE | No |

Captured FM grid is 8750–10800, step 5, Exp 2, MHz (0.05 MHz). It is one receiver's region, not a global constant. An unset RDS field is not a parser failure. “Not Ready” is distinct from a tuned station. Frequency seeking may temporarily report a nonnumeric value.

### Preset storage conclusion

Manual memory, replacement and clear operations exist physically; network XML remains unverified. R2 variables named StorePreset parse returned list entries; they are not a preset-storage PUT implementation. D ajxSetSystemMemory loads a Scene and is unrelated to tuner memory. R1 numeric Preset_Sel recalls a station; it does not save one. No store/overwrite/delete/auto-preset command or misleading UI button is added in v0.7.2.


## Audio and receiver settings matrix

Manual ranges below describe the receiver feature, NOT a verified wire encoding. “Unknown” must never be replaced with a guessed Val/Exp/Unit.

| Capability | Evidence / GET | PUT / range / encoding | Class | v0.8 candidate |
|---|---|---|---|---|
| Native volume | R1 Main_Zone/Basic_Status/Volume/Lvl, physical stepping PASS | Main_Zone/Volume/Lvl/{Val integer,Exp 0,Unit empty}; existing 1–99 guardrail; user-configured maximum may clamp | VERIFIED_RN301_XML | Existing |
| Mute | R1 + physical app PASS, Basic_Status/Volume/Mute | Main_Zone/Volume/Mute On/Off | VERIFIED_RN301_XML | Existing |
| Bass | M pp17,35; R-N301 GET unknown | Feature ±10 dB at 20 Hz; XML path, increment and numeric encoding unknown | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| Treble | M pp17,35; R-N301 GET unknown | Feature ±10 dB at 20 kHz; XML path, increment and encoding unknown | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| Balance | M pp17,35; R-N301 GET unknown | L+10…R+10; XML increment/center/encoding unknown | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| FHEM Low/Mid/High equalizer | R2 System/Sound/Equalizer/{Low,Mid,High} GetParam | Same paths scalar -10…10; not proof of R-N301 Bass/Treble mapping | OTHER_YAMAHA_MODELS_ONLY | No |
| FHEM scalar balance API | R2 System/Sound/Balance GetParam | Same path scalar -10…10; receiver compatibility unverified | OTHER_YAMAHA_MODELS_ONLY | No |
| AVR Bass/Treble API | R3 Main_Zone/Sound_Video/Tone/{Bass,Treble} / Basic_Status | Val -60…60, step 5, Exp 1, Unit dB; explicitly RX-V475 | OTHER_YAMAHA_MODELS_ONLY | No |
| Speakers A on/off | M pp8,16; XML GET unknown | PUT unknown; speaker bank selection, not channel routing | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| Speakers B on/off | M pp8,16; XML GET unknown | PUT unknown | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| Speakers A+B / both off | M independent bank switches; XML GET unknown | No verified aggregate command/value | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| Sleep timer | M p17; D generic zone sleep callback; R2 conflicting System path | Off,30/60/90/120 min feature; R-N301 path/spacing unresolved without actual response | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| Max Volume | M p35; not present in captured System/Config | Feature 1…99 step 1, Max; XML unknown | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| Initial Volume | M p35 | Off, Mute,1…99,Max; XML unknown | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| Network Standby | D explicit settings GET/PUT, matching documented feature | System/Misc/Network/Network_Standby: GetParam / On or Off | VERIFIED_RN301_XML | Yes, read first and physical retest |
| Auto Power Standby | M p32 | Off,20min,2/4/8/12h; input-dependent; XML unknown | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| Network Name | D explicit friendly-name callback + R2 same path | System/Misc/Network/Network_Name: GetParam / XML-escaped string; UI limit 1–15 characters | VERIFIED_RN301_XML | Yes, read first and retest |
| Firmware version | C System/Config/Version = 1.13/0.05 | GET System/Config only; no write | VERIFIED_RN301_XML | Yes, display only |
| Firmware update status | M pp35–37; R2 generic Yamaha_Network_Site/Status | No R-N301-specific status response; do not assume generic path | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| Generic network update status API | R2 System/Misc/Update/Yamaha_Network_Site/Status | GET GetParam; generic available/unavailable parser, no R-N301 capture | OTHER_YAMAHA_MODELS_ONLY | No |
| Dimmer | M p8 | Five brightness levels; XML unknown (R2 uses incompatible 1–3 range) | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| Pure Direct | No R-N301 control established in M/R1/C | No request proposed | UNSUPPORTED_OR_NO_EVIDENCE | No |
| AVR sound program / Straight | R3 Main_Zone/Surround/Program_Sel/Current | AVR Sound_Program/Straight values; not R-N301 evidence | OTHER_YAMAHA_MODELS_ONLY | No |
| Other R-N301 sound processing | No compatible path established | No request proposed | UNSUPPORTED_OR_NO_EVIDENCE | No |
| Loudness | No R-N301 control established | No request proposed | UNSUPPORTED_OR_NO_EVIDENCE | No |
| Left-only / right-only channel routing | No compatible command evidence | Not equivalent to balance or Speakers A/B | UNSUPPORTED_OR_NO_EVIDENCE | No |
| Input Trim | M p31 | -10…+10 dB step 1 feature; XML unknown | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |
| DC OUT power mode | M p35 | Physical setup feature; XML unknown | DOCUMENTED_RN301_FEATURE_XML_UNKNOWN | Defer |

### Important protocol distinctions

- R2 equalizer Low/Mid/High is not evidence that Bass/Treble use the same API. R3 tone values use tenths of dB; transferring that encoding to this receiver would be a guess.
- Balance feature range is documented, but a displayed L/R label does not establish signed integer wire encoding or a channel-isolation command.
- Sleep is deliberately **B**. D's generic zone callback emits spaced tokens such as “30 min”; R2's System callback uses a different path. Neither an actual R-N301 Sleep field nor command result is captured. Even receiver-hosted JavaScript contains AVR-only functions.
- Max/Initial Volume are setup limits, not a discovery mechanism for slider range. Existing native-volume safety and receiver clamping remain unchanged.
- Network Standby and Network Name have stronger, explicit network-settings GET/PUT evidence in the receiver-delivered settings UI, with matching family implementation. They are **reference-verified**, not newly physically tested. Keep runtime capability/error handling even for A commands.
- Firmware version is readable without initiating an update. D's Local_PC update-start code is not an update-status query and must not be used to discover status. No update functionality is proposed.
- Dimmer's five R-N301 levels contradict the three-level FHEM family setting; no conversion is assumed.
- General Spotify/SERVER/NET RADIO capabilities are documented in YAMAHA_PROTOCOL.md. This audit does not expand their implemented scope.

## Exact verified request examples

All requests are HTTP POST to `/YamahaRemoteControl/ctrl`, local receiver only, XML envelope shown below. GET/PUT here are Yamaha cmd attributes, not HTTP verbs. These are reference/capture-verified examples, not commands sent during this audit. New candidates remain absent from production code.

### Tuner reads (R1, C, D)
```xml
<YAMAHA_AV cmd="GET"><Tuner><Play_Info>GetParam</Play_Info></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="GET"><Tuner><Config>GetParam</Config></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="GET"><Tuner><Play_Control><Preset><Preset_Sel_Item>GetParam</Preset_Sel_Item></Preset></Play_Control></Tuner></YAMAHA_AV>
```
Play_Info returns Tuning/Band, Tuning/Freq/Current/{Val,Exp,Unit}, Preset/Preset_Sel, FM_Mode, Signal_Info/Tuned, Signal_Info/Stereo, and optional Meta_Info fields listed above. Config returns regional Range_and_Step. Empty RDS fields are valid. Preset list entries determine which numbers are actually available.

### Absolute tuning, seeking and recall (D 1678–1762; C; R1)
```xml
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Band>FM</Band><Freq><FM><Val>9740</Val><Exp>2</Exp><Unit>MHz</Unit></FM></Freq></Tuning></Play_Control></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Band>AM</Band><Freq><AM><Val>1134</Val><Exp>0</Exp><Unit>kHz</Unit></AM></Freq></Tuning></Play_Control></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Freq><FM><Val>Auto Up</Val></FM></Freq></Tuning></Play_Control></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Freq><FM><Val>Auto Down</Val></FM></Freq></Tuning></Play_Control></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Preset><Preset_Sel>3</Preset_Sel></Preset></Play_Control></Tuner></YAMAHA_AV>
```
AM seek uses AM in place of FM in the same D callback. Validate against the actual regional config. Relative tuning and preset navigation compose these verified absolute operations; they are not additional native XML verbs.

### Band and FM reception mode (D callbacks plus C response fields)
```xml
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Band>AM</Band></Tuning></Play_Control></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Band>FM</Band></Tuning></Play_Control></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><FM_Mode>Mono</FM_Mode></Play_Control></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><FM_Mode>Auto</FM_Mode></Play_Control></Tuner></YAMAHA_AV>
```
Read actual state from Play_Info after a successful ACK. Auto is a reception mode, not proof of stereo reception.

### Network settings and version (D 1478–1484,2043–2049; C)
```xml
<YAMAHA_AV cmd="GET"><System><Misc><Network><Network_Standby>GetParam</Network_Standby></Network></Misc></System></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><System><Misc><Network><Network_Standby>On</Network_Standby></Network></Misc></System></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><System><Misc><Network><Network_Standby>Off</Network_Standby></Network></Misc></System></YAMAHA_AV>
<YAMAHA_AV cmd="GET"><System><Misc><Network><Network_Name>GetParam</Network_Name></Network></Misc></System></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><System><Misc><Network><Network_Name>R-N301</Network_Name></Network></Misc></System></YAMAHA_AV>
<YAMAHA_AV cmd="GET"><System><Config>GetParam</Config></System></YAMAHA_AV>
```
Name is an example, not a requested rename. Config's Version field is read-only. Candidate GET responses must be checked for RC and actual expected field; absent or rejected fields must not be represented as supported. A PUT ACK does not prove the requested setting took effect; subsequent GET reconciliation is required.

## Evidence-based v0.8 recommendation

A small candidate scope is: explicit FM/AM selection, config-driven AM tuning, FM Auto/Mono, firmware-version display, and optional RDS clock display after capturing a populated field. Network Standby and Network Name are separately reference-verified candidates, with read-first capability checks and physical retests before claiming support.

Defer preset store/overwrite/delete/auto-preset, Bass/Treble/Balance, Speakers A/B, Sleep, Max/Initial Volume, Auto Power Standby, Dimmer and firmware update status until R-N301-specific request/response evidence exists. Do not implement Pure Direct, loudness, AVR sound modes or left/right routing on this evidence.

Next research can capture only known-safe GETs and the exact traffic generated by an existing compatible controller while the user intentionally performs an operation. Preserve pre-change values and separate storage/settings changes from ordinary playback. A screenshot/button in the obsolete application would prove UI intent, not the XML request. No screenshot accompanied the supplied text attachment in this workspace.

v0.7.2 implements none of these new candidate settings. Stop here; v0.8 requires a separate request.


## Existing native volume / mute examples (R1 and physical regression baseline)

These already-implemented commands complete the A-class audio rows above. They are not new v0.8 proposals.
```xml
<YAMAHA_AV cmd="GET"><Main_Zone><Basic_Status>GetParam</Basic_Status></Main_Zone></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Lvl><Val>50</Val><Exp>0</Exp><Unit></Unit></Lvl></Volume></Main_Zone></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Mute>On</Mute></Volume></Main_Zone></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Mute>Off</Mute></Volume></Main_Zone></YAMAHA_AV>
```
Basic_Status returns Volume/Lvl and Volume/Mute; PUT success uses Yamaha rsp=PUT and RC=0, followed by receiver readback. Native integer volume does not establish a dB conversion.
