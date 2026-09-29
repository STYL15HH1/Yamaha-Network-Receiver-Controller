# Yamaha Network Receiver Controller

A modern native Android controller for the **Yamaha R-N301**. Control your receiver directly over its local Yamaha HTTP/XML interface—without an application cloud backend, Yamaha account, or Spotify account integration in the app.

**Stable version: v1.0.0 · Android 8.0+ · Eight languages · MIT**

[Download from GitHub Releases](https://github.com/STYL15HH1/Yamaha-Network-Receiver-Controller/releases) · [Release notes](docs/RELEASE_NOTES_v1.0.0.md) · [Protocol documentation](docs/YAMAHA_PROTOCOL.md)

**Unofficial project:** this is an independent community project, not affiliated with or endorsed by Yamaha.

## Screenshots

<table>
<tr>
  <td align="center" valign="top"><img src="docs/screenshots/Screenshot_20260929_093550_Yamaha%20Network%20Receiver%20Controller.jpg" width="220" alt="Home &amp; rotary volume"><br><strong>Home &amp; rotary volume</strong></td>
  <td align="center" valign="top"><img src="docs/screenshots/Screenshot_20260929_093608_Yamaha%20Network%20Receiver%20Controller.jpg" width="220" alt="Tuner &amp; presets"><br><strong>Tuner &amp; presets</strong></td>
  <td align="center" valign="top"><img src="docs/screenshots/Screenshot_20260929_093618_Yamaha%20Network%20Receiver%20Controller.jpg" width="220" alt="Tuner Now Playing"><br><strong>Tuner Now Playing</strong></td>
  <td align="center" valign="top"><img src="docs/screenshots/Screenshot_20260929_093638_Yamaha%20Network%20Receiver%20Controller.jpg" width="220" alt="Source selection"><br><strong>Source selection</strong></td>
</tr>
<tr>
  <td align="center" valign="top"><img src="docs/screenshots/Screenshot_20260929_093648_Yamaha%20Network%20Receiver%20Controller.jpg" width="220" alt="Application language"><br><strong>Application language</strong></td>
  <td align="center" valign="top"><img src="docs/screenshots/Screenshot_20260929_093705_Yamaha%20Network%20Receiver%20Controller.jpg" width="220" alt="Net Radio catalogue"><br><strong>Net Radio catalogue</strong></td>
  <td align="center" valign="top"><img src="docs/screenshots/Screenshot_20260929_093717_Yamaha%20Network%20Receiver%20Controller.jpg" width="220" alt="Four Favorites"><br><strong>Four Favorites</strong></td>
  <td align="center" valign="top"><img src="docs/screenshots/Screenshot_20260929_093727_Yamaha%20Network%20Receiver%20Controller.jpg" width="220" alt="Compact Home &amp; Spotify"><br><strong>Compact Home &amp; Spotify</strong></td>
</tr>
</table>

Actual phone captures supplied for public use, shown at their original proportions. Radio metadata and catalogue contents come from the receiver and may vary; no particular catalogue backend is required.

## Features

| Area | Controls and capabilities |
| --- | --- |
| Receiver | Automatic SSDP discovery, saved receiver reconnect, manual IPv4/hostname fallback, receiver status, Power / Standby and firmware information |
| Volume | Native Yamaha values, live rotary control, precision − / + and Mute / Unmute |
| Favorites | Up to four configurable quick sources, with saved order |
| Tuner | FM / AM, saved-preset recall, manual tuning, seek, FM Auto / Mono and available RDS metadata |
| Spotify | Receiver-provided track/artist/album metadata and Previous / Play-Pause / Next |
| SERVER / DLNA | Browse receiver-visible media, navigate folders and control playback |
| Net Radio | Multi-level receiver catalogue browsing, search within the loaded menu, station selection, Now Playing and Stop |

Sources include **Tuner, CD, Optical, Line 1–3, Spotify, SERVER, Net Radio and AirPlay**, plus **Coaxial when advertised/supported by the receiver**. AirPlay is source selection only. Available controls reflect the receiver's supported capabilities.

Volume remains in native receiver units; the app does not invent a dB conversion. Spotify is controlled through the R-N301, without a Spotify SDK, Web API or OAuth flow.

## Languages and appearance

Choose **System default** or English, Español, Deutsch, Français, Italiano, Polski, 한국어 or 日本語.

Appearance supports **System / Light / Dark**, with saved preferences.

## Requirements

- A **Yamaha R-N301** network receiver. Other Yamaha models have not been validated and are not guaranteed to work.
- **Android 8.0 or newer** (minSdk 26).
- Phone and receiver reachable on the same local network, with the receiver's network features available.
- Network standby enabled on the receiver if you want to wake it from standby.

Manual connection is available when the network blocks multicast discovery.

## Installation

1. Open [GitHub Releases](https://github.com/STYL15HH1/Yamaha-Network-Receiver-Controller/releases).
2. Download the signed **Yamaha-Network-Receiver-Controller-v1.0.0.apk** release asset.
3. Open the APK on your phone and, when prompted, allow installation from the trusted browser or file manager.
4. Discover your receiver or enter its address in Settings.

Android may display security or Play Protect warnings for applications installed outside Google Play. Verify the download source and review any warning before proceeding; do not disable Play Protect globally.

Updates require the same package and signing identity. A differently signed development installation cannot be updated in place with the production APK; uninstalling it removes local settings.

## Net Radio and YTuner

The app browses the Internet Radio catalogue **exposed by the receiver**. Catalogue navigation and station selection use Yamaha XML; the receiver retrieves and plays the audio. The app does not proxy radio streams, query Radio Browser directly, or submit arbitrary stream URLs.

The receiver can use its normal Internet Radio service where available. **YTuner is not an application dependency and is not bundled.** Advanced users may independently configure it at receiver/network level as a self-hosted catalogue alternative. Service availability and catalogue contents depend on the chosen backend.

## Privacy and network model

Receiver control happens over the local network. The app has **no analytics, advertising, telemetry, application cloud backend or required user account**.

Legacy receiver control uses cleartext HTTP. The transport restricts destinations to local/private IPv4 addresses, uses fixed receiver endpoints and disables proxies/redirects. Discovery uses SSDP multicast. Hostnames are resolved through Android's network resolver; numeric addresses avoid hostname lookups. The author link opens an external browser only when selected.

The receiver and its Spotify, radio or DLNA services have their own network behavior; this app does not provide or authenticate those services.

## Architecture

~~~text
Jetpack Compose
    ↓
ViewModel / StateFlow
    ↓
YamahaRepository
    ↓
Yamaha XML protocol / HTTP transport
    ↓
Yamaha R-N301
~~~

XML construction and parsing are isolated from Compose. Communication stays local, and the app does not proxy audio. SERVER and Net Radio content is handled by the receiver itself.

## Build from source

Use **JDK 17**, the checked-in **Gradle 9.6.0 wrapper**, Android SDK platform 37 and build tools 36.0.0. Accept SDK licenses and configure ANDROID_HOME or an untracked local.properties.

~~~sh
./gradlew testDebugUnitTest assembleDebug
~~~

Use gradlew.bat on Windows. Debug APKs are for development, not the normal end-user installation.

Signed release builds use owner-controlled Gradle properties configured outside the repository:

- YAMAHA_RELEASE_STORE_FILE
- YAMAHA_RELEASE_KEY_ALIAS
- YAMAHA_RELEASE_STORE_PASSWORD
- YAMAHA_RELEASE_KEY_PASSWORD

~~~sh
./gradlew assembleRelease lint lintRelease
~~~

Release builds fail if signing properties are missing; debug builds remain independent. Never commit keystores or signing credentials. See [release preparation](docs/RELEASE_PREPARATION.md) for validation and signing evidence.

## Known limitations

- Other receiver models are not guaranteed.
- AirPlay is input selection only; Spotify Stop, Shuffle and Repeat are not implemented.
- Preset storage, tone controls, speaker A/B selection and Sleep are not implemented.
- Net Radio search covers the loaded menu, not the entire catalogue. Large/slow menus can reach traversal limits.
- Another controller can change the receiver menu during browsing. Unavailable services/media and missing metadata are reported without inventing content.
- No direct stream URLs, artwork fetching, background service or home-screen widget.

## Documentation

- [Yamaha protocol and verified commands](docs/YAMAHA_PROTOCOL.md)
- [Capability audit and unresolved features](docs/RN301_CAPABILITY_AUDIT.md)
- [Discovery](docs/DISCOVERY.md)
- [Validation](docs/VALIDATION.md)
- [Physical test findings](docs/PHYSICAL_TEST_CHECKLIST.md)
- [v1.0.0 release notes](docs/RELEASE_NOTES_v1.0.0.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)

## License and attribution

Original project source is licensed under the [MIT License](LICENSE). Third-party resources and supplied brand artwork retain their respective ownership and terms; the MIT license does not grant trademark or logo rights.

Yamaha is a trademark of Yamaha Corporation, Spotify of Spotify AB, and AirPlay of Apple Inc. This project is not affiliated with, endorsed by, or supported by those companies.

Created by [STYL15HH1](https://github.com/STYL15HH1).
