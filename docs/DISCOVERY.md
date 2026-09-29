# Receiver discovery (v1.0.0; introduced in v0.2)

The discovery abstraction is independent of Compose:

YamahaDiscovery -> SsdpYamahaDiscovery -> SsdpSearch + ReceiverVerifier.

AndroidSsdpSearch owns the local-network socket and multicast lock. YamahaReceiverVerifier checks UPnP identity and the legacy Yamaha control API. State is exposed by ReceiverViewModel.

## Why SSDP

The R-N301's actual read-only UPnP description at port 8080 identifies urn:schemas-upnp-org:device:MediaRenderer:1. FHEM's YAMAHA_NP_getMediaRendererDesc uses this same description path. Neither the primary Home Assistant config flow nor the Angular reference supplies an Android discovery implementation; this is a new standards-based SSDP implementation, not copied architecture. There is no IP-range sweep.

The optional YamahaRemoteControl/desc.xml returned 404 on the inspected receiver. Discovery does not require it.

## Wire behavior

UDP multicast destination: 239.255.255.250:1900. M-SEARCH requests use MX=2 and MAN="ssdp:discover". Search targets are MediaRenderer:1 and upnp:rootdevice, with one repeat after approximately one second. TTL=2. Replies are collected for 4.5 seconds on an ephemeral socket bound to a local Wi-Fi/Ethernet IPv4 address.

Maximum 256 datagrams per scan; 8192-byte receive bound. At most 16 distinct responding IPv4 hosts are verified. The complete scan has a 12-second coroutine deadline; an in-flight platform HTTP read can take up to its 3-second I/O timeout to unwind cancellation. Already-verified candidates are retained if the deadline expires. No repeated automatic scan loop is scheduled.

Replies must be HTTP 200 SSDP responses with the requested ST and a single LOCATION. LOCATION must be HTTP, use the same numeric private/link-local IPv4 as the datagram sender, port 80 or 8080, and have no credentials, query or fragment. Duplicate hosts are collapsed before verification. Compatible results are deduplicated by UDN (address fallback).

For each candidate:
1. GET the safe advertised LOCATION, without redirects/proxies and with bounded response size.
2. Require a UPnP MediaRenderer:1 with manufacturer Yamaha Corporation and model R-N301.
3. GET System/Config through POST /YamahaRemoteControl/ctrl; require R-N301.
4. GET Main_Zone/Basic_Status; require a valid Yamaha response.
5. Offer Connect to the user. Do not automatically connect to discovered devices.

Public destinations, cross-host LOCATION URLs, arbitrary ports and non-Yamaha/non-R-N301 devices are excluded.

## Android requirements

Existing INTERNET and ACCESS_NETWORK_STATE permissions are retained. Added normal permissions:
- ACCESS_WIFI_STATE
- CHANGE_WIFI_MULTICAST_STATE

A non-reference-counted WifiManager.MulticastLock is acquired only during Wi-Fi search and released in finally. No location, Bluetooth, background service, notifications or analytics permissions are added. Wi-Fi/Ethernet is selected explicitly for discovery and receiver HTTP traffic rather than depending on cellular default routing. No INTERNET validation capability is required, so a local Wi-Fi network without Internet can be used.

The app still targets SDK 36. Android's documented implicit LAN access through INTERNET applies at this target; target-37 LAN runtime permission migration remains future work.

## Startup and lifecycle

The last successfully selected address is persisted only after a valid connection. It is tried first once on launch; failure starts one finite discovery attempt. With no saved address, discovery starts on first foreground entry. Scan again is explicit. Discovery and polling pause/cancel when backgrounded; source actions cancel scanning. Manual IPv4/hostname entry and Test connection remain in Connection settings / Add manually.

## Physical validation status

UPnP identity and legacy API reads were observed from the development workstation. The workstation's multicast probe found no candidates. The subsequent v0.2.0 Android test passed SSDP discovery, automatic IP detection, R-N301 identification and connection on the physical phone and receiver. This supersedes the original pending-phone-test status. Multiple receivers and other network configurations remain separate test scenarios.

## Sources

- [FHEM YAMAHA_NP_getMediaRendererDesc](https://github.com/mhop/fhem-mirror/blob/04580b9847ce3f8fced54196ea2f943b2d8d2d80/fhem/FHEM/71_YAMAHA_NP.pm)
- [Primary config_flow.py](https://github.com/rihokirss/homeasisstant-rn301/blob/87c1f789bd2d13f8e965913108524f4f39c9ed26/custom_components/yamaha_rn301/config_flow.py)
- [UPnP MediaRenderer device specification](https://www.upnp.org/specs/av/UPnP-av-MediaRenderer-v3-Device-20101231.pdf)
- [Android MulticastLock](https://developer.android.com/reference/android/net/wifi/WifiManager.MulticastLock)
- [Android local-network permissions](https://developer.android.com/privacy-and-security/local-network-permission)
