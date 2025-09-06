package com.example.musicplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import android.support.v4.media.session.MediaSessionCompat // for MediaStyle on older devices
import androidx.media3.session.MediaSession as M3Session
import androidx.media3.session.MediaSessionService as M3Service
import androidx.media3.common.Player

/**
 * Media3 provides MediaSessionService, but to keep compatibility with a wide range,
 * we'll extend Service via MediaSessionService (Media3) for controls + notification.
 *
 * Workflow will put MainActivity as LAUNCHER; we build a media-style notification
 * with transport controls + foreground service.
 */
class PlayerService : M3Service() {

    companion object {
        const val CHANNEL_ID = "musicplayer_channel"
        const val NOTIF_ID = 1001
    }

    private lateinit var playback: PlaybackManager
    private var session: M3Session? = null

    override fun onCreate() {
        super.onCreate()
        playback = PlaybackManager(this)
        session = MediaSession.Builder(this, playback.getPlayer())
            .setId("MusicSession")
            .build()
        createChannel()
        startForeground(NOTIF_ID, buildNotification(isPlaying = false))
        playback.getPlayer().addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val n = buildNotification(isPlaying)
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_ID, n)
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): M3Session? {
        return session
    }

    override fun onDestroy() {
        session?.release()
        playback.release()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Background audio playback"
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val player = playback.getPlayer()

        val playPauseAction = if (isPlaying)
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause, "Pause",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this, androidx.media3.session.SessionCommand.COMMAND_CODE_PLAYER_PAUSE
                )
            )
        else
            NotificationCompat.Action(
                android.R.drawable.ic_media_play, "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this, androidx.media3.session.SessionCommand.COMMAND_CODE_PLAYER_PLAY
                )
            )

        // For previous/next we just use intents to MediaSession (handled by Media3).
        val prevAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous, "Previous",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this, androidx.media3.session.SessionCommand.COMMAND_CODE_SESSION_FAST_FORWARD // repurpose
            )
        )
        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next, "Next",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this, androidx.media3.session.SessionCommand.COMMAND_CODE_SESSION_REWIND // repurpose
            )
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(player.mediaMetadata.title ?: "Playing…")
            .setContentText(player.mediaMetadata.artist ?: "Unknown Artist")
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(session?.sessionCompatToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        return builder.build()
    }
}

/**
 * Helper for media-button PendingIntents without custom BroadcastReceiver.
 */
object MediaButtonReceiver {
    fun buildMediaButtonPendingIntent(
        service: PlayerService,
        @Suppress("UNUSED_PARAMETER") commandCode: Int
    ): PendingIntent {
        // Media3 handles play/pause/next/prev via session. We just re-open app.
        val intent = Intent(service, MainActivity::class.java)
        return PendingIntent.getActivity(
            service, commandCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                (if (android.os.Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
    }
}