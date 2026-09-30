# Yamaha Receiver Controller v1.0.0

The first stable release of an Android controller for Yamaha network receivers using Yamaha's legacy local XML control API. **Yamaha R-N301** is the primary physically verified target.

This replacement changes public branding and links only. It preserves the original v1.0.0 functionality, package, version and signing identity.

## Highlights

- Automatic SSDP discovery, saved receiver reconnect and manual address fallback.
- Power / Standby, native live rotary Volume, precision − / + and Mute.
- Source selection and up to four configurable Favorites.
- FM/AM Tuner, saved presets, manual tuning/seek, FM Auto/Mono and available RDS metadata.
- Spotify receiver-local metadata and Previous / Play-Pause / Next controls.
- SERVER/DLNA receiver browsing and playback.
- Net Radio catalogue browsing, local menu search, station selection, Now Playing and Stop.
- Compatibility with normal receiver radio services and independently configured catalogue alternatives such as YTuner. **YTuner is optional, not bundled or required.**
- Eight languages, System default language, and System / Light / Dark appearance.
- No analytics, advertising, telemetry, app cloud backend or app-managed Yamaha/Spotify authentication.

## Installation

Requires **Android 8.0+** and a Yamaha R-N301 reachable on the local network. Download the signed **Yamaha-Receiver-Controller-v1.0.0.apk** from the release assets.

Android may warn about installations outside Google Play. Verify the source and review warnings; do not disable Play Protect globally. An existing installation signed with a different development key must be uninstalled before installing the production APK, removing its local preferences.

Release identity: versionName **1.0.0**, versionCode **15**, application ID **com.styl15hh1.rn301controller**.

Production signing certificate SHA-256:
AB:2A:CE:5A:9A:33:D2:F9:EC:C1:D3:48:2D:FA:51:58:DD:AE:44:A5:89:79:FA:18:5E:F1:32:96:E0:1D:A0:92

## Known limitations

Other Yamaha models are not guaranteed. Volume is native, not inferred dB. AirPlay supports input selection only. Spotify Stop/Shuffle/Repeat, preset storage, tone/speaker/Sleep controls and direct radio stream URLs are not implemented. Receiver services determine catalogue/media availability and metadata; concurrent controllers can invalidate browsing state. Net Radio search is limited to the loaded menu and traversal is bounded.

Independent open-source project. Not affiliated with or endorsed by Yamaha Corporation. See the project README and third-party notices for licensing and attribution.
