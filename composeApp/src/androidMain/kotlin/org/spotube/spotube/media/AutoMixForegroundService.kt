package org.spotube.spotube.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.spotube.spotube.MainActivity
import org.spotube.spotube.R
import org.spotube.spotube.core.automix.AutoMixPlaybackManager
import org.spotube.spotube.core.automix.AutoMixController
import org.spotube.spotube.core.i18n.getString
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.auto_mix_notification_text
import spotube.composeapp.generated.resources.auto_mix_pause
import spotube.composeapp.generated.resources.auto_mix_play
import spotube.composeapp.generated.resources.auto_mix_title

class AutoMixForegroundService : Service(), KoinComponent {
    companion object {
        private const val CHANNEL_ID = "automix_playback"
        private const val NOTIFICATION_ID = 4243
        private const val ACTION_TOGGLE = "org.spotube.spotube.automix.TOGGLE"

        fun start(context: Context) {
            val intent = Intent(context, AutoMixForegroundService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AutoMixForegroundService::class.java))
        }
    }

    private data class Strings(
        val title: String,
        val text: String,
        val play: String,
        val pause: String
    )

    private val manager: AutoMixPlaybackManager by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null
    private var mediaSession: MediaSessionCompat? = null
    @Volatile private var strings: Strings? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(strings)
        scope.launch {
            val loaded = runCatching {
                Strings(
                    title = getString(Res.string.auto_mix_title),
                    text = getString(Res.string.auto_mix_notification_text),
                    play = getString(Res.string.auto_mix_play),
                    pause = getString(Res.string.auto_mix_pause)
                )
            }.getOrNull() ?: return@launch
            strings = loaded
            ensureChannel(loaded)
            if (manager.state.value.isActive) updateNotification(manager.state.value)
        }
        mediaSession = MediaSessionCompat(this, "AutoMixSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    if (manager.state.value.isActive && !manager.state.value.isPlaying) {
                        manager.togglePlayPause()
                    }
                }

                override fun onPause() {
                    if (manager.state.value.isActive && manager.state.value.isPlaying) {
                        manager.togglePlayPause()
                    }
                }


            })
            isActive = true
        }
    }

    private fun ensureChannel(labels: Strings?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                labels?.title ?: "Auto Mix",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = labels?.text ?: "Auto Mix playback"
                setShowBadge(false)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> if (manager.state.value.isActive) manager.togglePlayPause()
        }
        val state = manager.state.value
        startForeground(NOTIFICATION_ID, buildNotification(state))
        if (observeJob == null) {
            observeJob = scope.launch {
                manager.state
                    .map { state ->
                        listOf(
                            state.current?.id,
                            state.isPlaying,
                            state.title,
                            state.positionMs / 1_000L
                        )
                    }
                    .distinctUntilChanged()
                    .collect { updateNotification(manager.state.value) }
            }
        }
        return START_NOT_STICKY
    }

    private fun updateNotification(state: AutoMixPlaybackManager.State) {
        updateMediaSession(state)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun updateMediaSession(state: AutoMixPlaybackManager.State) {
        val session = mediaSession ?: return
        val track = state.current
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track?.title ?: "")
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    track?.artists?.joinToString(", ") { it.name }.orEmpty()
                )
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track?.album?.title.orEmpty())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, state.durationMs)
                .build()
        )
        val playbackState = if (state.isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
                .setState(playbackState, state.positionMs, if (state.isPlaying) 1f else 0f)
                .build()
        )
    }

    private fun buildNotification(state: AutoMixPlaybackManager.State): android.app.Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val labels = strings
        val title = state.current?.title ?: labels?.title ?: "Auto Mix"
        val artistText = state.current?.artists?.joinToString(", ") { it.name }
            ?: labels?.text ?: "Auto Mix playback"
        val text = listOfNotNull(artistText, state.planLevel?.name?.uppercase())
            .joinToString(" - ")
        val playPauseLabel = if (state.isPlaying) {
            labels?.pause ?: "Pause"
        } else {
            labels?.play ?: "Play"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_spotube)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                playPauseLabel,
                serviceAction(ACTION_TOGGLE, 1)
            )
        mediaSession?.let { session ->
            builder.setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0)
            )
        }
        return builder.build()
    }

    private fun serviceAction(action: String, requestCode: Int): PendingIntent {
        return PendingIntent.getService(
            this,
            requestCode,
            Intent(this, AutoMixForegroundService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        observeJob?.cancel()
        observeJob = null
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
