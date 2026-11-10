# Video library foundation

## Device-library permission

`VideoAccessPermission` centralizes version-specific permission rules:

- Android 14+ (API 34+): requests `READ_MEDIA_VIDEO` and `READ_MEDIA_VISUAL_USER_SELECTED`. Full and selected-video-only access are represented separately.
- Android 13 (API 33): requests `READ_MEDIA_VIDEO`.
- Android 12 and earlier: requests `READ_EXTERNAL_STORAGE`.

Veyra does not request image or audio permissions. Permission prompts occur only after the user chooses to grant or manage video access. Selected-video access is never mislabeled as full-library access.

## User-selected folders

`OpenDocumentTree` launches Android's Storage Access Framework picker. Veyra takes a persistable read grant and reloads the OS-maintained persisted grant list on later launches.

`SafVideoFolderDataSource` recursively walks child documents, detects video MIME types, and returns content URIs. It never converts document URIs into filesystem paths. A folder grant can be removed by releasing it.

Some SAF providers do not expose duration in their document cursor, so SAF items currently use a duration of zero until metadata is loaded for that item.

## Library repository

`VideoLibraryRepository` combines MediaStore results (when device-library access exists) with each saved SAF folder. A failure in one source does not discard successful results from other sources.

Results are deduplicated by exact content URI, then sorted newest-first with a case-insensitive filename tie-breaker. Different provider URIs for the same underlying file may still appear as separate entries; Veyra does not guess identity from filenames.

## Thumbnail loading

`VideoThumbnailDataSource` extracts a preview frame from content URIs, including SAF document URIs, on the IO dispatcher. It uses scaled frame extraction where supported and a bounded in-memory LRU cache. Thumbnails are not persisted to disk. A revoked grant or unsupported provider returns no thumbnail rather than crashing the library.

## Metadata loading and enrichment

`VideoMetadataDataSource` reads duration, encoded width and height, rotation, bitrate, and container MIME type from a content URI using `MediaMetadataRetriever`. Metadata is loaded on the IO dispatcher.

`VideoMetadataRepository` enriches a caller-selected batch, preserving valid MediaStore values and filling missing fields from the container. It limits concurrent provider reads to three by default. The caller should request metadata for visible or selected items rather than the entire library during every refresh.

A provider failure returns the original item unchanged. One inaccessible video therefore does not abort enrichment of the rest of the batch.

## Metadata display formatting

`VideoDisplayFormatters` provides Android-UI-independent formatting for duration, display resolution, and file size. Missing or invalid values display as `Unknown`. Quarter-turn video rotation is reflected in the displayed resolution, and file sizes use binary units (KiB, MiB, GiB).

## Functional access test screen

The temporary screen can request/manage device video permission, add folders, list/remove saved grants, refresh and merge library results, and report revoked access or scan errors. This is a functional test surface, not the final library UI.

## Data rules

- Use content URIs for media access.
- Do not rely on filenames or filesystem paths as unique identities.
- Keep media querying, thumbnail extraction, and metadata extraction off the main thread.
- Keep permission handling outside data sources.
- Phone and tablet library screens remain separate UI implementations over shared data.

## Not implemented yet

- Thumbnail presentation in the library UI
- Search, sorting, filtering, and folder navigation UI
- Database-backed playback history and resume positions
- Playback engine
