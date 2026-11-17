# Veyra V1 scope

Veyra V1 is a local-first Android video experience. Audio/music and device streaming/casting are deferred to Veyra V2.

## Included in V1

- Browse videos from the Android device library through permission-aware MediaStore access.
- Add and remove user-selected folders through the Storage Access Framework.
- Search the library and sort by the supported name, date, duration, and file-size orders.
- Play local videos through Media3 with queue navigation, transport controls, saved progress, and Android media-session controls.
- Configure whether playback resumes from saved progress and what happens when a video ends.
- Provide video-tab settings and independently designed phone and tablet landscape interfaces.

## Product constraints

- Local browsing and playback must work without internet or another device.
- Use content URIs as media access identities; do not infer access from filenames or filesystem paths.
- Do not add image or audio permissions for video-library access.
- V1 playback is local. Do not introduce remote downloads or streaming as a substitute for local access.
- Phone and tablet landscape layouts are separate design targets. The tablet interface is not a scaled phone layout.
- Final screen geometry, visual styling, and interaction details must follow the user's supplied designs rather than inferred Material defaults.

## Deferred to V2

- Music/audio player experience.
- Device discovery, remote-library access, streaming, and casting.
- Collaborative comments synchronized between connected devices.
