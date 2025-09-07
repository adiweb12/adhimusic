package com.example.musicplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.common.Player

/**
 * The Media3 MediaSessionService is a robust way to handle background media playback.
 * It manages the MediaSession and provides a media-style notification with transport controls
 * that work out-of-the-box with the system.
 */
class PlayerService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "musicplayer_channel"
        const val NOTIF_ID = 1001
    }

    private lateinit var playbackManager: PlaybackManager
    private var mediaSession: MediaSession? = null

    // A simple MediaButtonReceiver is no longer needed with Media3. The Session handles everything.
    // The previous implementation was a custom object which would not work.

    override fun onCreate() {
        super.onCreate()
        playbackManager = PlaybackManager(this)

        mediaSession = MediaSession.Builder(this, playbackManager.getPlayer())
            .setId("MusicSession")
            .build()

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification(isPlaying = false))
        
        playbackManager.getPlayer().addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Update the notification when the playing state changes.
                val notification = buildNotification(isPlaying)
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIF_ID, notification)
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        // Return the MediaSession to the caller (e.g., MainActivity's MediaController)
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.release()
        playbackManager.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Background audio playback controls"
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val player = playbackManager.getPlayer()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's icon
            .setContentTitle(player.mediaMetadata.title ?: "Playing...")
            .setContentText(player.mediaMetadata.artist ?: "Unknown Artist")
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    mediaSession?.getMediaButtonIntent()
                )
            )
            .addAction(
                if (isPlaying) {
                    NotificationCompat.Action(
                        android.R.drawable.ic_media_pause,
                        "Pause",
                        mediaSession?.getMediaButtonIntent()
                    )
                } else {
                    NotificationCompat.Action(
                        android.R.drawable.ic_media_play,
                        "Play",
                        mediaSession?.getMediaButtonIntent()
                    )
                }
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_next,
                    "Next",
                    mediaSession?.getMediaButtonIntent()
                )
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionCompatToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0)
                )
            )

        return builder.build()
    }
}
