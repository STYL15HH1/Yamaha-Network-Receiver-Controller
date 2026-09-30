## v1.1.0 SERVER browser orchestration

Existing SERVER XML commands and parsers are unchanged. The same bounded aggregation adapter used for NET RADIO now wraps the separate SERVER page browser. It reads List_Info, traverses existing Page Up/Down commands, preserves page-local Direct_Sel identities and checks the receiver window again before selection. Track selection retains Direct_Sel followed by Play.

Both browser operations allow at most 64 pages and 90 seconds. SERVER previously used a 15-second operation deadline; aggregation now shares the existing NET RADIO deadline. Readiness retry counts, page transition checks, catalogue-root Return limits and stale-menu validation remain in the protocol-specific page browser. Equal names remain separate items by original page and line. Single-page menus issue no page commands. Search is local filtering only.

App Home is application navigation only. Server root and Radio root retain their existing bounded Cursor Return behavior.

# v1.0.0 release baseline

Version **1.0.0 (15)**, package **com.styl15hh1.rn301controller**. The user approved the current v0.7.7 functionality and visual design as the stable baseline. No new receiver commands or capabilities are introduced.

Later user reports confirm v0.7.5 Power, live rotary Volume, Mute, Favorites/Sources, Spotify playback/metadata, FM/AM Tuner/presets/manual tuning/seek/Auto/Mono/RDS, Net Radio/YTuner aggregation/playback/Stop and reconnection. Subsequent v0.7.7 physical visual feedback and final approval supersede older UI-pending notes. This does not invent new SERVER/DLNA edge-case or unresolved-command physical results.

The alphabet index was removed after physical feedback; current Net Radio uses ordinary scrolling and local Search. Volume/Sources expansion persists. European locales use “Net Radio”; Japanese/Korean retain their translations.

[Release preparation](RELEASE_PREPARATION.md) records current validation/signing blockers. Sections below are historical technical evidence, not the current release designation; earlier pending items are superseded only by explicit later results.

---

# v0.7.5 — logical NET RADIO menus, unchanged XML

VersionName 0.7.5 / versionCode 12. No Yamaha XML builder/parser command changes in this release.

NET RADIO still exclusively uses the existing receiver HTTP/XML interface at /YamahaRemoteControl/ctrl. Neither YTuner nor a particular catalogue provider is required; no direct Radio Browser API or external flag service is used.

## Automatic page aggregation

AggregatingRadioBrowser wraps the existing NetRadioMediaBrowser. It reads Ready List_Info windows, rewinds a retained later cursor to the first page, then uses the existing Page Down operation to collect the menu in receiver order. The UI receives one aggregated MediaList and renders it in a LazyColumn. SERVER keeps its existing page UI and raw adapter.

Bounds: at most 64 pages / 512 wire slots, with a 90-second operation timeout; existing Ready/Busy/Loading retries and 700 ms post-PUT delay remain unchanged. Each page must advance exactly one position while source, menu layer, title and reported entry count remain consistent. Non-advancing, skipped, changed, unavailable or oversized menus fail explicitly rather than looping or claiming a partial menu is complete. Menus of eight or fewer entries issue **no page command**.

Each aggregated row retains originPage and the original Line_1..Line_8 index. Selection returns to the row's original page, checks the menu identity and target page contents, then delegates to the existing adapter, which revalidates before Direct_Sel. A changed page is rejected rather than selecting a different station.

Identical labels are not unique station IDs: same-named entries across page boundaries are retained as separate page/line identities and LazyColumn keys. Repeat observation of a page is rejected. A successful station selection that leaves the menu unchanged reuses the aggregate instead of needlessly scanning it again. Directory selection, Back, Home and Refresh rebuild the target menu. Back's root check uses a raw window without scanning the menu being left.

Unchanged commands:
- GET NET_RADIO/List_Info = GetParam
- PUT NET_RADIO/List_Control/Page = Down / Up
- PUT NET_RADIO/List_Control/Direct_Sel = Line_1 .. Line_8
- PUT NET_RADIO/List_Control/Cursor = Return
- GET NET_RADIO/Play_Info = GetParam
- PUT NET_RADIO/Play_Control/Playback = Play (after station selection) / Stop

Existing exact XML fixtures and provenance in the historical sections remain authoritative. Ready-state handling, station-playback confirmation, Now Playing metadata and Stop remain in the existing implementation.

Country flags use offline ISO alpha-2 names plus a small alias table, only for directory entries in recognized country menus. Flags are decorative regional-indicator Unicode; no country name or wire value is altered. Unknown names remain selectable without a flag.

New aggregation/UI behavior requires physical receiver testing; no new physical-verification claim is made.
---

# v0.7.4 — supplied physical findings and presentation scope

Evidence below comes from the user's real R-N301 at 192.168.1.55. No new network commands or discovery changes were introduced. HTTP transport remains POST /YamahaRemoteControl/ctrl, with GET/PUT declared by the YAMAHA_AV cmd attribute.

## Known-good environment control — PHYSICAL PASS

```xml
<YAMAHA_AV cmd="GET"><Main_Zone><Basic_Status>GetParam</Basic_Status></Main_Zone></YAMAHA_AV>
```

HTTP 200, RC="0"; Power On, Volume 40 (native), Mute Off, Input TUNER, Src_Name Tuner, Src_Number 1. The endpoint worked during the rejected-path tests below. Native volume remains native: no dB conversion.

## Spotify Play_Info — PHYSICAL PASS

```xml
<YAMAHA_AV cmd="GET"><Spotify><Play_Info>GetParam</Play_Info></Spotify></YAMAHA_AV>
```

HTTP 200, RC="0". Representative actual structure:
```xml
<YAMAHA_AV rsp="GET" RC="0"><Spotify><Play_Info>
  <Feature_Availability>Ready</Feature_Availability>
  <Playback_Info>Play</Playback_Info>
  <Meta_Info>
    <Artist>KSK Mix House</Artist><Album>Give Me More</Album><Track>Give Me More</Track>
  </Meta_Info>
  <Input_Logo>
    <URL_S>/YamahaRemoteControl/Logos/logo0065.png</URL_S><URL_M></URL_M><URL_L></URL_L>
  </Input_Logo>
</Play_Info></Spotify></YAMAHA_AV>
```

Fixture: app/src/test/resources/rn301-spotify-physical.xml, transcribed from the user-supplied response. Parser preserves Ready, raw Playback_Info, artist/album/track and source-logo paths; empty URL_M/L become null. Unknown playback stays UNKNOWN. URL_S is **input/source branding, not album artwork**. Paths are stored only; no fetch or external artwork lookup.

Metadata presentation trims strings, omits blank rows and deduplicates case-insensitively in track → artist → album order. The physical example displays Give Me More once, followed by KSK Mix House. Parser data remains intact; presentation deduplication does not discard receiver evidence.

## Retained Spotify controls

All use POST /YamahaRemoteControl/ctrl and the existing XML:
```xml
<YAMAHA_AV cmd="PUT"><Spotify><Play_Control><Playback>Skip Rev</Playback></Play_Control></Spotify></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Spotify><Play_Control><Playback>Play</Playback></Play_Control></Spotify></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Spotify><Play_Control><Playback>Pause</Playback></Play_Control></Spotify></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Spotify><Play_Control><Playback>Skip Fwd</Playback></Play_Control></Spotify></YAMAHA_AV>
```

Previous, Play, Pause, Next respectively. Expected acknowledgment remains PUT RC=0; state is refreshed and receiver Playback_Info remains authoritative. Existing R-N301 integration/FHEM provenance is recorded in the historical command sections below; the user confirms these as the working control set. No new control XML was invented or changed.

**Spotify Stop is excluded in v0.7.4**, including the builder and player capability guard. Historical references below mentioning five Spotify actions are superseded. SERVER/NET RADIO Stop are unaffected. No Shuffle/Repeat UI or writes.

## PHYSICAL REJECTED GET paths

The user tested GET requests for:
- System/Sound/Balance
- System/Sound/Equalizer/Low
- System/Sound/Equalizer/Mid
- System/Sound/Equalizer/High
- Main_Zone/Sound_Video/Tone/Bass
- Main_Zone/Sound_Video/Tone/Treble
- Main_Zone/Speaker_Preout/Speaker_AB
- Spotify/Play_Control/Shuffle
- Spotify/Play_Control/Repeat

Each returned HTTP 400. This proves only that these exact tested paths were not accepted. It does not prove that physical sound/speaker features are absent or authorize trying alternative PUT paths.

Bass, Treble, Balance, Speakers A/B, Sleep and Tuner preset STORE/MEMORY are **PHYSICAL_FEATURE_XML_UNRESOLVED**. The user observes that the original NP Controller lacks these controls too. No new probes, preset write or Android Sleep fallback. Existing preset recall and all approved Tuner controls remain unchanged.

## Volume presentation only

Home Volume is collapsed by default; its live summary is native value plus mute state. Toggling the header performs no request. Expanded content reuses the unchanged 220 dp rotary, precision buttons and mute, with the existing 125 ms serialized/conflated live writer and reconciliation. No protocol changes, limits or conversions were introduced.
---

# v0.7.3 tuner / firmware extension

Status: new commands **IMPLEMENTED / UNIT TESTED / PHYSICAL TEST REQUIRED**. Full validation recorded in VALIDATION.md. User-reported v0.7.2 live rotary volume, header, Favorites, Tuner layout and Power/Mute/source controls are PASS. Existing volume scheduling and preset recall XML are unchanged.

All XML below is sent as HTTP POST to /YamahaRemoteControl/ctrl. cmd GET/PUT is the Yamaha operation.

## FM/AM selection
Reference: receiver-hosted receiver-scr1.js ajxSetTunerBand (1690–1692), matching Tuner/Play_Info/Tuning/Band and regional config captures.
```xml
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Band>FM</Band></Tuning></Play_Control></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Band>AM</Band></Tuning></Play_Control></Tuner></YAMAHA_AV>
```
Only enum FM/AM allowed. Connected/on/active/Ready tuner and valid target-band config required. After RC0/rsp PUT, read Basic_Status and Play_Info. Selected UI band follows readback, never just acknowledgement.

## FM Auto/Mono
Reference: receiver-scr1.js ajxSetTunerStereoMono (1686–1688), scr0 Auto/Mono callbacks; actual Play_Info FM_Mode=Auto.
```xml
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><FM_Mode>Auto</FM_Mode></Play_Control></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><FM_Mode>Mono</FM_Mode></Play_Control></Tuner></YAMAHA_AV>
```
Only on a Ready active FM tuner. FM_Mode is configured reception mode; Signal_Info/Stereo and Tuned independently describe actual reception. On AM, FM mode/RDS controls are omitted. Unknown mode strings select neither option.

## AM tuning and seek
Reference: ajxSetTunerFreq (1708–1762) AM branch: Exp0/kHz; ajxSetTunerPresetTuningProc (1678–1684) chooses AM or FM.
```xml
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Band>AM</Band><Freq><AM><Val>1134</Val><Exp>0</Exp><Unit>kHz</Unit></AM></Freq></Tuning></Play_Control></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Freq><AM><Val>Auto Up</Val></AM></Freq></Tuning></Play_Control></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Freq><AM><Val>Auto Down</Val></AM></Freq></Tuning></Play_Control></Tuner></YAMAHA_AV>
```
Absolute/one-step AM controls normalize current frequency to integer kHz. Use only Config/Range_and_Step/AM Min/Max/Step. The earlier regional capture is 531–1611 step9, but this is not a hardcoded operating range. FM remains Exp2/MHz with its own returned grid. Invalid/missing config disables manual writes for that band. Off-grid, out-of-range and nonintegral values are rejected. Seek transient Auto Up/Down is represented as searching, not malformed frequency. No literal relative Up/Down command invented.

Shared GETs:
```xml
<YAMAHA_AV cmd="GET"><Tuner><Config>GetParam</Config></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="GET"><Tuner><Play_Info>GetParam</Play_Info></Tuner></YAMAHA_AV>
```
One config response is parsed independently for FM and AM: one invalid/missing band does not discard the other. Readback after commands updates actual state; a failed PUT does not automatically disconnect.

## RDS and Home presentation
Play_Info/Meta_Info: Program_Service, Radio_Text_A, Radio_Text_B, Program_Type, optional Clock_Time. Values are trimmed; blank/dash-only metadata is hidden. A/B text duplicates and duplicates of station are suppressed. AM ignores stale FM metadata. One TunerStatus feeds both dedicated Tuner and Home; Home compacts optional text to two lines, with band/signal/preset in one summary.

Clock_Time exists in both old and fresh R-N301 GET captures but is empty. Parse populated values as opaque optional text; no time-zone/date/countdown interpretation. Tests with populated text are synthetic structural tests, not physical evidence. The subdued clock label appears only on Tuner when nonempty.

## Receiver firmware
Reference: actual System/Config/Version in earlier physical-config.xml (1.13/0.05).
```xml
<YAMAHA_AV cmd="GET"><System><Config>GetParam</Config></System></YAMAHA_AV>
```
Existing optional connect-time GET; no added polling or firmware-update request. Parser returns trimmed optional Version text. Connection can still use valid model description and status if this optional query fails. Settings → About → Receiver information displays cached model/address/version, with connection state; nothing added to Home.

## Sleep / Sound scope
Sleep final classification: **DOCUMENTED_FEATURE_XML_UNRESOLVED**. Main_Zone Basic_Status currently has no Sleep; family System/Basic_Status returned HTTP400. Generic candidate PUT paths are insufficiently verified. No Sleep GET/PUT feature, timer UI, local timer or background fallback implemented. See RN301_CAPABILITY_AUDIT.md for detailed evidence and the corrected family token spacing.

No Bass/Treble/Balance/EQ/Speakers/store-preset PUT added. Old milestone notes follow and retain historical context.

# v0.7.2 live volume and capability audit

This section supersedes earlier release-only rotary scheduling descriptions. XML encoding is unchanged.

- Live native volume uses the existing POST /YamahaRemoteControl/ctrl PUT:
```xml
<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Lvl><Val>50</Val><Exp>0</Exp><Unit></Unit></Lvl></Volume></Main_Zone></YAMAHA_AV>
```
Reference: primary R-N301 media_player.py async_set_volume_level (262–265), pinned source linked in RN301_CAPABILITY_AUDIT.md; previously physically verified absolute native integer writes. No dB conversion.
- Gesture preview uses the established 12-degree/native-unit mapping and bounds policy. LiveVolumeWriter holds one latest target, a conflated wakeup channel and one worker. INTERVAL_MS=125. The repository samples the latest value inside its mutex; stale queued targets are not accumulated. HTTP serialization and 125 ms minimum live-volume spacing remain in force; slow requests lower throughput.
- Successful live PUT checks Yamaha RC/rsp but does not fabricate a receiver status or run a GET per movement. Foreground general polling pauses during active/pending live volume; already-running reads cannot replace the gesture preview.
- Release sends only a still-needed latest target, suppresses an already acknowledged duplicate, and performs status readback. If a live attempt failed/in flight, at most one final synchronization is attempted. Errors preserve preview during rotation; final errors use command context and authoritative readback, not automatic discovery/disconnection.
- Background/cancel drops unsent targets. A live write already executed cannot be rolled back. Foreground polling later resynchronizes with hardware. Receiver-configured maximum can clamp the requested target; final readback wins.
- General/native +/- and all tuner XML remain unchanged. Tuner fields already parsed (RDS service/text/type, band/frequency/preset/tuned/stereo) now have one authoritative information area; compact preset buttons show only numbers.
- v0.7.1 gesture, +/- and Mute are user-reported physical PASS. v0.7.2 live-write timing is IMPLEMENTED / PHYSICAL RETEST REQUIRED.

See [R-N301 capability audit](RN301_CAPABILITY_AUDIT.md) for classified evidence and exact reference-only candidate XML. No preset store, tone, speaker or setup-setting commands were added.

The historical protocol notes below retain their original milestone context.

# Yamaha R-N301 protocol implemented through v0.7

**Current hardware update:** NET RADIO activation, basic catalogue browsing and station playback through the receiver’s Yamaha/vTuner service are now user-confirmed PASS. v0.6 UI functionality and language/settings navigation are also PASS. Earlier pending statements below describe their milestone-time status; unreported edge cases remain pending. SERVER core discovery, multi-level browsing and end-to-end playback are now physically VERIFIED (user report, 2026-09-30).

Exact paths were inspected before implementation. The v0.1 sections retain representative reference-derived XML. The v0.2 sections distinguish user-confirmed v0.1 hardware results, additional read-only workstation captures, and controls still awaiting physical Android testing.

## Reference snapshots

- **A (primary)**: [rihokirss/homeasisstant-rn301, media_player.py, commit 87c1f789](https://github.com/rihokirss/homeasisstant-rn301/blob/87c1f789bd2d13f8e965913108524f4f39c9ed26/custom_components/yamaha_rn301/media_player.py). Functions: async_update, _set_power_state, async_set_volume_level, async_mute_volume, async_select_source, _do_api_get/_put/_request.
- **B**: [FHEM 71_YAMAHA_NP.pm, commit 04580b98](https://github.com/mhop/fhem-mirror/blob/04580b9847ce3f8fced54196ea2f943b2d8d2d80/fhem/FHEM/71_YAMAHA_NP.pm). YAMAHA_NP_SendCmd, YAMAHA_NP_getModel/getInputs, statusRequest systemConfig/basicStatus, set power/mute/volume handling. This is a multi-device implementation, predominantly CRX-N560; R-N301 is named in its supported-family commentary. Its System/Basic_Status and scalar System/Volume/Lvl are NOT substituted for A's R-N301 paths.
- **C**: [Yamaha Remote, commit 2664e33a](https://github.com/tryptophane/yamaha-remote/tree/2664e33af839dbd7ca9f7f8165e8120ca32bdfc3): src/app/service/xml/http.ts and xml-builder.ts; input-selection.service.ts; volume-control.service.ts; resources/remote-api/YamahaRemoteControl.desc.xml. The descriptor fixture is explicitly RX-V475, NOT R-N301. It proves descriptor grammar, not R-N301 command compatibility.

## Transport and envelope

All control requests below use **HTTP POST http://HOST/YamahaRemoteControl/ctrl**, port 80. The XML cmd attribute selects GET or PUT; this does not change the HTTP method. Content-Type is text/xml; charset=utf-8. Builder prepends an XML 1.0 UTF-8 declaration. References A _do_api_request and C http.ts.

Responses require YAMAHA_AV, matching rsp, and integer RC. RC=0 succeeds. Nonzero RC becomes UnsupportedCommand carrying the raw RC for debug output; meanings are deliberately not guessed. System Config alone tolerates an absent RC, following B's explicit exception. Missing/malformed envelopes, missing mandatory fields and invalid numeric data are InvalidResponse. DTD/entities and payloads over 512 KiB are rejected.

Every PUT expects an acknowledgement of this form, possibly with empty echoed command nodes:

```xml
<YAMAHA_AV rsp="PUT" RC="0"/>
```

The parser validates the acknowledgement, then the repository reads Basic_Status. UI state is never inferred from a PUT echo. No retries of state-changing requests occur.

## GET status / GET power / GET volume / GET mute / GET source

All five reads are fulfilled by **one Basic_Status request**, rather than speculative individual GET paths. Endpoint: POST /YamahaRemoteControl/ctrl. Reference: A async_update, corroborated by C's descriptor G1.

```xml
<YAMAHA_AV cmd="GET"><Main_Zone><Basic_Status>GetParam</Basic_Status></Main_Zone></YAMAHA_AV>
```

Representative response (A's unitless volume representation):

```xml
<YAMAHA_AV rsp="GET" RC="0">
  <Main_Zone><Basic_Status>
    <Power_Control><Power>On</Power></Power_Control>
    <Volume><Lvl><Val>25</Val><Exp>0</Exp><Unit></Unit></Lvl><Mute>Off</Mute></Volume>
    <Input><Input_Sel>NET RADIO</Input_Sel></Input>
  </Basic_Status></Main_Zone>
</YAMAHA_AV>
```

Parser behavior:

- Main_Zone/Basic_Status/Power_Control/Power: exact On or Standby. Unknown/missing is invalid, never silently Standby.
- Volume/Lvl: preserve integer Val, exponent Exp and Unit. For dB only, display Val / 10^Exp. For example Val=-325, Exp=1, Unit=dB displays -32.5 dB. Empty Unit remains native, with no arbitrary percentage conversion.
- Volume/Mute: On=true; Off=false; invalid text is an error.
- Input/Input_Sel: preserve arbitrary nonempty text as Source.id; known values get friendly labels, unknown values display safely.
- Powered-on status requires volume, mute and source. Standby may omit these; they become null and controls disable.
- In v0.4, transient refresh failures mark values stale and disable controls; three consecutive refresh failures set UNAVAILABLE and clear live values. A failed command alone does not disconnect. Neither condition is a standby reading.

## Power On

Endpoint: POST /YamahaRemoteControl/ctrl. Request:

```xml
<YAMAHA_AV cmd="PUT"><System><Power_Control><Power>On</Power></Power_Control></System></YAMAHA_AV>
```

Expected response: successful PUT acknowledgement, then refreshed Basic_Status with On when the transition completes. Parser: common acknowledgement validation and status parsing. References: A _set_power_state; B PUT:System,Power_Control,Power.

## Standby

Endpoint: POST /YamahaRemoteControl/ctrl. Request:

```xml
<YAMAHA_AV cmd="PUT"><System><Power_Control><Power>Standby</Power></Power_Control></System></YAMAHA_AV>
```

Expected response: successful PUT acknowledgement, then status Standby. A receiver that stops networking may instead time out; the app correctly reports unavailable. Parser: common acknowledgement and status handling. References: same as Power On.

## SET volume (including up/down)

Endpoint: POST /YamahaRemoteControl/ctrl. Reference A async_set_volume_level verifies the native R-N301 request:

```xml
<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Lvl><Val>25</Val><Exp>0</Exp><Unit></Unit></Lvl></Volume></Main_Zone></YAMAHA_AV>
```

Only if the connected device reports dB AND advertises matching limits, the same structure can carry dB; C xml-builder.ts dbValue and volume-control.service.ts verify this encoding:

```xml
<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Lvl><Val>-325</Val><Exp>1</Exp><Unit>dB</Unit></Lvl></Volume></Main_Zone></YAMAHA_AV>
```

Expected response: PUT acknowledgement then actual current volume in Basic_Status. Parser: common acknowledgement; reads Val/Exp/Unit on refresh. Builder retains receiver encoding. Up/down computes one advertised native Step, clamps to Min/Max, and submits the same SET command. The slider quantizes to Step and writes on release. No unverified Up/Down XML literals are sent. Limits must match current exponent/unit; missing limits disable writes. 0.5 dB corresponds to Step=5 at Exp=1, when advertised.

**Uncertainty:** A's code uses unitless values, while its docs/api-reference.md claims dB (-80.5..+16.5) and different power/mute paths. No conversion between the native R-N301 number and actual amplifier dB has been proven. RX-V475 limits from C must never be hardcoded for R-N301. B's System/Volume/Lvl scalar path is not implemented.

## SET mute / Unmute / Toggle

Endpoint: POST /YamahaRemoteControl/ctrl.

```xml
<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Mute>On</Mute></Volume></Main_Zone></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Mute>Off</Mute></Volume></Main_Zone></YAMAHA_AV>
```

Expected response: PUT acknowledgement, then Basic_Status/Volume/Mute reflects On/Off. Parser: common acknowledgement and status. Corrected in v0.4 using reference D, receiver-served scr1.js ajxSetMute (lines 422–430), and gZoneIdx2Name (around line 124), which maps the main zone to Main_Zone. References A async_mute_volume (lines 271–273) and B use System/Volume/Mute; that conflicting path was used previously and failed in the user’s hardware test. The receiver’s own implementation takes precedence. Empty nested echoes and bare RC=0 acknowledgements are accepted; neither is mistaken for the new mute state. Toggle chooses the inverse of the last confirmed state inside the serialized repository; unknown state cannot toggle. Mute is not inferred from zero volume.

## SET source

Endpoint: POST /YamahaRemoteControl/ctrl.

```xml
<YAMAHA_AV cmd="PUT"><Main_Zone><Input><Input_Sel>CD</Input_Sel></Input></Main_Zone></YAMAHA_AV>
```

Expected response: PUT acknowledgement, then Basic_Status/Input/Input_Sel. Parser: common acknowledgement and safe Source model. References A async_select_source and SOURCE_MAPPING, C input-selection.service.ts setInputTo.

Use exact receiver-advertised Param values when available. Fallback labels map to TUNER, CD, OPTICAL, COAXIAL, LINE1, LINE2, LINE3, Spotify, SERVER, NET RADIO, AirPlay. A explicitly lists all except COAXIAL/AirPlay; C confirms AirPlay casing. COAXIAL is a requested known hardware input whose literal still needs confirmation on the R-N301. Fallback availability is visibly marked unconfirmed. Unsupported writes are reported, not silently mapped to another input. XML metacharacters are escaped.

Through v0.2 no source-specific media request was implemented. v0.3 adds only the Spotify requests documented below.

## Device description and capability discovery

Endpoint: **HTTP GET /YamahaRemoteControl/desc.xml** (no request XML).
Reference C resources/remote-api/YamahaRemoteControl.desc.xml.

Representative structure:

```xml
<Unit_Description Version="1.2" Unit_Name="R-N301">
  <Menu>
    <Menu Func="Vol_Lvl"><Put_2>
      <Cmd ID="P2">Val=Param_1:Exp=Param_2:Unit=Param_3</Cmd>
      <Param_1><Range>-805,165,5</Range></Param_1>
      <Param_2><Direct>1</Direct></Param_2>
      <Param_3><Direct>dB</Direct></Param_3>
    </Put_2></Menu>
    <Cmd_List>
      <Define ID="P2">Main_Zone,Volume,Lvl</Define>
      <Define ID="G2">Main_Zone,Input,Input_Sel_Item</Define>
    </Cmd_List>
  </Menu>
</Unit_Description>
```

This is a synthetic grammar example, not an assertion that an R-N301 advertises this range. Parser requires Unit_Description, reads Unit_Name and Define paths, resolves the volume command ID within its unit Menu, and reads the associated Put_2 range/Exp/Unit. It does not mistake tuner/tone ranges for volume. Unrecognized grammar yields no range, not guessed limits. The returned model must identify R-N301 before any control writes are enabled.

## GET System Config

Endpoint: POST /YamahaRemoteControl/ctrl. Reference B YAMAHA_NP_getModel / statusRequest systemConfig.

```xml
<YAMAHA_AV cmd="GET"><System><Config>GetParam</Config></System></YAMAHA_AV>
```

Representative response:

```xml
<YAMAHA_AV rsp="GET" RC="0"><System><Config>
  <Model_Name>R-N301</Model_Name>
  <Volume><Min>0</Min><Max>100</Max><Step>1</Step></Volume>
</Config></System></YAMAHA_AV>
```

Parser reads System/Config/Model_Name and optional scalar Volume/Min/Max/Step. The shown values are synthetic test data, not hardcoded bounds. B documents scalar native limits; these are usable only with Exp=0 and empty Unit in current status. Description ranges take precedence when their encoding matches; configuration is the fallback. B notes this response can omit RC, so only this command permits that omission. Nonzero RC still fails.

Feature_Existence in B is a mixed feature list; this app does not blindly turn it into source IDs. Description/configuration are optional individually, but at least one must supply an R-N301 model. A valid Yamaha Basic_Status response is also mandatory. No other model is silently accepted.

## GET available sources (capability gated)

Endpoint: POST /YamahaRemoteControl/ctrl. Reference C input-selection.service.ts loadInputList and descriptor G2.

```xml
<YAMAHA_AV cmd="GET"><Main_Zone><Input><Input_Sel_Item>GetParam</Input_Sel_Item></Input></Main_Zone></YAMAHA_AV>
```

Representative response:

```xml
<YAMAHA_AV rsp="GET" RC="0"><Main_Zone><Input><Input_Sel_Item>
  <Item_1><Param>CD</Param><RW>RW</RW><Title>CD</Title><Src_Name>CD</Src_Name></Item_1>
  <Item_2><Param>COAXIAL</Param><RW>RW</RW><Title>Coaxial</Title></Item_2>
</Input_Sel_Item></Input></Main_Zone></YAMAHA_AV>
```

Sent only when the receiver description explicitly defines Main_Zone,Input,Input_Sel_Item. Parser preserves Param and nonempty Title, filters empty entries, deduplicates IDs, respects read-only RW, and safely represents unknown values. Empty/malformed lists fall back to known inputs with a visible notice. COAXIAL fallback remains disabled unless advertised or already observed as the current input, because no supplied implementation verifies its literal on the R-N301.

## Original v0.1 hardware questions (updated by v0.2 evidence below)

1. Does R-N301 firmware expose desc.xml with Unit_Name and the same volume grammar?
2. Does System Config provide model and native limits? What are their exact values?
3. Does its status use unitless volume, and is any documented native-to-dB mapping available?
4. Does it advertise Input_Sel_Item, COAXIAL and AirPlay on this unit/region?
5. Do System power/mute writes work as in A, including network standby wake?
6. Are all expected fields present in standby, and how long do transitions take?

These are deliberately documented rather than hidden by newer MusicCast assumptions. Later sections document the limited v0.3 Spotify player addition; browsing APIs remain out of scope.

## v0.2 hardware evidence and discovery

The user confirmed v0.1 connection, model detection, power-state read, current-source read and Spotify source detection on R-N301 at 192.168.1.55. Volume displayed **40 (native)**. No native-to-dB mapping is established.

Additional development-workstation requests were read-only: System Config identified R-N301, the Yamaha command descriptor returned HTTP 404, and no top-level Volume limits were present in Config. These observations explain why native volume and disabled adjustment without advertised limits remain unchanged.

**HTTP GET http://HOST:8080/MediaRenderer/desc.xml** has no request XML. The actual response contains root/device with manufacturer Yamaha Corporation, modelName R-N301, modelNumber N301 and deviceType urn:schemas-upnp-org:device:MediaRenderer:1. The parser handles namespace prefixes, requires this manufacturer/model/device type, and reads optional friendlyName/UDN. Discovery follows the validated SSDP LOCATION rather than assuming a fixed description URL. Reference B, YAMAHA_NP_getMediaRendererDesc, lines 2682-2691, verifies this UPnP path. See [DISCOVERY.md](DISCOVERY.md) for exact SSDP requests, bounds, permissions and candidate verification.

The workstation SSDP probe returned no candidates during v0.2 development. Subsequent user testing physically verified Android SSDP discovery, IP detection, identification and connection; see the v0.3 evidence section.

## GET Tuner status, frequency, preset and metadata

Purpose: obtain the dedicated Tuner screen state. Endpoint: **POST /YamahaRemoteControl/ctrl**.

```xml
<YAMAHA_AV cmd="GET"><Tuner><Play_Info>GetParam</Play_Info></Tuner></YAMAHA_AV>
```

Expected response, shortened from the read-only R-N301 capture:

```xml
<YAMAHA_AV rsp="GET" RC="0"><Tuner><Play_Info>
  <Feature_Availability>Not Ready</Feature_Availability>
  <Search_Mode>Tuning</Search_Mode>
  <Preset><Preset_Sel>1</Preset_Sel></Preset>
  <Tuning><Band>FM</Band><Freq>
    <Current><Val>9740</Val><Exp>2</Exp><Unit>MHz</Unit></Current>
  </Freq></Tuning>
  <Signal_Info><Tuned>Negate</Tuned><Stereo>Negate</Stereo></Signal_Info>
  <Meta_Info><Program_Type></Program_Type><Program_Service></Program_Service>
    <Radio_Text_A></Radio_Text_A><Radio_Text_B></Radio_Text_B>
  </Meta_Info>
</Play_Info></Tuner></YAMAHA_AV>
```

Parser behavior:
- Validate the common GET envelope and Tuner/Play_Info container.
- Preserve band and Feature_Availability; preset is nullable integer 1..40.
- Current frequency uses Val / 10^Exp and the receiver's Unit, so 9740/10^2 is 97.4 MHz. AM with Exp=0 retains its kHz value. Missing frequency is unavailable; malformed numeric frequency is an invalid response.
- Assert/Negate signal flags become nullable booleans.
- Program_Service is station; Radio_Text_A/B supply available program text; Program_Type is optional. Empty fields remain absent, never fabricated.
- Preset controls require availability Ready. Tuner read errors are isolated from otherwise valid basic receiver status.
- Poll only while the Tuner screen is visible and the powered receiver reports TUNER. Presets are fetched once per session/explicit refresh, not every two seconds.

Exact references: **A _update_media_playing, lines 371-435** verifies TUNER -> Tuner and Play_Info plus Tuning/Freq/Current, Preset/Preset_Sel, Signal_Info and Meta_Info paths. **B line 1541**, GET:Tuner,Play_Info:GetParam, corroborates the request. **C descriptor G1, lines 728-738** is additional grammar evidence only.

The full observed response is a unit-test fixture at app/src/test/resources/rn301-tuner-not-ready.xml. It was captured while the tuner was Not Ready, so reception/RDS and Android screen behavior still need physical validation.

## GET stored Tuner presets

Purpose: discover actual selectable stored presets rather than assume eight slots. Endpoint: **POST /YamahaRemoteControl/ctrl**.

```xml
<YAMAHA_AV cmd="GET"><Tuner><Play_Control><Preset><Preset_Sel_Item>GetParam</Preset_Sel_Item></Preset></Play_Control></Tuner></YAMAHA_AV>
```

Expected list shape:

```xml
<YAMAHA_AV rsp="GET" RC="0"><Tuner><Play_Control><Preset><Preset_Sel_Item>
  <Item_1><Param>1</Param><RW>RW</RW><Title>FM 97.40 MHz</Title></Item_1>
  <Item_2><Param>2</Param><RW>RW</RW><Title>FM 97.90 MHz</Title></Item_2>
</Preset_Sel_Item></Preset></Play_Control></Tuner></YAMAHA_AV>
```

Parser validates the envelope and exact list container, keeps numeric Param values 1..40 only when RW includes W, deduplicates and sorts by preset number. Empty/non-numeric/No Preset/read-only entries are excluded. Missing titles get a neutral preset-number label. An empty valid list means no stored presets, not eight imaginary stations.

Reference **C descriptor G3, lines 728-738** verifies the path, but its RX-V475 origin alone was insufficient. This exact GET was additionally sent to the physical R-N301: its response provided six writable stored presets (1..6) and a nonselectable No Preset entry. Full response: app/src/test/resources/rn301-presets.xml. This read-only evidence supports the implemented R-N301 list query.

The [R-N301 owner's manual](https://europe.yamaha.com/files/download/other_assets/5/332425/R-N301_om_G.pdf), radio preset section, documents up to 40 FM/AM stations. Forty is capacity, six is the observed stored list, and eight is only the primary reference's navigation assumption.

## SET Tuner preset / Previous preset / Next preset

Purpose: select a stored station. Endpoint: **POST /YamahaRemoteControl/ctrl**.

```xml
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Preset><Preset_Sel>4</Preset_Sel></Preset></Play_Control></Tuner></YAMAHA_AV>
```

Expected response is the common PUT acknowledgement with RC=0, followed by GET Basic_Status and GET Tuner/Play_Info to read the actual resulting state. Do not infer a station from the acknowledgement.

Exact primary reference: **A async_play_media, lines 303-306** sends the numeric media_id at Tuner/Play_Control/Preset/Preset_Sel. **A _next_preset, lines 446-458, and _previous_preset, lines 462-474** use the same numeric selection behavior with an assumed 1..8 range. The app instead walks and wraps the receiver's advertised stored list. If list retrieval fails, explicit direct buttons 1..8 are marked unconfirmed; previous/next remain disabled. This fallback does not claim that the receiver has only eight slots.

Builder accepts numbers 1..40; repository normally requires membership in the advertised list and active, ready Tuner. Missing list permits only the reference-supported 1..8 fallback. **B lines 1375/1379** contains Next/Prev string forms for the broader device family; these strings are deliberately not sent by this implementation. **C P2** corroborates the numeric path but is not the R-N301 compatibility authority.

No preset PUT, source-switch PUT, tuning-frequency write, band change, scan, registration or deletion was performed during development hardware inspection. Subsequent user testing passed the Android Tuner screen, preset retrieval and selection. Wraparound and individual RDS behavior were not separately reported; new manual tuning and Spotify controls still require physical testing.

## Historical v0.2 questions (see physical results above and v0.3 evidence below)

- Does Android SSDP receive responses on the user's router/Wi-Fi, including standby and multiple receivers?
- Do numeric preset writes and readiness transitions work as documented by A on this firmware?
- Which RDS fields appear for actual broadcasts, and how quickly do they update?
- Does other R-N301 firmware expose the optional command descriptor, volume limits or input list?
- Native volume 40 still has no verified dB conversion.


## v0.3 evidence and physical status

The user has now physically verified v0.2 SSDP discovery, IP detection, R-N301 identification, connection, source selection/UI, Tuner screen, saved-preset retrieval and saved-preset selection on the Android phone and R-N301. Earlier pending statements in this document are historical. Subsequent v0.3 user testing passed native volume steps, manual tuner frequency controls, presets and source artwork/UI. Spotify playback controls and seek behavior were not separately confirmed.

**D: receiver-served Yamaha implementation**, inspected read-only at http://192.168.1.55/JavaScripts/scr0.js and /JavaScripts/scr1.js. These are additional primary protocol evidence supplied by the R-N301 firmware, not runtime dependencies. Local ignored snapshots:
- scr0.js SHA-256 B8C0CED473BCBF2D243765F945721AE0491FF171D4DFB2CD5E2BD3DC6F38E863.
- scr1.js SHA-256 FBFE19BEB7E2E26F31963BBC8EF4F5158F3023EE5B9AD26820EED76B49A61DBB.

The scripts also contain generic AVR features: their dB assumptions and party-mode volume paths are NOT adopted. Main_Zone/Config returned HTTP 400 in a read-only probe and is not added as a runtime request.

## Volume − / Volume + (native step operation)

No explicit relative-volume XML literal could be verified for R-N301. In particular, Up/Down, Up 1 dB and System/Party_Mode are not assumed compatible. The app implements the operation using a fresh status read and a single reference-backed native SET, without converting to percentage or dB.

Evidence:
- **A async_set_volume_level, lines 262-265**: R-N301 native integer Val, Exp=0, empty Unit at Main_Zone/Volume/Lvl.
- **B volumeUp/volumeDown, lines 1274-1290**: one receiver-native integer per step. B's System/Volume/Lvl scalar path is not copied.
- [Official R-N301 owner's manual](https://uk.yamaha.com/files/download/other_assets/4/332424/R-N301_om_AB-1.pdf), Max Volume / Initial Volume section (printed p.37): numeric volume settings use one-step increments. This supports native integer stepping, not a dB mapping.

Endpoint for both reads and writes: **POST /YamahaRemoteControl/ctrl**.

First, GET Main_Zone/Basic_Status as documented above. If returned native Val is 40, a plus tap sends:

```xml
<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Lvl><Val>41</Val><Exp>0</Exp><Unit></Unit></Lvl></Volume></Main_Zone></YAMAHA_AV>
```

A minus tap from a freshly read 40 sends Val=39 with the identical envelope. Expected response: common PUT RC=0 acknowledgement, then GET Basic_Status. The actual returned volume is displayed; an acknowledgement alone never changes the UI.

The read, one SET and refresh share the repository mutex. A failed fresh read prevents the write. No automatic retry or press-and-hold repeat. If matching limits/step were advertised, use that native step; otherwise only Exp=0, empty Unit and numeric 0..99 are allowed. The primary integration admits zero; the manual's separate Max sentinel has no confirmed wire representation, so fallback plus is disabled at 99. Receiver maximum-volume clamping is respected through readback. A separate controller can still race between network read and SET; without a proven atomic relative command this cannot be eliminated.

The user subsequently confirmed native integer stepping and volume buttons PASS on the physical R-N301 in v0.3.

## GET FM range and step

Purpose: avoid assuming 0.1 MHz or another region's limits. Endpoint: **POST /YamahaRemoteControl/ctrl**.

```xml
<YAMAHA_AV cmd="GET"><Tuner><Config>GetParam</Config></Tuner></YAMAHA_AV>
```

Expected response, shortened from the read-only R-N301 capture:

```xml
<YAMAHA_AV rsp="GET" RC="0"><Tuner><Config>
  <Feature_Availability>Not Ready</Feature_Availability>
  <Range_and_Step><FM>
    <Min><Val>8750</Val><Exp>2</Exp><Unit>MHz</Unit></Min>
    <Max><Val>10800</Val><Exp>2</Exp><Unit>MHz</Unit></Max>
    <Step><Val>5</Val><Exp>2</Exp><Unit>MHz</Unit></Step>
  </FM></Range_and_Step>
</Config></Tuner></YAMAHA_AV>
```

Parser validates envelope, positive consistent bounds and MHz units, then normalizes exactly to hundredths of MHz for the verified write encoding. Precision loss, invalid numbers and zero step are rejected. Missing FM limits disable manual controls. The observed device advertises **0.05 MHz**, not 0.1 MHz. Config loads once per tuner session/explicit refresh.

Exact reference: **D scr1.js ajxGetTunerConfig (line 2022), ajxHandleTunerConfig (1816 onward; FM Min/Max/Step at 1839-1848)**. Physical read-only fixture: app/src/test/resources/rn301-tuner-config.xml.

## Direct FM tuning and frequency step − / +

Purpose: tune a numeric FM frequency, or one advertised step from a fresh frequency read. Endpoint: **POST /YamahaRemoteControl/ctrl**.

```xml
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning>
  <Band>FM</Band><Freq><FM><Val>10165</Val><Exp>2</Exp><Unit>MHz</Unit></FM></Freq>
</Tuning></Play_Control></Tuner></YAMAHA_AV>
```

Expected response: PUT acknowledgement; refresh Basic_Status and Tuner/Play_Info. Parser reads actual Tuning/Freq/Current Val/Exp/Unit using the existing frequency parser. No optimistic frequency or RDS label is displayed.

Exact reference: **D scr1.js ajxSetTunerFreq, lines 1708-1762**, constructs Band plus Freq/FM/Val, Exp=2 and Unit=MHz. **A** confirms the current-frequency read path. **B tunerFMFrequency, lines 1663 onward**, instead uses Tuning/FM/Freq scalar data; that different multi-device command is deliberately NOT used.

For step −/+, the repository first rereads tuner state under its lock, adds/subtracts the advertised FM step, validates bounds/grid, and emits the same direct SET once. No fabricated Up/Down literal. Decimal numeric entry is parsed exactly (decimal comma also accepted), never rounded onto another station. Only powered, active, ready FM with a validated range can write; AM manual tuning and band switching remain out of scope.

## FM seek down / seek up

Endpoint: **POST /YamahaRemoteControl/ctrl**.

```xml
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Freq><FM><Val>Auto Down</Val></FM></Freq></Tuning></Play_Control></Tuner></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Freq><FM><Val>Auto Up</Val></FM></Freq></Tuning></Play_Control></Tuner></YAMAHA_AV>
```

Exact reference: **D scr0.js onBtnTunerUp/onBtnTunerMinus, lines 18-19**, call **scr1.js ajxSetTunerPresetTuningProc, lines 1678-1684**. That function sends the shown nested FM/Val command. C contains additional auto-seek grammar, but is not used to substitute a different request structure.

Expected response: common PUT acknowledgement followed by status refresh. During seek, known Auto Up/Auto Down (and read-only TP states) in Current/Val are represented as a seeking state with unavailable numeric frequency, rather than malformed XML. Unknown nonnumeric values remain invalid. Seek completion is observed through conservative foreground polling; no local station scan, registration/deletion or unverified cancellation command is implemented.


## GET Spotify Now Playing

Endpoint: **POST /YamahaRemoteControl/ctrl**. Purpose: receiver-local playback state and metadata, with no Spotify Web API, authentication or SDK.

```xml
<YAMAHA_AV cmd="GET"><Spotify><Play_Info>GetParam</Play_Info></Spotify></YAMAHA_AV>
```

Representative active response (synthetic metadata; not a physical playback capture):

```xml
<YAMAHA_AV rsp="GET" RC="0"><Spotify><Play_Info>
  <Feature_Availability>Ready</Feature_Availability>
  <Playback_Info>Play</Playback_Info>
  <Meta_Info><Artist>Artist</Artist><Album>Album</Album><Track>Track</Track></Meta_Info>
</Play_Info></Spotify></YAMAHA_AV>
```

Exact references: **A _update_media_playing, lines 371-417**, Spotify mapping and Meta_Info/Playback_Info; **D scr1.js ajxGetNetusbUnitName (Spotify at 1389), ajxGetNetusbPlayInfo (1463-1466)**. **C Spotify G1 and metadata menus, lines 826-912**, corroborate the field names.

Parser requires the Yamaha GET envelope and Spotify/Play_Info container. Empty Artist/Album/Track are null; Track falls back to Song only if provided. XML entities are decoded once by the XML parser. Play, Pause and Stop map to distinct domain states. Absent/unknown playback stays UNKNOWN with raw text retained; it is never inferred from track presence. Not Ready disables controls without fabricating media.

If Play_Mode/Shuffle or Play_Mode/Repeat fields are present, they are optional read-only metadata: On/Off -> Boolean; Off/One/All -> RepeatMode. Unknown shuffle is null; unknown repeat is UNKNOWN. The primary reference's logic equating both modes to one Play_Mode text is not copied.

The observed read-only physical response contained Not Ready, Stop and empty metadata, captured as rn301-spotify-not-ready.xml. It contained no shuffle/repeat fields. Active metadata is still pending physical phone validation.

## Spotify Play / Pause / Stop / Previous / Next

Endpoint: **POST /YamahaRemoteControl/ctrl**.

```xml
<YAMAHA_AV cmd="PUT"><Spotify><Play_Control><Playback>Play</Playback></Play_Control></Spotify></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Spotify><Play_Control><Playback>Pause</Playback></Play_Control></Spotify></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Spotify><Play_Control><Playback>Stop</Playback></Play_Control></Spotify></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Spotify><Play_Control><Playback>Skip Rev</Playback></Play_Control></Spotify></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><Spotify><Play_Control><Playback>Skip Fwd</Playback></Play_Control></Spotify></YAMAHA_AV>
```

Exact references: **A _media_play_control and async_media_play/pause/stop/next_track/previous_track, lines 276-300**; **D scr0.js onBtnSpotifyPlay/Pause/Stop/Rev/Fwd, lines 136-140**, routed through **scr1.js ajxSetNetusbPlayback, lines 1399-1403**. C's Spotify UI lists Play/Pause/Skip; Stop is additionally supported by A and the receiver-served D code, not inferred from a playback-state value.

Expected response: common PUT acknowledgement. Repository rechecks active source and readiness, sends one control, then reads Basic_Status and Spotify/Play_Info. UI always shows the returned playback state. No automatic PUT retry, no optimistic Play/Pause transition. Unsupported commands and invalid player responses remain player errors without falsely setting receiver power to Standby.

**Shuffle/repeat writes are intentionally omitted.** A sends Shuffle as a generic Playback value without source-specific evidence; C defines mode paths but exposes no Spotify mode setters; D's generic network mode functions do not prove Spotify supports them. No proven repeat action is available. Thus neither a toggle nor modern Spotify behavior is invented.

## Player architecture and polling

Compose -> ReceiverViewModel -> YamahaRepository -> YamahaPlayer/LegacyYamahaPlayer -> YamahaTransport/XML builder/parser.

NowPlaying retains source, title, artist, album, station, raw playback and typed playback mapping, optional shuffle/repeat and availability. YamahaPlayer is source-independent at its interface; the current implementation explicitly allows only Spotify. SERVER, NET RADIO and AirPlay playback/browsing remain unimplemented.

The compact Home card and dedicated player page use the same StateFlow. Basic status and visible Spotify metadata refresh in one serialized cycle approximately every two seconds (plus request duration). Settings and Tuner pages suspend Spotify metadata polling. Leaving Spotify or powering off clears metadata; backgrounding cancels polling/actions. The existing minimum request spacing remains. No background service or cloud requests were added.

## v0.3 protocol uncertainties requiring physical testing

- Native single-step volume is reference-backed read/SET, not a verified atomic relative command. Concurrent external volume changes can race; fallback Max is not guessed.
- New direct FM and seek writes are verified against receiver-served code, but not exercised on hardware during development.
- Spotify controls are verified against A and receiver-served code; active metadata, Stop behavior and availability transitions must be tested with an actual Spotify Connect session.
- Shuffle/repeat write support remains unresolved and controls are not exposed.
- Firmware scripts include generic features, so only documented selected paths were implemented. No R-N301 dB mapping is asserted.

## v0.4 corrections and limits (2026-09-26)

The reported v0.3 Mute failure was an incorrect “Receiver not found” state while other controls worked. Code investigation found the conflicting System scope above, HTTP command rejections classified as receiver absence, and repository command exceptions clearing the connection. The original failing HTTP response was not captured; an HTTP 400 for that particular Mute tap is not claimed.

v0.4 uses the firmware's Main_Zone scope, validates RC/rsp acknowledgements, and refreshes actual Basic_Status. A failed command makes one read-only status probe, never retries the PUT or starts discovery, and preserves a reachable connection. Error contexts distinguish Mute/command failures, refresh failures, timeouts, network absence and discovery failures. Debug logs identify Mute On/Off, endpoint, status and parsing/error details without raw payloads. The corrected Mute write still requires phone/receiver verification.

Read-only checks of the test R-N301 at 192.168.1.55 found firmware 1.13/0.05, native Val=50, Exp=0, empty Unit and Mute=Off. System/Config exposes no volume limits; /YamahaRemoteControl/desc.xml returns HTTP 404. These observations are not successful Mute-control tests.

The native slider is enabled only with advertised, matching Min/Max and Step=1. Dragging updates local state only; releasing sends one existing SET volume command followed by readback. No range is inferred from the current volume or borrowed from another model. This receiver therefore retains the physically verified minus/plus fallback. Native-to-dB conversion and the Max sentinel remain unresolved. Synthetic range fixtures test the conditional slider; they are not a claim that this R-N301 advertises such a range.

No new Spotify, SERVER or NET RADIO protocol features were introduced in v0.4.

## v0.5 SERVER browser and player (2026-09-27)

**Physical result supplied by the user:** v0.4 Mute/Unmute using Main_Zone/Volume/Mute **PASS**. This supersedes the earlier pending status. No SERVER browsing or playback command has been physically verified in this milestone.

### Exact evidence used

- **A**, the pinned R-N301 Home Assistant integration above, re-inspected: _parse_server_xml_response (686–727), _create_browse_media_children (730–762), _browse_server_root (840–875), _browse_server_item (877–930), _navigate_to_server_path (933–955), _browse_server_back (957–988), _get_current_server_list (990–1038), _reset_server_to_root (1040 onward), _play_server_track (649–665), _media_play_control and transport actions (275–301), and SERVER Play_Info mapping/parsing (382–430).
- **B**, pinned FHEM implementation above: player handling around 1408, Direct_Sel at 1451, list parsing/navigation around 2348–2442. Its generic Player scope is not substituted for R-N301 SERVER. It corroborates eight slots, Container/Item/Unselectable attributes, cursor position and Return navigation. directPlay orchestration was inspected but is not copied.
- **D**, previously captured receiver-served scr1.js: ajxGetNetusbUnitName (1374 onward) explicitly maps SERVER; ajxSetNetusbPlayback (1399), ajxSetNetUsbDSel (1409), ajxSetNetusbButton (1439), ajxGetNetusbListInfo (1450), ajxGetNetusbPlayInfo (1463), ajxSetNetusbPageUpDown (1468), ajxHandleNetusbPlayInfo (1544), ajxHandleNetusbList_Info (1617). Page calculation uses eight lines. List_Control handling schedules a list read after 700 ms.
- **C**, Yamaha Remote descriptor was re-inspected for list grammar only. It describes RX-V475, not R-N301; its Return to Home and Jump_Line possibilities are not treated as verified R-N301 commands.

All requests below are **HTTP POST /YamahaRemoteControl/ctrl**, port 80, text/xml; charset=utf-8. XML cmd determines GET versus PUT. No phone-to-NAS discovery, ContentDirectory calls, media URL retrieval or media streaming is implemented. The receiver remains the DLNA client and playback device.

### GET current SERVER list

Purpose: read the receiver's current menu/window. References A _browse_server_root/_get_current_server_list and D ajxGetNetusbListInfo.

```xml
<YAMAHA_AV cmd="GET"><SERVER><List_Info>GetParam</List_Info></SERVER></YAMAHA_AV>
```

Representative **synthetic**, reference-derived response:

```xml
<YAMAHA_AV rsp="GET" RC="0"><SERVER><List_Info>
  <Menu_Status>Ready</Menu_Status>
  <Menu_Layer>2</Menu_Layer>
  <Menu_Name>Music</Menu_Name>
  <Cursor_Position><Current_Line>9</Current_Line><Max_Line>18</Max_Line></Cursor_Position>
  <Current_List>
    <Line_1><Txt>Artists</Txt><Attribute>Container</Attribute></Line_1>
    <Line_2><Txt>Example track</Txt><Attribute>Item</Attribute></Line_2>
  </Current_List>
</List_Info></SERVER></YAMAHA_AV>
```

Parser validates the envelope and position values, reads only SERVER/List_Info, preserves titles with XML entity decoding, and accepts Line_1 through Line_8. Duplicate/out-of-range slots or malformed XML are invalid. Blank rows are omitted. Container is browsable DIRECTORY; Item is playable TRACK. Unselectable, missing and unknown attributes are displayed safely without selection. Albums and playlists remain containers because these references do not expose reliable subtypes; A's depth-based album inference is deliberately not used.

Ready requires valid layer/cursor/count metadata. Busy can omit those fields; the browser makes at most six reads, 500 ms apart, then reports not ready. Unknown statuses are not interpreted as Ready. Zero entries is an empty state, with a root-specific no-media-servers message. Other failures use a folder-loading error rather than asserting the receiver disappeared.

### SELECT directory or playable item

Purpose: activate one slot in the current window. References A _navigate_to_server_path/_play_server_track, B listItem selection and D ajxSetNetUsbDSel.

```xml
<YAMAHA_AV cmd="PUT"><SERVER><List_Control><Direct_Sel>Line_2</Direct_Sel></List_Control></SERVER></YAMAHA_AV>
```

Slot is 1–8, not the absolute library index. Expected response: common PUT RC=0 acknowledgement. Before writing, fresh List_Info must match the displayed layer, menu name, page and entries; a mismatch asks the user to refresh. Container selection reads the resulting menu after 700 ms and does not issue Play. Item selection follows Direct_Sel with the Play command below, as A _play_server_track explicitly does, then reads menu and actual playback metadata. No media URL is fetched. No state-changing command is automatically retried.

### BACK and HOME

Purpose: move to the parent menu. References A _browse_server_back/_reset_server_to_root; B Cursor:Return; D ajxSetNetusbButton.

```xml
<YAMAHA_AV cmd="PUT"><SERVER><List_Control><Cursor>Return</Cursor></List_Control></SERVER></YAMAHA_AV>
```

Expected response: PUT RC=0, followed by List_Info showing a lower Menu_Layer. Home repeats verified Return commands until layer 1, with progress checks, at most 32 transitions and an overall operation deadline. There is no invented Home XML. Android Back uses a fresh layer: return one level when nested; leave the screen when already at root. A separate Receiver action can leave the browser even when the media server is unresponsive.

Only receiver-reported Menu_Name and level are displayed. No fake filesystem path or reconstructed breadcrumb is persisted across external navigation.

### PREVIOUS / NEXT PAGE

Purpose: change the current list window. References A _browse_server_item and D ajxSetNetusbPageUpDown/ajxHandleNetusbList_Info.

```xml
<YAMAHA_AV cmd="PUT"><SERVER><List_Control><Page>Up</Page></List_Control></SERVER></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><SERVER><List_Control><Page>Down</Page></List_Control></SERVER></YAMAHA_AV>
```

Expected response: PUT RC=0 then List_Info. Page = floor((Current_Line-1)/8)+1; count = ceil(Max_Line/8). Controls are bounded by the reported count, with no wrap request at either end. A fresh preflight confirms the displayed window; readback must retain menu/layer and advance or retreat one page. Only the current window is stored/rendered in LazyColumn. Browser lists are event-driven, not periodically polled.

### GET SERVER playback metadata

Purpose: populate the shared NowPlaying model. References A SERVER device mapping/media parsing, D ajxGetNetusbPlayInfo and ajxHandleNetusbPlayInfo.

```xml
<YAMAHA_AV cmd="GET"><SERVER><Play_Info>GetParam</Play_Info></SERVER></YAMAHA_AV>
```

Expected shape (synthetic):

```xml
<YAMAHA_AV rsp="GET" RC="0"><SERVER><Play_Info>
  <Feature_Availability>Ready</Feature_Availability>
  <Playback_Info>Play</Playback_Info>
  <Meta_Info><Artist>Artist</Artist><Album>Album</Album><Song>Track title</Song></Meta_Info>
</Play_Info></SERVER></YAMAHA_AV>
```

Parser reads Track/Song/Title, Artist and Album when nonempty. Missing metadata is omitted, never fabricated. Playback_Info Play/Pause/Stop maps to explicit states; other values stay UNKNOWN. Optional Play_Mode shuffle/repeat fields remain read-only. No shuffle/repeat writes were added.

### SERVER PLAY / PAUSE / STOP / PREVIOUS / NEXT

Purpose: control the receiver's SERVER player. References A _media_play_control and async_media_* methods, with SERVER mapping; D ajxSetNetusbPlayback. Each request is independently represented behind YamahaPlayer:

```xml
<YAMAHA_AV cmd="PUT"><SERVER><Play_Control><Playback>Play</Playback></Play_Control></SERVER></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><SERVER><Play_Control><Playback>Pause</Playback></Play_Control></SERVER></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><SERVER><Play_Control><Playback>Stop</Playback></Play_Control></SERVER></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><SERVER><Play_Control><Playback>Skip Rev</Playback></Play_Control></SERVER></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><SERVER><Play_Control><Playback>Skip Fwd</Playback></Play_Control></SERVER></YAMAHA_AV>
```

Expected response: common PUT RC=0 acknowledgement followed by Basic_Status and SERVER Play_Info. The source is rechecked before playback writes. UI uses actual readback, never the desired state as a prediction. Spotify retains its separate XML command scope.

### Lifecycle, errors and uncertainties

SERVER selection first uses the existing input command, then waits for confirmed SERVER status before list requests. Browser operations share the repository mutex, use a 15-second coroutine deadline (an in-flight blocking HTTP read may finish at its socket timeout), and isolate loading errors from receiver connectivity. General status polling remains authoritative for repeated receiver failures.

Visible SERVER/Spotify playback metadata is polled conservatively with general status, about every two seconds plus request duration. Settings/background stop unnecessary player reads. Lists load on entry, navigation, refresh or page change only. Cancelling does not repeat an uncertain write.

Unit tests use synthetic fixtures. Physical tests must confirm menu status/empty-list representation, source readiness timing, eight-slot paging including the last page, folder selection, track activation, all five playback controls, metadata, Return/root behavior, slow/disappearing NAS recovery and shared menu changes. External controllers can still race between preflight and PUT; the legacy API has no verified transaction token. There is no proven album subtype, persisted stable object ID, media URL or atomic breadcrumb in the implemented list grammar. NET RADIO was out of scope for v0.5; v0.6 is documented below.

## v0.6 NET RADIO — reference verified, physical verification PENDING

All new commands below are REFERENCE VERIFIED and covered by JVM fixtures/navigation tests. None is claimed PHYSICALLY VERIFIED. Fixtures are synthetic grammar examples, not captured radio catalogue responses.

Physical status supplied by the user: discovery, connection, source selection, Power/Standby, native +/- volume, Mute/Unmute, Tuner/presets/manual tuning, Spotify source/artwork and Settings gear/navigation PASS. SERVER core discovery, browsing and end-to-end playback are now VERIFIED (user report, 2026-09-30). No change to working power, volume, mute or tuner command XML was made.

### Exact evidence

- A, pinned R-N301 media_player.py above: _browse_net_radio_root (507 onward), _browse_net_radio_item (577 onward), _navigate_and_play_station (643 Direct_Sel, 646 Play); _update_media_playing (382–430) maps NET RADIO to NET_RADIO and reads playback metadata.
- B, pinned 71_YAMAHA_NP.pm above: directPlay (753 onward), directPlaySleepNetradio readiness handling (894–923; default 3 seconds), menu-level/path traversal (924–1048), listItem/Direct_Sel (1451 onward), List_Info parsing (2348–2442). B demonstrates asynchronous receiver navigation; its regex matching and different generic Player scope are not copied.
- D, existing receiver-served snapshots: .work/receiver-scr1.js ajxGetNetusbUnitName (1374) maps NET RADIO to NET_RADIO; ajxSetNetusbPlayback (1399), ajxSetNetUsbDSel (1409), ajxSetNetusbButton (1439), ajxGetNetusbListInfo (1450), ajxGetNetusbPlayInfo (1463), ajxSetNetusbPageUpDown (1468), ajxHandleNetusbPlayInfo (1544), ajxHandleNetusbList_Info (1617). The shared list handler uses eight slots for NET RADIO as well as SERVER. A 700 ms list-refresh delay follows navigation writes.
- D .work/receiver-scr0.js ChangeNetUsbBtnLyout (4139 onward): NET RADIO hides Pause/Rev/Fwd and ultimately Play, retaining Stop; onBtnNetStop (107) writes Stop. .work/receiver-home.html (724–743) selects the “stop only” button background. Therefore only Stop is exposed as a standalone radio playback control. Internal Play after station selection is independently verified by A.
- [Home Assistant core yamaha/media_player.py](https://github.com/home-assistant/core/blob/dev/homeassistant/components/yamaha/media_player.py), inspected 2026-09-27: play_media NET RADIO delegates logical paths such as Bookmarks>Internet>Radio Paradise to zctrl.net_radio. This corroborates path architecture, not a new XML command.

### Endpoint and responses

Every request uses HTTP POST /YamahaRemoteControl/ctrl on the verified local receiver, port 80, text/xml; charset=utf-8. XML cmd, not the HTTP method, selects GET/PUT. GET requires YAMAHA_AV rsp="GET" RC="0"; PUT requires rsp="PUT" RC="0", with optional echoed nodes. Nonzero RC is retained as a command rejection, not assigned an invented service meaning.

| Purpose | Method / XML path | Evidence | Test / hardware status |
|---|---|---|---|
| Activate source | PUT Main_Zone/Input/Input_Sel = NET RADIO | A async_select_source; existing input protocol | Unit tested; source selection already physical PASS, new browser activation pending |
| Current directory | GET NET_RADIO/List_Info = GetParam | A 507/580; D 1450/1617 | Unit tested / PENDING |
| Select directory or station | PUT NET_RADIO/List_Control/Direct_Sel = Line_1..Line_8 | A 577/643; D 1409 | Unit tested / PENDING |
| Back, bounded Home | PUT NET_RADIO/List_Control/Cursor = Return | D 1439; scr0 onBtnNetLeft; B list handling | Unit tested / PENDING |
| Previous/next page | PUT NET_RADIO/List_Control/Page = Up or Down | D 1468/1617; scr0 onBtnNetUp/Down | Unit tested / PENDING |
| Playback readback | GET NET_RADIO/Play_Info = GetParam | A _update_media_playing; D 1463/1544 | Unit tested / PENDING |
| Start selected station | PUT NET_RADIO/Play_Control/Playback = Play | A _navigate_and_play_station 646 | Unit tested / PENDING |
| Stop station | PUT NET_RADIO/Play_Control/Playback = Stop | D onBtnNetStop/ajxSetNetusbPlayback and Stop-only layout | Unit tested / PENDING |

Exact requests (UTF-8 XML declaration omitted here only for brevity):

```xml
<YAMAHA_AV cmd="PUT"><Main_Zone><Input><Input_Sel>NET RADIO</Input_Sel></Input></Main_Zone></YAMAHA_AV>
<YAMAHA_AV cmd="GET"><NET_RADIO><List_Info>GetParam</List_Info></NET_RADIO></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><NET_RADIO><List_Control><Direct_Sel>Line_2</Direct_Sel></List_Control></NET_RADIO></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><NET_RADIO><List_Control><Cursor>Return</Cursor></List_Control></NET_RADIO></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><NET_RADIO><List_Control><Page>Up</Page></List_Control></NET_RADIO></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><NET_RADIO><List_Control><Page>Down</Page></List_Control></NET_RADIO></YAMAHA_AV>
<YAMAHA_AV cmd="GET"><NET_RADIO><Play_Info>GetParam</Play_Info></NET_RADIO></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><NET_RADIO><Play_Control><Playback>Play</Playback></Play_Control></NET_RADIO></YAMAHA_AV>
<YAMAHA_AV cmd="PUT"><NET_RADIO><Play_Control><Playback>Stop</Playback></Play_Control></NET_RADIO></YAMAHA_AV>
```
### List parsing and navigation

Example GET response, synthetic and reference-derived:

```xml
<YAMAHA_AV rsp="GET" RC="0"><NET_RADIO><List_Info>
<Menu_Status>Ready</Menu_Status><Menu_Layer>2</Menu_Layer><Menu_Name>Locations</Menu_Name>
<Cursor_Position><Current_Line>9</Current_Line><Max_Line>18</Max_Line></Cursor_Position>
<Current_List>
<Line_1><Txt>Europe</Txt><Attribute>Container</Attribute></Line_1>
<Line_2><Txt>Example station</Txt><Attribute>Item</Attribute></Line_2>
</Current_List></List_Info></NET_RADIO></YAMAHA_AV>
```

The generic parser reads the exact NET_RADIO scope and maps its public source ID to NET RADIO. Container is DIRECTORY; Item is STATION; unknown/missing/Unselectable attributes remain visible but cannot be selected. Menu titles are not used to infer item type. XML entities are decoded and outer whitespace is trimmed; interior whitespace, case, Unicode and punctuation are preserved.

Ready requires positive Menu_Layer, valid Current_Line/Max_Line and eight-slot row identifiers. Empty lists are valid. Busy can omit positions/items; Loading is tolerated as a non-ready status but is not claimed as an observed R-N301 value. Other non-ready statuses become a service/menu error without inventing a specific RC interpretation.

Source activation confirms Basic_Status before any list access. Readiness uses at most 20 reads with one-second delays, with a 90-second deadline for the complete interactive NET RADIO operation. Navigation PUTs are never retried. After Direct_Sel, a directory must become Ready at the next layer; an unchanged Ready parent is not treated as success. Home sends at most 32 Return commands with decreasing-layer checks; Android Back leaves the browser only at layer 1.

Pages use floor((Current_Line-1)/8)+1 and ceil(Max_Line/8), verified in D's shared NET RADIO/SERVER controller. Fresh preflight compares source, layer, menu name, page, item list and total count. Page readback must move exactly one page without changing menu/count. Failed, slow or inconsistent navigation produces a browser error, not a disconnect or SSDP search. A command/read in progress may finish at the transport socket timeout before coroutine cancellation is observed.

### Station playback and metadata

A station receives Direct_Sel followed by the explicitly verified Play. Up to ten Play_Info reads one second apart wait for actual Play and non-Not-Ready availability. No predicted playback state is shown. Failed startup remains a radio error. The generic repository then refreshes Now Playing from the receiver.

```xml
<YAMAHA_AV rsp="GET" RC="0"><NET_RADIO><Play_Info>
<Feature_Availability>Ready</Feature_Availability><Playback_Info>Play</Playback_Info>
<Meta_Info><Station>Example radio</Station><Song>Current programme</Song><Artist>Host</Artist></Meta_Info>
</Play_Info></NET_RADIO></YAMAHA_AV>
```

Parser reads Station, Track/Song/Title, Artist and Album only when present. There is no invented separate Program XML field. Play/Pause/Stop map to explicit state; unknown values remain UNKNOWN. Optional shuffle/repeat metadata stays read-only. The generic player offers NET RADIO Stop only; internal Play is not a standalone resume promise. Stop validates its acknowledgement and reads back actual status/player state.

### Paths and future favorites

YamahaMediaBrowser is shared by SERVER and NET RADIO adapters. BrowserState carries a nullable List<String> navigationPath and optional RadioFavorite(displayName, navigationPath). Null means ancestors are unknown, for example entering an already-nested receiver menu. Root establishes an empty path; verified directory transitions append actual row names; Back removes one. External level/name changes, source changes, cancellation or failures invalidate path knowledge. No path or favorite is persisted in v0.6.

RadioPathNavigator.navigatePath first reaches root, returns to page one, and searches exact names across all pages at each level. It detects duplicate names even on later pages. It returns to the matched page and revalidates its original snapshot before selection. Intermediate components must be containers; the final component must be playable. Missing, ambiguous, changed, overlarge or timed-out paths fail without fuzzy substitution. Limits: 32 components, 64 pages per level, 120-second traversal deadline (125 seconds at repository boundary). Each traversal selection/page step checks that NET RADIO is still active.

Playback readback cannot prove station identity when the receiver supplies no identifying metadata. Preflight cannot eliminate the final race with a physical remote or another controller: no verified transaction token exists. Logical paths can become invalid or ambiguous when the external catalogue changes. These limitations must be checked on hardware, and favorite replay must retain the same fail-safe behavior.

### Lifecycle, logging and remaining uncertainty

List_Info is event-driven, with temporary bounded readiness polling only. Visible NET RADIO Now Playing uses the existing approximately two-second general polling loop; background cancels work. All commands share the repository mutex and transport spacing. Debug logs include source activation, list requests, status/layer/cursor/page, truncated selected title/path, playback state and browser failures. Raw XML/catalogues and stream URLs are not logged.

The current receiver-side catalogue/service availability, actual root categories, metadata fields, loading timing, station Play behavior, Return and pagination still require physical testing. NET RADIO does not query any third-party directory from Android. SERVER retains its existing protocol and 15-second operation deadline, with the original test suite preserved. No new network permission, dependency, external service or audio player was added.
## v0.7 rotary volume and daily-use UX

No new Yamaha XML path is introduced. Working power/mute/source/tuner/Spotify/SERVER/NET RADIO command builders and parsers are preserved.

### Official range evidence versus runtime limits

The [official R-N301 manual, RL edition](https://th.yamaha.com/files/download/other_assets/6/332426/R-N301_om_RL.pdf), printed p.35, lists numeric 1–99 in one-step increments plus a separate Max option under Max Volume; Initial Volume additionally lists Off/Mute. The previously referenced AB edition prints this on p.37. This is evidence about setup settings, NOT proof that every firmware exposes runtime Min/Max or that Max has numeric wire value 100.

The R-N301 integration A async_set_volume_level (262–265), existing physical integer-step PASS and the firmware's native Val/Exp/Unit response support integer target writes. Existing read-only System/Config and description evidence supplies no configured Max Volume path for this receiver. The v0.7 review found no confirmed R-N301 configured-Max read in A or D; B's multi-device volumeStraightMax/default must not be substituted.

Policy:
- Native rotary only for empty Unit, Exp=0 and a reported numeric value in 0..99.
- Application rotary guardrails are explicitly 1..99, based conservatively on the numeric documentation; they are not labelled receiver-reported bounds.
- Matching advertised integer limits, if present through the existing verified capability reader, narrow those guardrails. Different encoding/step or inconsistent bounds disable the rotary.
- A receiver-reported zero is shown without silently changing it; CCW at zero does nothing and CW can enter the documented numeric range.
- Configured maximum clamping is accepted through actual receiver readback. No guessed Max sentinel or dB conversion.
- Existing precision +/- handling remains unchanged, including its already supported zero endpoint.

### Gesture and write sequence

RotaryVolumePolicy computes atan2(y-centerY, x-centerX) in screen coordinates. The shortest normalized angular delta handles the wrap boundary; fractional motion accumulates at **12 degrees per native step** (30 steps/revolution). Native bounds clamp the preview. The indicator follows relative steps and is not a percent/range slider.

RotaryVolumeController separates receiver value, active preview and pending final target. Idle polling wins; active/pending polling cannot pull the preview backward. Compose uses a 180 dp Canvas control, a center dead zone and cancellation on interrupted/multitouch/out-of-ring gestures. Step crossings call Compose SegmentFrequentTick haptics; platform settings/capabilities decide whether a tick is delivered. No vibration permission is added.

**Final-only coalescing** is the chosen network strategy: zero writes during movement, one final target on successful release when changed. There is no periodic 100–150 ms debounce timer. This avoids a queued write per step and gives the legacy receiver the final user target. Cancelled gestures do not write. A second gesture is disabled while the final transaction completes.

Under the existing repository mutex:
1. Validate connected/on.
2. GET Main_Zone/Basic_Status and revalidate native encoding/current limits.
3. PUT the existing Main_Zone/Volume/Lvl target, preserving Exp=0 and empty Unit, unless readback already equals the target.
4. Validate PUT RC=0.
5. GET Basic_Status and display the actual value (including receiver clamping).

Example final target 45:

```xml
<YAMAHA_AV cmd="PUT"><Main_Zone><Volume><Lvl><Val>45</Val><Exp>0</Exp><Unit></Unit></Lvl></Volume></Main_Zone></YAMAHA_AV>
```

Endpoint remains HTTP POST /YamahaRemoteControl/ctrl. Exact path evidence is A async_set_volume_level and existing physically tested native +/- writes. The new multi-step rotary interaction is UNIT TESTED, PHYSICAL TEST PENDING.

A failed final write uses the existing non-intrusive command error and read-only status probe, never a repeated PUT or discovery. The preview resolves to the last actual receiver value; if reads also fail it is stale/disabled until polling recovers. External changes during a gesture can still race with final target selection: the user's final absolute target wins, followed by receiver readback.

### Quick Sources and recent source

Ordered IDs (maximum five) and the last explicitly selected/confirmed source are encoded into the existing local SharedPreferences SettingsStore. Unknown/unavailable saved entries cannot issue shortcut commands. Selection uses the existing repository source action and verified input XML. No input is changed automatically on startup. No database, media URL, Favorite or new network service is introduced.

### Tuner/RDS evidence and presentation

Existing GET Tuner/Play_Info and preset commands are unchanged. A _update_media_playing (371–435), D receiver-scr1.js (1995–2002) and the existing rn301-tuner-not-ready.xml capture verify Program_Service, Radio_Text_A, Radio_Text_B and Program_Type. Existing Tuning/Freq/Current and Signal_Info/Tuned,Stereo remain in use.

The parser already supplied all these fields; v0.7 adds presentation/fixture coverage without speculative fields. Empty RDS is omitted. Current station name precedes frequency, with program text/type and actual signal state when available. Preset cards use receiver titles/frequencies; live RDS is only attached to the current preset and never guessed from a frequency. Manual controls remain accessible below the responsive preset grid. Home Tuner metadata uses the same conservative foreground refresh cycle and stops in Settings/background.

### Generic player density and scope

PlayerPresentation chooses station/title first, deduplicates secondary metadata and collapses stopped/unknown players lacking metadata into a small source/state row. Playing/paused or meaningful metadata retains the full compact card; the dedicated player remains available. Existing per-source capabilities govern every playback action. No new player commands were added.

NET RADIO basic source activation, Yamaha/vTuner catalogue operation and station playback are now physically PASS. Edge-case paths, service failures and individual metadata/Stop behavior are not automatically marked passed. SERVER core discovery, browsing and end-to-end playback are now physically VERIFIED (user report, 2026-09-30). Custom radio URLs, YCast and DLNA infrastructure are outside v0.7.