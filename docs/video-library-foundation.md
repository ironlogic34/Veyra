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

## Library search and sorting

`VideoLibraryQuery` provides pure, UI-independent collection operations:

- Case-insensitive search across display names and relative folder paths.
- Optional exact MIME-type filtering.
- Newest/oldest, name A–Z/Z–A, duration shortest/longest, and file size smallest/largest sorting.
- Deterministic tie-breaks and no mutation of the source list.

Phone and tablet interfaces consume the shared logic through their own separate UI implementations.

## Thumbnail loading and presentation

`VideoThumbnailDataSource` extracts preview frames from content URIs on the IO dispatcher, with scaled extraction where supported and a bounded in-memory LRU cache.

`VideoThumbnailFrame` is a reusable Compose preview frame with configurable aspect ratio, shape, and fallback surface. `VideoTransportControls` provides a reusable play/pause and seek surface while leaving placement and styling to each device layout.

## Metadata loading and enrichment

`VideoMetadataDataSource` reads duration, encoded width and height, rotation, bitrate, and container MIME type from a content URI using `MediaMetadataRetriever`. Metadata is loaded on the IO dispatcher.

`VideoMetadataRepository` enriches a caller-selected batch, preserving valid MediaStore values and filling missing fields from the container. It limits concurrent provider reads to three by default.

`VideoDisplayFormatters` formats duration, display resolution, and file size. Missing or invalid values display as `Unknown`.

## Playback engine and session

`VideoPlaybackEngine` wraps AndroidX Media3 ExoPlayer and supports single-item playback, queues, controls, seeking, repeat-one, listeners, and state snapshots.

`VideoPlaybackSession` now connects the engine to the SQLite history store and the existing resume/completion policies:

- Opening a video optionally restores a valid saved position.
- Opening a queue restores the selected video's position.
- Pausing persists non-zero progress.
- Reaching the end marks the item completed and applies Stop, Repeat, or Play Next.
- Session release awaits a final progress save before releasing the player and database.

The host must call `saveProgress()` when it enters a background/paused state and await `release()` when the session is finished. Periodic checkpoints, lifecycle integration, and notification/background playback remain follow-up work.

## Playback history storage

`VideoPlaybackHistoryStore` persists records in `veyra_video_history.db`. It supports save/update, lookup by exact content URI, recent-history queries, individual deletion, and clearing history. Database operations run on the IO dispatcher. Schema changes require explicit migrations; unsupported upgrades refuse to delete history.

## Functional access test screen

The current activity remains a functional access test surface, not the final library UI.

## Data rules

- Use content URIs for media access.
- Do not rely on filenames or filesystem paths as unique identities.
- Keep media querying, thumbnail extraction, metadata extraction, and database work off the main thread.
- Keep permission handling outside data sources.
- Keep phone and tablet library screens as separate implementations over shared data.

## Not implemented yet

- Thumbnail presentation in the library.
- Folder navigation and UI controls for search, sorting, and filtering.
- Periodic playback checkpoints and lifecycle/background session integration.
- Notification, lock-screen, and Bluetooth controls.
- Final phone and tablet video screens based on the user's design specifications.
