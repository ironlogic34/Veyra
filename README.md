# Veyra

A local-first Android video player, with a broader media ecosystem planned for later versions.

## Version 1.0 scope

Veyra 1.0 focuses on a complete local video experience:

- Video library and folder browsing
- Local video playback and playback controls
- Video-tab settings
- Playback settings
- Independently designed phone and tablet landscape interfaces

Audio/music and device streaming/casting are explicitly deferred to Veyra 2.0.

## Product principles

- Offline-first: local video browsing and playback must work without internet or another device.
- Android-native: built with Kotlin and Jetpack Compose.
- Distinct form factors: phone and tablet layouts are independently designed; the tablet UI is not a scaled phone layout.
- User-controlled media access: use Android media permissions for the device library and Storage Access Framework for user-selected folders.
- No accidental downloads: V1 playback is local; remote streaming is a later-version feature.

## Development status

The repository contains the Android foundation, shared UI tokens, initial interaction primitives, and the first MediaStore video-library data source.

- [V1 scope](docs/v1-scope.md)
- [Architecture overview](docs/architecture/overview.md)
- [Design-system foundation](docs/design-system.md)
- [Video library foundation](docs/video-library-foundation.md)

Playback, final screens, and settings are not yet implemented.
