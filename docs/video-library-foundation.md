# Video library foundation

## Current implementation

`MediaStoreVideoDataSource` queries Android's external video collection and returns `VideoItem` records.

Each item currently contains:
- Content URI
- Display name
- Duration
- File size
- Last-modified timestamp
- MIME type
- Relative folder path when available (Android 10+)

Results are ordered by modified time, newest first, with media ID as a deterministic secondary sort.

## Permission boundary

The caller must request permission before querying:
- Android 13 and newer: `READ_MEDIA_VIDEO`
- Android 12 and older: `READ_EXTERNAL_STORAGE`

The data source does not launch permission prompts and does not silently request access.

## Folder access

User-selected folder support will use the Storage Access Framework and persisted URI permissions. It is intentionally separate from MediaStore's device-wide indexed collection. That source is not implemented yet.

## Design and data rules

- Use content URIs for media access.
- Do not rely on filenames or filesystem paths as unique identities.
- Keep media querying off the main thread.
- Keep permission handling outside the data source.
- Phone and tablet library screens remain separate UI implementations over shared data.

## Not implemented yet

- Runtime permission flow and partial/denied permission states
- SAF folder picker and persisted folder grants
- Thumbnail loading and caching
- Search, sorting, filtering, and folder navigation UI
- Database-backed playback history and resume positions
- Playback engine
