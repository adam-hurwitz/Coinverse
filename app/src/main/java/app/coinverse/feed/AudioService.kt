package app.coinverse.feed

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Binder
import android.os.Handler
import app.coinverse.App
import app.coinverse.MainActivity
import app.coinverse.R.drawable.ic_coinverse_notification_24dp
import app.coinverse.R.string.app_name
import app.coinverse.R.string.notification_channel_description
import app.coinverse.analytics.Analytics
import app.coinverse.feed.models.Content
import app.coinverse.feed.models.ContentToPlay
import app.coinverse.utils.CONTENT_SELECTED_ACTION
import app.coinverse.utils.CONTENT_SELECTED_BITMAP_KEY
import app.coinverse.utils.CONTENT_TO_PLAY_KEY
import app.coinverse.utils.EXOPLAYER_NOTIFICATION_ID
import app.coinverse.utils.OPEN_CONTENT_FROM_NOTIFICATION_KEY
import app.coinverse.utils.OPEN_FROM_NOTIFICATION_ACTION
import app.coinverse.utils.PLAYER_ACTION
import app.coinverse.utils.PLAYER_KEY
import app.coinverse.utils.PLAY_OR_PAUSE_PRESSED_KEY
import app.coinverse.utils.PlayerActionType.PAUSE
import app.coinverse.utils.PlayerActionType.PLAY
import app.coinverse.utils.PlayerActionType.STOP
import app.coinverse.utils.byteArrayToBitmap
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoRendererEventListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

private val LOG_TAG = AudioService::class.java.simpleName

class AudioService : Service() {
    @Inject
    lateinit var analytics: Analytics

    private var startId = 0
    private var player: SimpleExoPlayer? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var content = Content()
    private var startAutoPlay: Boolean = false
    private var startWindow: Int = 0
    private var startPosition: Long = 0
    private var seekToPositionMillis = 0
    private var playOrPausePressed = false

    /**
     * This class will be what is returned when an activity binds to this service.
     * The activity will also use this to know what it can get from our service to know
     * about the video playback.
     */
    inner class AudioServiceBinder : Binder() {
        /**
         * This method should be used only for setting the exoplayer instance.
         * If exoplayer's internal are altered or accessed we can not guarantee
         * things will work correctly.
         */
        fun getExoPlayerInstance() = player
    }

    override fun onCreate() {
        super.onCreate()
        (applicationContext as App).component.inject(this)
    }

    // Called first time audiocast is loaded.
    override fun onBind(intent: Intent?) = AudioServiceBinder().apply {
        player = SimpleExoPlayer.Builder(
            applicationContext,
            AudioOnlyRenderersFactory(applicationContext)
        ).build()
        player?.setHandleWakeLock(true)
        player?.addListener(PlayerEventListener())
        buildNotification(
            contentToPlay = intent!!.getParcelableExtra(CONTENT_TO_PLAY_KEY),
            bitmap = intent.getByteArrayExtra(CONTENT_SELECTED_BITMAP_KEY)
                .byteArrayToBitmap(applicationContext)
        )
    }

    override fun onDestroy() {
        playerNotificationManager?.setPlayer(null)
        player?.release()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.startId = startId
        val intent = intent.apply {
            when (intent?.action) {
                CONTENT_SELECTED_ACTION -> {
                    val contentToPlay = this?.getParcelableExtra<ContentToPlay>(CONTENT_TO_PLAY_KEY)
                    /** New content playing */
                    if (!contentToPlay?.content?.equals(content)!!) {
                        content = contentToPlay.content
                        seekToPositionMillis = 0
                        analytics.updateStartActionsAndAnalytics(content)
                        player?.prepare(
                            ProgressiveMediaSource.Factory(
                                DefaultDataSourceFactory(
                                    applicationContext,
                                    Util.getUserAgent(applicationContext, getString(app_name))
                                )
                            )
                                .createMediaSource(Uri.parse(content.audioUrl))
                        )
                        playerNotificationManager?.setPlayer(null)
                        buildNotification(
                            intent.getParcelableExtra(CONTENT_TO_PLAY_KEY),
                            intent.getByteArrayExtra(CONTENT_SELECTED_BITMAP_KEY)
                                .byteArrayToBitmap(applicationContext)
                        )
                    }
                }
                PLAYER_ACTION -> when (this?.getStringExtra(PLAYER_KEY)) {
                    PAUSE.name -> {
                        player?.playWhenReady = false
                        playOrPausePressed = this.getBooleanExtra(PLAY_OR_PAUSE_PRESSED_KEY, false)
                    }
                    PLAY.name -> {
                        player?.playWhenReady = true
                        playOrPausePressed = this.getBooleanExtra(PLAY_OR_PAUSE_PRESSED_KEY, false)
                    }
                    STOP.name -> stopService()
                    else -> FirebaseCrashlytics.getInstance()
                        .log("$LOG_TAG ExoPlayer controls error")
                }
                else -> FirebaseCrashlytics.getInstance()
                    .log("$LOG_TAG ExoPlayer onStartCommand error")
            }
        }
        return START_REDELIVER_INTENT
    }

    private inner class AudioOnlyRenderersFactory(var context: Context) : RenderersFactory {
        override fun createRenderers(
            eventHandler: Handler,
            videoRendererEventListener: VideoRendererEventListener,
            audioRendererEventListener: AudioRendererEventListener,
            textRendererOutput: TextOutput,
            metadataRendererOutput: MetadataOutput,
            drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?
        ) =
            arrayOf(
                MediaCodecAudioRenderer(
                    context, MediaCodecSelector.DEFAULT, eventHandler,
                    audioRendererEventListener
                )
            )
    }

    private fun buildNotification(contentToPlay: ContentToPlay, bitmap: Bitmap?) {
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
            applicationContext,
            contentToPlay.content.title,
            app_name,
            notification_channel_description,
            EXOPLAYER_NOTIFICATION_ID,
            object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun createCurrentContentIntent(player: Player) = PendingIntent.getActivity(
                    applicationContext,
                    0,
                    Intent(applicationContext, MainActivity::class.java).apply {
                        action = OPEN_FROM_NOTIFICATION_ACTION
                        putExtra(OPEN_CONTENT_FROM_NOTIFICATION_KEY, contentToPlay)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                override fun getCurrentContentText(player: Player) =
                    contentToPlay.content.description

                override fun getCurrentContentTitle(player: Player) = contentToPlay.content.title

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ) = bitmap
            },
            object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationStarted(
                    notificationId: Int,
                    notification: Notification
                ) {
                    // Starts foreground service.
                    startForeground(notificationId, notification)
                    player?.playWhenReady = true
                }

                override fun onNotificationCancelled(notificationId: Int) {
                    stopService()
                }
            })
        playerNotificationManager?.setSmallIcon(ic_coinverse_notification_24dp)
        playerNotificationManager?.setPlayer(player)
    }

    private inner class PlayerEventListener : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playWhenReady == false) stopForeground(false)
            val newSeekPositionMillis = player?.currentPosition!!
            if (player?.currentPosition!! > 0L && newSeekPositionMillis > seekToPositionMillis
                && playOrPausePressed == false && playbackState == Player.STATE_BUFFERING
            )
                seekToPositionMillis = newSeekPositionMillis.toInt()
            val job = CoroutineScope(Dispatchers.IO).launch {
                try {
                    analytics.updateActionsAndAnalytics(
                        content = content,
                        watchPercent = analytics.getWatchPercent(
                            player?.currentPosition!!.toDouble(),
                            seekToPositionMillis.toDouble(), player?.duration!!.toDouble()
                        )
                    )
                } catch (error: Exception) {
                    this.cancel()
                    FirebaseCrashlytics.getInstance()
                        .log("$LOG_TAG Audio error: ${error.localizedMessage}")
                }
            }
            job.invokeOnCompletion { job.cancel() }
        }

        override fun onPositionDiscontinuity(@Player.DiscontinuityReason reason: Int) {
            // The user has performed a seek whilst in the error state. Update the resume position so
            // that if the user then retries, playback resumes from the position to which they seeked.
            if (player?.playbackError != null) updateStartPosition()
        }

        override fun onPlayerError(e: ExoPlaybackException) {
            if (isBehindLiveWindow(e)) clearStartPosition() else updateStartPosition()
        }
    }

    private fun updateStartPosition() {
        if (player != null) {
            startAutoPlay = player?.playWhenReady!!
            startWindow = player?.currentWindowIndex!!
            startPosition = Math.max(0, player?.contentPosition!!)
        }
    }

    private fun stopService() {
        player?.playWhenReady = false
        stopForeground(true)
        stopSelf(startId)
    }

    private fun isBehindLiveWindow(e: ExoPlaybackException) =
        if (e.type != ExoPlaybackException.TYPE_SOURCE) false
        else {
            while (e.sourceException != null) if (e.sourceException is BehindLiveWindowException) true
            FirebaseCrashlytics.getInstance()
                .log("$LOG_TAG Audio error: ${e.sourceException.cause.toString()}")
            false
        }

    private fun clearStartPosition() {
        startAutoPlay = true
        startWindow = C.INDEX_UNSET
        startPosition = C.TIME_UNSET
    }
}