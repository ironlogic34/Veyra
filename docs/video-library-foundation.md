# Video library and playback foundation

## Device-library permission and folders

`VideoAccessPermission` centralizes version-specific video permission rules. Android 14+ distinguishes full access from selected-video access; Android 13 requests video access; Android 12 and earlier use external-storage read permission. Veyra does not request image or audio permissions.

The Storage Access Framework folder picker takes persistable read grants. `SafVideoFolderDataSource` recursively scans selected folders using content URIs, never guessed filesystem paths. Saved grants can be removed by releasing them.

## Library data and query

`VideoLibraryRepository` combines MediaStore results with saved SAF folders. A failure in one source does not discard successful results from another. Results are deduplicated by exact content URI.

`VideoLibraryQuery` supports case-insensitive name/folder search, exact MIME filtering, and eight deterministic sort orders. `VideoLibraryControls` provides caller-owned search, sort, and optional MIME filters. The phone and tablet screens remain separate consumers of this shared logic.

## Thumbnail and metadata

`VideoThumbnailDataSource` extracts preview frames from content URIs on IO and uses a bounded in-memory LRU cache. `VideoMetadataRepository` enriches missing duration, resolution, rotation, and bitrate fields with bounded concurrency. Display formatters handle duration, resolution, and file size.

## Playback session and host integration

`VideoPlaybackEngine` wraps Media3 ExoPlayer and supports single-item playback, queues, transport operations, listeners, and snapshots.

`VideoPlaybackSession` connects the player to SQLite history and completion policies:

- Opening an item or queue restores a valid saved position when enabled.
- Switching to another item saves the outgoing item's current position before replacing the media.
- Pause and lifecycle callbacks persist non-zero progress.
- Progress writes are serialized so an older asynchronous save cannot overwrite a newer one.
- Completion records the item and applies Stop, Repeat, or Play Next.
- Session release awaits the final save before releasing ExoPlayer and closing the history store.

The started and bound `VideoPlaybackService` owns one session independently of the Activity. The Activity binds to the service and attaches `VideoPlaybackSessionLifecycle` only for progress persistence; Activity destruction does not release the player. The service releases the session when Android destroys the service. Queue items carry display titles so system playback surfaces can identify the current video.

The functional host opens a tapped library item as the start of a queue, resumes its saved position, displays `VideoPlaybackContent`, and returns to the library with playback paused. Leaving the Activity in the background does not pause the player. The playback content remains a neutral building block; final phone and tablet arrangements remain independent design work.

## Player surface and controls

`VideoPlayerSurface` attaches the session-owned player to Media3 PlayerView and detaches the view without releasing the player. `VideoPlaybackContent` observes player state and connects the surface to reusable play/pause and seek controls. Screen owners retain control over placement and styling.

## Playback history

`VideoPlaybackHistoryStore` persists records by exact content URI in SQLite. It supports save/update, lookup, recent-history queries, individual deletion, and clearing history. Database work runs on IO. Schema upgrades require explicit migrations and never silently discard history.

## Playback feature phases

### Phase 1: background and system controls

- A service owns the playback session, so Activity navigation and recreation do not release the player.
- A foreground media-playback notification exposes previous, play/pause, and next controls.
- A Media3 MediaSession publishes transport state to Android lock-screen, headset, and Bluetooth media controls.
- ExoPlayer handles audio focus and uses a local wake lock while active playback runs with the screen off; the permission is declared and the foreground service provides the required playback context.
- Queue items include display titles for system playback surfaces.

### Phase 2: in-player features and preferences

- Subtitle track selection.
- Aspect-ratio and screen-orientation controls.
- Playback lock and sleep timer.
- Persistent playback preferences and remaining confirmed controls.

### Validation still required

- Full device testing for background playback, media notification visibility, Bluetooth/headset controls, lifecycle transitions, media-provider revocation, queues, and resume edge cases.
- Final phone and tablet playback layouts based on the user's supplied designs.

## Release work outside playback

Signed release APK builds, GitHub Releases, and the in-app update checker remain separate V1 release tasks.
