package com.example.musicplayer

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val contentUri: Uri,
    val albumId: Long?
)

enum class RepeatMode { OFF, ONE, ALL }