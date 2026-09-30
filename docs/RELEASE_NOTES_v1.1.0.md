# Yamaha Receiver Controller v1.1.0

A focused browsing and navigation update for this Android controller for compatible Yamaha network receivers. Yamaha R-N301 remains the primary physically verified target.

- Updated application name and public project branding to **Yamaha Receiver Controller**; package and production signing identity are unchanged.
- SERVER/DLNA discovery through the receiver, multi-level folder browsing, track selection and actual receiver playback are now physically verified end-to-end.
- SERVER now uses the same compact browsing layout as Net Radio, with continuous directory lists instead of visible menu levels and page controls.
- SERVER pages load automatically within strict limits. Local search filters the loaded directory without sending search queries to the receiver.
- A global App Home button returns immediately to the Receiver screen without changing input or playback.
- Separate Server root and Radio root folder buttons return to their catalogue roots. Receiver / Now Playing navigation remains available.
- Existing Yamaha XML commands, SERVER playback and Net Radio behavior are preserved; no protocol regressions are intended. YTuner remains optional, independently configured, and neither bundled nor queried directly by the app.

## Installation

Requires Android 8.0+ and a Yamaha R-N301 reachable on the local network. Install **Yamaha-Receiver-Controller-v1.1.0.apk**, signed with the existing production key.

VersionName **1.1.0**, versionCode **16**, application ID **com.styl15hh1.rn301controller**.

Catalogue loading is limited to 64 pages and 90 seconds. Search covers only the loaded directory, not an entire DLNA library or radio catalogue.

The final signed artifact will undergo physical validation before publication. The exact validated APK will be published unchanged, without another rebuild.

Independent open-source project. Not affiliated with or endorsed by Yamaha Corporation.
