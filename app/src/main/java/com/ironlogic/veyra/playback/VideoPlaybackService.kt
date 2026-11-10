package com.ironlogic.veyra.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.ironlogic.veyra.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Owns the playback session independently of the Activity.
 *
 * The MediaSession publishes transport state to Android, lock-screen controls,
 * headsets, and Bluetooth devices. The service notification is updated from
 * the same ExoPlayer instance used by the in-app UI.
 */
class VideoPlaybackService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): VideoPlaybackService = this@VideoPlaybackService
    }

    lateinit var playbackSession: VideoPlaybackSession
        private set

    private lateinit var mediaSession: MediaSession
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var foreground = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackNotification()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackNotification()
        }

        override fun onMediaItemTransition(
            mediaItem: androidx.media3.common.MediaItem?,
            reason: Int
        ) {
            updatePlaybackNotification()
        }

        override fun onMediaMetadataChanged(
            mediaMetadata: androidx.media3.common.MediaMetadata
        ) {
            updatePlaybackNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        playbackSession = VideoPlaybackSession(applicationContext)
        mediaSession = MediaSession.Builder(this, playbackSession.engine.player)
            .setId("veyra-video-playback")
            .build()
        playbackSession.engine.player.addListener(playerListener)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> playbackSession.engine.play()
            ACTION_PAUSE -> playbackSession.engine.pause()
            ACTION_PREVIOUS -> playbackSession.engine.player.seekToPreviousMediaItem()
            ACTION_NEXT -> playbackSession.engine.seekToNextMediaItem()
            ACTION_STOP -> {
                playbackSession.engine.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                foreground = false
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun updatePlaybackNotification() {
        if (!::playbackSession.isInitialized || !::mediaSession.isInitialized) return

        val player = playbackSession.engine.player
        val notification = buildPlaybackNotification()
        if (player.isPlaying) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                foreground = true
            } catch (_: SecurityException) {
                // Playback remains usable in-app if Android rejects foreground promotion.
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, notification)
            }
        } else {
            if (foreground) {
                stopForeground(STOP_FOREGROUND_DETACH)
                foreground = false
            }
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildPlaybackNotification(): Notification {
        val player = playbackSession.engine.player
        val title = player.mediaMetadata.title?.toString()
            ?: player.currentMediaItem?.mediaMetadata?.title?.toString()
            ?: "Veyra video playback"
        val isPlaying = player.isPlaying
        val contentIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN_APP,
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "Playing video" else "Playback paused")
            .setContentIntent(contentIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .addAction(action(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS))
            .addAction(
                action(
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (isPlaying) "Pause" else "Play",
                    if (isPlaying) ACTION_PAUSE else ACTION_PLAY
                )
            )
            .addAction(action(android.R.drawable.ic_media_next, "Next", ACTION_NEXT))
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.platformToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun action(icon: Int, title: String, action: String): Notification.Action {
        val intent = Intent(this, VideoPlaybackService::class.java).setAction(action)
        val pendingIntent = PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Action.Builder(icon, title, pendingIntent).build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Video playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Playback controls for videos playing in Veyra."
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        if (::playbackSession.isInitialized) {
            playbackSession.engine.player.removeListener(playerListener)
        }
        if (::mediaSession.isInitialized) mediaSession.release()
        if (::playbackSession.isInitialized) {
            serviceScope.launch { playbackSession.release() }
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.ironlogic.veyra.playback.PLAY"
        const val ACTION_PAUSE = "com.ironlogic.veyra.playback.PAUSE"
        const val ACTION_PREVIOUS = "com.ironlogic.veyra.playback.PREVIOUS"
        const val ACTION_NEXT = "com.ironlogic.veyra.playback.NEXT"
        const val ACTION_STOP = "com.ironlogic.veyra.playback.STOP"

        private const val CHANNEL_ID = "veyra_video_playback"
        private const val NOTIFICATION_ID = 4101
        private const val REQUEST_OPEN_APP = 4102
    }
}
