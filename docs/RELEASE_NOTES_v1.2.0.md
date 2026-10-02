# Yamaha Receiver Controller v1.2.0

Version **1.2.0 / 17**, application ID `com.styl15hh1.rn301controller`. The signed APK has been physically accepted on Yamaha R-N301. Publication will be performed manually later; nothing has been uploaded by this preparation step. Existing signing configuration is unchanged.

## Home Screen Widget

- Power On / Standby and manual Refresh.
- Current source and source-specific metadata, including available station, RDS or track information.
- Up to four existing Favorite Sources, with selected-source highlighting. One to three favorites are centered; four fill the row.
- Responsive resizing, compact layouts, light/dark appearance and offline/cached state with a stale update time.
- Tap receiver information to open the app. Multiple instances share the saved receiver and favorites.
- No continuous background polling or volume controls. Explicit actions use finite platform jobs and the existing receiver commands.

## Compatibility Report

**Settings → Receiver → Compatibility Report** provides read-only GET diagnostics:

- Receiver model and firmware, capability map and Main Zone state.
- Separate evidence for declared capability, API read success and runtime readiness; “Not Ready” is not treated as unsupported.
- Tuner / RDS / stored-preset diagnostics and receiver-reported preset count.
- SERVER / NET RADIO / Spotify network source diagnostics and AirPlay capability declarations where available.
- Explicit Copy Report and Share Report actions, without automatic telemetry or uploads.
- Privacy-safe export excludes IP address, System_ID, network identifiers, raw XML/errors, credentials and station/track metadata. Unknown identity formats are omitted conservatively.

A failed description XML request does not invalidate successful control API reads. Diagnostics may inspect other local Yamaha models but do not verify their control commands or enable normal controls on them.

## Tuner preset frequencies

- Each returned preset shows its number plus stored frequency in smaller, centered secondary text.
- FM and AM formats are supported, for example `99.10 MHz` and `1134 kHz`. Frequency text stays on one line and shrinks when needed.
- The receiver's existing `Preset_Sel_Item` response is authoritative. The existing tuner refresh reloads overwritten frequencies; no local preset-frequency database, added polling or RDS scanning is used.
- Preset count is dynamic. Missing or unexpected titles retain the selectable preset number and omit the frequency.
- Preset tiles do not show or discover station names; live station/RDS information remains in the player.

## Compact Tuner player

- Station and frequency share one row; station text remains dominant.
- Band, preset and actual reception/tuned state use one compact status line.
- Radio Text is limited to one line with ellipsis. Program Type and RDS clock are omitted from the main card.
- FM / AM / Auto / Mono controls share a row when width permits, retaining selection and enabled states. Previous / Next remain available.
- Reduced spacing makes the player substantially shorter: approximately 407 to 208 dp in a 379 dp-wide automated test fixture (about 49%). Actual height depends on content, font size and available width.

## Compatibility and preserved behavior

**Yamaha R-N301 remains the only physically verified model.** Other Yamaha models are not claimed physically compatible. Existing volume, power, sources, playback, browsing/search/pagination, settings and eight languages remain available.

Preset creation, editing and deletion are not implemented; manage stored presets using the receiver/remote. Scenes, Sleep Timer and automatic station-name discovery are not implemented.

See [final validation and accepted artifact](VALIDATION_v1.2.0.md) and [final screenshot inventory](screenshots/README.md).

Independent open-source project. Not affiliated with or endorsed by Yamaha Corporation.
