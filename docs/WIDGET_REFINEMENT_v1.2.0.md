# v1.2.0 final widget implementation

This document describes the final widget included in the owner-accepted v1.2.0 APK. Earlier intermediate widget hashes and validation counts are superseded by [final v1.2.0 validation](VALIDATION_v1.2.0.md). Physical acceptance of the signed APK was supplied by the owner; this local documentation pass does not claim additional hardware testing.

## Composition and behavior

- Normal heights (180 dp and above): receiver header, separate Power / Standby and Refresh buttons, a full-width current-source card, and 56 dp Favorite Sources tiles. The source card grows with available height.
- Below 180 dp, a compact layout preserves the controls and favorites with a 132 dp resize floor. Provider minimum width is 250 dp, with horizontal/vertical resizing and a 4×2 launcher hint on API 31+.
- One to three favorites are centered; four fill the row. Missing tiles occupy no slots. Existing source order, configuration, icons and selected-source colors are retained.
- Current-source card opens the app and displays source-specific available metadata: Tuner station/frequency/reception/RDS, Spotify title/artist, SERVER title/artist/album, NET RADIO station/title, or a simple input name.
- Source name remains distinct from secondary metadata. Missing or repeated metadata does not create blank placeholder rows. Rich content is height-limited.
- Cached content is retained offline. The stale/offline time appears when applicable; successful operation does not add an unnecessary timestamp row.
- Light/dark appearance, localization, multiple instances and existing action/readback behavior are retained.
- No volume controls, continuous background polling or metadata discovery. Explicit actions use finite jobs; interrupted control actions are not replayed automatically.

## Evidence

Focused and full-suite widget coverage includes favorite counts/centering, selected states, responsive geometry, light/dark colors, rich/missing metadata, offline cache recovery, multiple instances and immutable action identities. Current full-suite and lint results are recorded in the linked final validation document.

The supplied [final widget screenshot](screenshots/Yamaha-Receiver-Controller-Widget-v1.2.0.jpg) shows three centered favorites with Spotify selected. Temporary native RemoteViews previews are generated test artifacts, not release screenshots; they are excluded from the repository.
