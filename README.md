# Veyra

A local-first Android media player and multi-device media ecosystem.

## Project status

Veyra is at the foundation stage. The repository is being set up before feature implementation.

## Product principles

- Offline-first: local media browsing and playback must work without internet or another device.
- Android-native: built with Kotlin and Jetpack Compose.
- Distinct form factors: phone and tablet layouts are independently designed; the tablet UI is not a scaled phone layout.
- User-controlled sharing: devices share media only when hosting is explicitly enabled and access is authorized.
- No accidental downloads: remote playback should stream media rather than copy entire files by default.

## Planned areas

- Local video and music libraries
- Media playback and playback-state persistence
- A Stream area for connected devices, remote libraries, and casting
- Authorized cross-device comments
- Dedicated phone and tablet experiences

## Development

See `docs/architecture/overview.md` for the current architectural direction and decisions still awaiting detailed design.

## Status

No playback, library scanning, streaming, or final UI has been implemented yet. Those will be introduced in small, reviewable changes.
