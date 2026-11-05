# Veyra 1.0 product scope

## Goal

Ship one complete, coherent local video experience before expanding Veyra into a broader multi-media and multi-device product.

## Included in V1

### Video library
- Browse videos indexed on the device.
- Browse folders and subfolders.
- Support the full device video library through Android media permission.
- Support user-selected folders through Storage Access Framework with persisted access.
- Present useful video metadata and thumbnails.
- Handle permission denial and revoked folder access clearly.

### Playback
- Local video playback.
- Resume from the last saved playback position.
- Subtitle support, including local subtitle selection and the previously requested subtitle search flow.
- Configurable fixed orientation.
- Aspect-ratio selector.
- Completion behavior: Stop, Repeat, or Play next.
- Playback lock on the right edge, including volume interaction.
- Background/screen-off playback.
- Notification, lock-screen, and Bluetooth playback controls.
- Metadata editing entry point.
- Sleep timer in the playback overflow menu.

### Settings
- Video-tab settings.
- Playback settings.
- Settings must be organized around the video experience in V1; audio and streaming settings are not part of this release.

### Form factors
- Phone UI follows its own approved design.
- Tablet landscape UI follows its own hand-drawn/approved design.
- The tablet layout is not a scaled phone layout.

## Explicitly deferred to V2

- Music/audio player.
- Stream tab functionality.
- Device pairing and discovery.
- Remote library browsing.
- Casting and cross-device playback control.
- Cross-device collaborative comments.

## Release rule

Do not add V2 functionality to V1 merely because the architecture can accommodate it. Keep interfaces extensible, but implement and test the V1 video experience first.

## Current implementation stage

The first MediaStore data source can enumerate indexed videos after permission has been granted. Runtime permission UI, SAF folder access, thumbnails, persistence, playback, settings, and final UI remain future implementation work.
