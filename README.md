# Veyra

Veyra is a private, local-first Android media environment. This branch is a fresh V1 implementation started from the clean `main` branch; the historical `setup/android-foundation` branch is preserved separately for reference and is not being overwritten.

## Current branch

`dev/fresh-v1-foundation`

The first pass establishes a clean Android-native project using Kotlin, Jetpack Compose and Material 3. It includes an intentionally small library shell with separate phone and tablet landscape layout branches, plus the initial dark cinematic visual language.

The current branch includes permission-aware MediaStore scanning, local thumbnails, and the first Media3 playback screen. After granting video access, the library renders local videos without copying or uploading media. Tapping a video opens playback, and Veyra saves the last position for Continue Watching.

## V1 direction

V1 is video-only: local discovery, folder access, thumbnails, playback, progress persistence, history, subtitles, playback preferences and Memory Marks. Music, streaming, casting and connected-device features remain V2 work.

## Build

Open the repository in Android Studio and run the `app` configuration. The project uses Gradle Kotlin DSL, Kotlin 2.2.20, Android Gradle Plugin 8.13.2, compile/target SDK 36 and min SDK 26.

The sandbox used for this initial commit does not include a local Android SDK or Gradle installation, so device compilation must be verified in Android Studio or Termux with the Android toolchain installed.

## Repository branches

- `main`: original project documentation.
- `setup/android-foundation`: preserved historical prototype and tests.
- `dev/fresh-v1-foundation`: clean implementation path from `main`.
