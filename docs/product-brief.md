# Veyra — Product Brief

## Product definition

**Veyra** is a private, local-first Android media environment: a calm place to live with personal video now, and personal music later. It should feel more intentional than a file browser and less distracting than an online content platform.

The product is not trying to beat VLC at codec breadth on day one. It is trying to make a user's own media feel remembered, organized and easy to return to.

## Scope decision

### Veyra V1 — Video

V1 is deliberately video-only:

- Local video discovery through MediaStore.
- User-selected folders through Android Storage Access Framework.
- Folder browsing, thumbnails and metadata.
- Continue Watching with persisted playback position.
- Playback queue, previous/next, repeat and Play Next.
- Resume automatically from the last position.
- Playback history.
- Aspect-ratio and resize modes without stretching.
- Orientation preferences.
- Subtitle selection and loading.
- Playback lock.
- Sleep timer.
- Background/system media controls where Android permits it.
- Related-videos drawer.
- Video-specific settings.
- Independent phone and tablet landscape layouts.

### Veyra V2 — Music and connected media

Deferred until V1 is stable:

- Music library and audio player.
- Audio-focused interface inspired by selected Lastwave interaction ideas, not copied wholesale.
- Remote media libraries.
- Connected-device streaming and casting.
- Sender and receiver controls.
- Manual hosting toggle.
- Reconnection and progress restoration.
- Collaborative connected-device comments.

## Original feature added: Memory Marks

**Memory Marks** are lightweight personal bookmarks attached to a moment in a video. A mark stores:

- Timestamp.
- Optional short note.
- Optional mood label: calm, funny, intense, beautiful, important or custom.
- Creation date.

A user can jump back to a mark from the video detail screen or the player. Marks are local-only and private. They are more personal than generic bookmarks and create a foundation for future “memory reels” without introducing social features.

### Why Memory Marks belong in V1

They make Veyra feel like a personal media environment, they are technically manageable after playback progress is working, and they reinforce the product's privacy-first identity. They should be implemented after the core player is reliable, not before.

## Recommended technical stack

Use **Android-native Kotlin with Jetpack Compose**.

- **UI:** Kotlin + Jetpack Compose + Material 3 primitives with a custom Veyra design system.
- **Playback:** AndroidX Media3 / ExoPlayer for playback, transport controls and media sessions.
- **Persistence:** Room for library records, playback progress, history, settings and Memory Marks.
- **Media discovery:** MediaStore for device-wide video discovery.
- **Folder permissions:** Storage Access Framework with persisted URI permissions.
- **Thumbnails:** Media3/Android thumbnail extraction with a disk cache; avoid decoding full videos in list composables.
- **Navigation:** Navigation Compose.
- **Background behavior:** MediaSessionService for ongoing playback and system controls.
- **Testing:** Unit tests for reducers/repositories, Compose UI tests for critical interaction states, and a small device-validation matrix.
- **Build:** Gradle Kotlin DSL, version catalogs and GitHub Actions CI.

Do not use Electron, Flutter or React Native for V1. They would add unnecessary layers around Android media permissions, MediaStore, Media3 and device-specific playback behavior.

## Architecture boundaries

Keep the application in clear layers:

1. **data** — MediaStore, SAF, Room, thumbnail cache and settings storage.
2. **domain** — media items, progress rules, queue rules, completion behavior and Memory Marks.
3. **playback** — Media3 player, MediaSessionService and playback state bridge.
4. **ui** — phone and tablet experiences, screens and reusable Veyra components.

The player service must not know about composable screens. Screens must consume state rather than directly manipulating persistence. Playback progress should be written periodically and at lifecycle boundaries, with debouncing to avoid excessive writes.

## V1 information architecture

### Phone

- **Library** — primary video grid/list, Continue Watching, recent additions and folders.
- **Player** — immersive playback with a compact floating control layer.
- **History** — recently watched videos and Memory Marks.
- **Settings** — appearance, playback, resume, orientation, aspect ratio, subtitles, lock and sleep timer.

Use a floating bottom navigation dock on phone. The selected item may expand to reveal its label, but the dock must remain readable and usable without animation.

### Tablet landscape

Do not scale the phone layout. Use a separate two-pane composition:

- Persistent left navigation rail.
- Large content canvas.
- Optional right-side queue or related-videos panel.
- Player controls arranged for landscape reachability.

The tablet should feel like a media desk, while the phone should feel like a pocket library.

## Visual direction

Use a dark, cinematic base with a warm accent rather than generic blue:

- Base: near-black graphite.
- Surface: charcoal glass with restrained translucency.
- Accent: ember/copper orange for active playback and progress.
- Secondary accent: muted violet for selected navigation states.
- Text: warm white and soft gray.
- Typography: a clean geometric sans for UI, with slightly wider tracking for section labels.

Use glass-like surfaces sparingly. The visual identity should come from spacing, contrast, motion and playback context—not from putting blur on every card.

Motion rules:

- Press feedback around 120–160ms.
- Drawers and panels around 220–300ms.
- No essential information depends on animation.
- Respect reduced-motion preferences.

## MVP implementation order

### Milestone 1 — Foundation

- Android project and CI.
- Compose theme and Veyra tokens.
- Navigation shell with phone/tablet layout branches.
- Room schema and repository interfaces.
- MediaStore permission flow.

### Milestone 2 — Library

- Scan local videos.
- Import selected folders through SAF.
- Persist media metadata.
- Thumbnail generation and caching.
- Empty, loading, permission-denied and scan-error states.
- Continue Watching section.

### Milestone 3 — Playback

- Media3 player screen.
- Play/pause, seek, previous/next and queue.
- Progress persistence and automatic resume.
- Completion behavior, history and cleanup.
- Aspect-ratio modes and orientation preference.

### Milestone 4 — Veyra identity

- Floating navigation dock.
- Related-videos drawer.
- Subtitle controls.
- Playback lock and sleep timer.
- Settings screen.
- Tablet-specific landscape player and library.

### Milestone 5 — Memory Marks

- Add/remove mark from player.
- Optional note and mood.
- Mark list on detail/history screen.
- Seek directly to a mark.
- Tests for persistence and deletion.

## Explicit non-goals for V1

- Online search or online recommendations.
- Accounts, analytics or cloud synchronization.
- Casting and remote libraries.
- Music playback.
- AI-generated tags or cloud AI processing.
- Social comments.
- A large theme marketplace.

## Quality bar

Veyra V1 is not complete when the screens compile. It is complete when:

- A user can find a local video without confusion.
- A video resumes correctly after force-closing and reopening the app.
- Video aspect ratio is never unexpectedly stretched.
- Folder permission loss is handled clearly.
- Queue and completion behavior are predictable.
- Phone and tablet layouts are intentionally different.
- Settings persist across restarts.
- Playback works with system controls and headphones where supported.
- CI passes and the core behavior is covered by tests.

## First repository task

Before changing the repository, inspect the current branch and existing code. Preserve newer implementation work. Do not recreate the Android foundation if it already exists. The first implementation pass should audit the repository against this brief and produce a gap list, followed by small coherent changes.
