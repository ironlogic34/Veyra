# Veyra architecture overview

## Product foundation

Veyra is an Android-native, local-first media application. Local browsing and playback must remain useful with no internet connection and no paired device. Multi-device streaming extends the local experience; it must not become a dependency of it.

The phone and tablet experiences are separate design targets. The tablet landscape interface is not a scaled version of the phone interface, and neither should be redesigned by inference from the other.

## Current implementation baseline

- Language: Kotlin
- UI: Jetpack Compose
- Build: Gradle Kotlin DSL
- Initial structure: one Android application module, with package boundaries kept clear until there is a concrete reason to split modules
- Namespace/application ID: `com.ironlogic.veyra`
- Initial SDK baseline: min 26, compile/target 36. Revisit after the supported-device range is explicitly agreed.

These are foundation choices, not a claim that the final product architecture is complete.

## Functional areas

1. **Local library** — index and browse device media, with support for user-selected folders.
2. **Video** — local playback and the independently designed phone/tablet playback surfaces.
3. **Music** — a player experience informed by the interaction patterns in LastWave-Native, without copying unrelated product behavior.
4. **Stream** — the primary place to discover paired devices, browse authorized remote libraries, and manage streaming/casting sessions. This replaces Search in the primary navigation plan.
5. **Settings** — permissions, appearance, library behavior, device identity, and sharing controls.
6. **Collaborative comments** — comments attached to a media identity and synchronized between authorized connected devices. Authorized devices can add and delete comments.

## Local media and playback direction

- Use Android MediaStore for indexed device media.
- Use Storage Access Framework for user-selected folders and persistable URI permissions.
- Evaluate AndroidX Media3/ExoPlayer for playback.
- Persist playback position and enough media identity information to restore a session safely.
- Do not treat a filename or path alone as a stable media identity.

No media scanning or playback code is implemented in this foundation commit.

## Multi-device direction

Two distinct user-facing capabilities are required:

- **Remote library access:** browse another verified device's permitted library and play a selected item without first copying the entire file.
- **Casting:** send the currently selected/playing media to another device, with playback controls available on both sender and receiver.

Both phone and tablet devices can act as host or receiver. Hosting is manually enabled by the user; pairing a device does not automatically enable library sharing. Access should be configurable per device and per shared folder/library.

Architectural direction:

- Local network discovery: Android Network Service Discovery (NSD/mDNS).
- Media transport: HTTP range requests or an equivalent seek-capable streaming transport.
- Playback control: a separate authenticated control channel, likely WebSocket-based.
- Remote connectivity: investigate a rendezvous service with direct peer connectivity and an encrypted relay fallback. The cloud service should coordinate connections, not store users' media.
- Security: verified device identity, explicit host approval, authenticated/encrypted sessions, revocation, and scoped permissions.
- Recovery: persist session state and reconnect only after re-authentication and media identity validation. Handle unavailable or moved media explicitly.

These are architectural directions. Protocols, server provider, relay policy, and implementation details are not yet locked.

## Collaborative comment direction

A comment needs a stable ID, media ID, author device identity, creation time, and content. Additions and deletions must synchronize across authorized connected devices. Deletion synchronization needs a durable representation so an old device cannot silently resurrect a deleted comment. Conflict behavior and whether comments can be timestamp-linked to playback remain to be specified.

## UI reference study

LastWave-Native is the interaction reference for the music experience and selected shared interaction patterns. Relevant source areas previously reviewed include:

- `ui/shell/MainShell.kt` — floating navigation
- `ui/theme/LiquidGlass.kt` — glass interaction
- `ui/common/ExpressiveMotion.kt` — shared motion
- `ui/common/ExpressiveShuffleButton.kt` and `ConnectedButtonGroup.kt` — button behavior
- `ui/player/DraggableNowPlayingButton.kt` — draggable mini-player

The implementation should reproduce the intended behavior and feel, not blindly copy source code or assume every LastWave feature belongs in Veyra. Visual and gesture validation against the reference is still required.

Reference repository: https://github.com/Clash-Projects/LastWave-Native

## Decisions intentionally left open

- Final phone and tablet UI specifications and exact component measurements
- Final colour, typography, shape, and motion tokens
- Library database and migration strategy
- Exact media identity strategy
- Streaming protocol details and rendezvous/relay provider
- Whether comments support playback timestamps
- Transcoding support and device capability negotiation
- Final supported Android version range

Do not silently invent these decisions during implementation. Record proposals and get confirmation when they materially affect product behavior.
