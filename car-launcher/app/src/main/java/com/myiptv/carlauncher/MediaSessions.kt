package com.myiptv.carlauncher

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val packageName: String,
    val title: String,
    val artist: String,
    val art: ImageBitmap?,
    val isPlaying: Boolean,
)

/** What's playing in any app (Spotify, YouTube Music, radio apps...), plus its controls. */
class MediaSessions(private val context: Context) {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val listener = ComponentName(context, MediaAccessService::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val _hasAccess = MutableStateFlow(false)
    /** False until the user turns on notification access for this app. */
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private var controller: MediaController? = null
    private var lastArt: Pair<Bitmap, ImageBitmap>? = null
    private var listening = false

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = pick(activeSessions())
    }

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
        pick(sessions.orEmpty())
    }

    fun start() {
        _hasAccess.value = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        if (!_hasAccess.value || listening) return
        try {
            manager.addOnActiveSessionsChangedListener(sessionsChanged, listener, handler)
            listening = true
            pick(activeSessions())
        } catch (e: SecurityException) {
            _hasAccess.value = false
        }
    }

    fun stop() {
        if (listening) {
            runCatching { manager.removeOnActiveSessionsChangedListener(sessionsChanged) }
            listening = false
        }
        setController(null)
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
            c.transportControls.pause()
        } else {
            c.transportControls.play()
        }
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    private fun activeSessions(): List<MediaController> =
        try {
            manager.getActiveSessions(listener)
        } catch (e: SecurityException) {
            emptyList()
        }

    /** Prefers whatever is playing right now, else the most recently active session. */
    private fun pick(sessions: List<MediaController>) {
        val playing = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        val current = sessions.firstOrNull { it.sessionToken == controller?.sessionToken }
        setController(playing ?: current ?: sessions.firstOrNull())
    }

    private fun setController(next: MediaController?) {
        if (next?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = next
            next?.registerCallback(callback, handler)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        val metadata = c?.metadata
        if (c == null || metadata == null) {
            _nowPlaying.value = null
            return
        }
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        _nowPlaying.value = NowPlaying(
            packageName = c.packageName,
            title = title,
            artist = artist,
            art = bitmap?.let(::toImage),
            isPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING,
        )
    }

    // Playback updates arrive every few seconds; don't re-convert the same album art each time
    private fun toImage(bitmap: Bitmap): ImageBitmap {
        lastArt?.let { (source, image) -> if (source === bitmap) return image }
        return bitmap.asImageBitmap().also { lastArt = bitmap to it }
    }
}
