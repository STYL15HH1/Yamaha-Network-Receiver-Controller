# Third-party notices

The vector paths in ic_radio, ic_disc, ic_music, ic_server, ic_internet_radio, ic_input, ic_previous, ic_next, ic_settings, ic_back, ic_play_arrow, ic_pause, ic_stop, ic_fast_forward and ic_fast_rewind are from Google's Material Design Icons (24px Material Icons), licensed under Apache License 2.0.

Source: https://github.com/google/material-design-icons
License: [material-icons-LICENSE.txt](licenses/material-icons-LICENSE.txt)

The v0.5 Settings icon was re-imported from src/action/settings/materialicons/24px.svg, including its nested visible path; the old empty path was a conversion defect.

Optical, coaxial, line, folder, home and chevron vectors are original simple project assets under the project's MIT license.

## Supplied Spotify artwork (v0.5 replacement)

Original: Image/Spotify.png, 512 × 256 RGBA, unchanged.
Derived resource: app/src/main/res/drawable-nodpi/spotify_wordmark.png, 499 × 150 RGBA.

The white mark was extracted using alpha and luminance to remove the source's nearly transparent black matte/shadow. Transparent outer space was cropped. No background is included. Compose uses aspect-preserving layout, theme foreground tint and shared disabled alpha.

Original SHA-256: D5B7C42A50F1A9FC177E43F6217F028B00B21E0DC7AB93BDF3DD6145DFC789BB
Derivative SHA-256: C324715B140B283E724CE05F6F76B48EEA1C3ACBFA18D34C2071675818428D16

The earlier supplied artwork is no longer used by the packaged resource.

## Supplied AirPlay artwork

Original: Image/airplay.png, 500 × 500 RGBA, unchanged.
Derived resource: app/src/main/res/drawable-nodpi/airplay_artwork.png, 409 × 358 RGBA (v0.6 symbol only).

The symbol above the transparent gap was cropped from the original (x=45..453, y=0..357), excluding the embedded AirPlay lettering. Visible artwork was converted to white with alpha retained. Shape/proportions remain intact. The generic tile supplies AirPlay exactly once. No background is included. Compose foreground tint ensures light-theme contrast.

Original SHA-256: A7A9F440A63492E141EBAA372BBA83B3672093969D5560B4BBE9AB83CB95DA49
Derivative SHA-256: D2343CE242A63791DB71FD883AE287C1E29DBFE6737534E7B53E185B981645C6

Spotify and AirPlay names/logos remain their respective owners' trademarks and are not relicensed under the project's MIT license. No affiliation or endorsement is implied.

## Supplied Yamaha launcher artwork

The user supplied Image/Yamaha logo.jpg (encoded content is WebP). The original remains unchanged. Derived launcher assets isolate the tuning-fork emblem, preserve proportions and provide adaptive foreground/background, monochrome and legacy fallback resources at all five Android densities.

The Yamaha name and emblem remain Yamaha trademarks, not relicensed under the project's MIT license. This unofficial controller does not imply Yamaha endorsement.

## Other dependencies and references

AndroidX, Kotlin, coroutines and test dependencies retain upstream licenses. Yamaha reference repositories and receiver-served JavaScript were inspected for protocol research, not copied into the application or used as runtime dependencies. Receiver firmware scripts remain only in ignored local research files.

No runtime image library or remote image service is used.


## v1.0.0 redistribution review

The existing project evidence records user-supplied Spotify, AirPlay and Yamaha artwork and derivative hashes, but includes no license, written permission, or documented brand-usage grant establishing redistribution rights for these assets in a public APK. Trademark attribution alone does not establish permission. **Public release concern: the owner must establish applicable permission/terms before distributing these packaged assets.** No replacement artwork was downloaded and no licensing permission is claimed here.

The MIT license covers original project code/assets only. The included Apache 2.0 Material Icons license remains applicable to upstream icon paths. Original source images stay in ignored local storage; only the existing derived application resources are tracked.
