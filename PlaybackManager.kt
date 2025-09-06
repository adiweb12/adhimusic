package com.example.musicplayer

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlin.random.Random

class PlaybackManager(private val context: Context) {

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private val queue = mutableListOf<Song>()
    private var shuffled = false
    var repeatMode: RepeatMode = RepeatMode.OFF
        set(value) {
            field = value
            player.repeatMode = when (value) {
                RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            }
        }

    fun getPlayer(): ExoPlayer = player

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        queue.clear()
        queue.addAll(songs)
        player.clearMediaItems()
        player.addMediaItems(queue.map { it.toMediaItem() })
        player.prepare()
        player.seekTo(startIndex, 0)
    }

    fun play() = player.play()
    fun pause() = player.pause()

    fun playSong(song: Song, all: List<Song>) {
        setQueue(all, all.indexOfFirst { it.id == song.id }.coerceAtLeast(0))
        play()
    }

    fun next() = player.seekToNextMediaItem()
    fun previous() = player.seekToPreviousMediaItem()

    fun toggleShuffle() {
        shuffled = !shuffled
        player.shuffleModeEnabled = shuffled
        if (shuffled) player.shuffleModeEnabled = true
    }

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun release() = player.release()

    private fun Song.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(contentUri)
            .setMediaId(id.toString())
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .build()
            )
            .build()
    }
}