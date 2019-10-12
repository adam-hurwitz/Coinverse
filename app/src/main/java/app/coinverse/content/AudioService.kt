package app.coinverse.content

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import app.coinverse.MainActivity
import app.coinverse.R.drawable.ic_coinverse_notification_24dp
import app.coinverse.R.string.*
import app.coinverse.analytics.Analytics.getWatchPercent
import app.coinverse.analytics.Analytics.updateActionsAndAnalytics
import app.coinverse.analytics.Analytics.updateStartActionsAndAnalytics
import app.coinverse.content.models.Content
import app.coinverse.content.models.ContentToPlay
import app.coinverse.utils.*
import app.coinverse.utils.PlayerActionType.*
import com.crashlytics.android.Crashlytics
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoRendererEventListener

private val LOG_TAG = AudioService::class.java.simpleName

class AudioService : Service() {
    private var player: SimpleExoPlayer? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var content = Content()
    private var startAutoPlay: Boolean = false
    private var startWindow: Int = 0
    private var startPosition: Long = 0
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var seekToPositionMillis = 0
    private var playOrPausePressed = false

    // Called first time audiocast is loaded.
    override fun onBind(intent: Intent?) =
            AudioServiceBinder().apply {
                player = ExoPlayerFactory.newSimpleInstance(
                        applicationContext,
                        AudioOnlyRenderersFactory(applicationContext),
                        DefaultTrackSelector())
                player?.addListener(PlayerEventListener())
                buildNotification(
                        intent!!.getParcelableExtra(CONTENT_TO_PLAY_KEY),
                        intent.getByteArrayExtra(CONTENT_SELECTED_BITMAP_KEY).byteArrayToBitmap(applicationContext)
                )
            }

    override fun onDestroy() {
        playerNotificationManager?.setPlayer(null)
        player?.release()
        wakeAndwifiLockManager(false)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) =
            super.onStartCommand(intent?.apply {
                when (intent.action) {
                    CONTENT_SELECTED_ACTION ->
                        this.getParcelableExtra<ContentToPlay>(CONTENT_TO_PLAY_KEY).also { contentToPlay ->
                            if (!contentToPlay.content.equals(content)) { // New content playing
                                content = contentToPlay.content
                                seekToPositionMillis = 0
                                updateStartActionsAndAnalytics(content)
                                player?.prepare(ProgressiveMediaSource.Factory(
                                        DefaultDataSourceFactory(
                                                applicationContext,
                                                Util.getUserAgent(applicationContext, getString(app_name))))
                                        .createMediaSource(Uri.parse(content.audioUrl)))
                                playerNotificationManager?.setPlayer(null)
                                buildNotification(
                                        intent.getParcelableExtra(CONTENT_TO_PLAY_KEY),
                                        intent.getByteArrayExtra(CONTENT_SELECTED_BITMAP_KEY)
                                                .byteArrayToBitmap(applicationContext))
                            }
                        }
                    PLAYER_ACTION -> when (this.getStringExtra(PLAYER_KEY)) {
                        PAUSE.name -> {
                            player?.playWhenReady = false
                            wakeAndwifiLockManager(false)
                            playOrPausePressed =
                                    this.getBooleanExtra(PLAY_OR_PAUSE_PRESSED_KEY, false)
                        }
                        PLAY.name -> {
                            player?.playWhenReady = true
                            wakeAndwifiLockManager(true)
                            playOrPausePressed =
                                    this.getBooleanExtra(PLAY_OR_PAUSE_PRESSED_KEY, false)
                        }
                        STOP.name -> stopService()
                        else -> Crashlytics.log(Log.ERROR, LOG_TAG, "ExoPlayer controls error")
                    }
                    else -> Crashlytics.log(Log.ERROR, LOG_TAG, "ExoPlayer onStartCommand error")
                }
            }, flags, startId)

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

    private inner class AudioOnlyRenderersFactory(var context: Context) : RenderersFactory {
        override fun createRenderers(eventHandler: Handler?,
                                     videoRendererEventListener: VideoRendererEventListener?,
                                     audioRendererEventListener: AudioRendererEventListener?,
                                     textRendererOutput: TextOutput?,
                                     metadataRendererOutput: MetadataOutput?,
                                     drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?) =
                arrayOf(MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, eventHandler,
                        audioRendererEventListener))
    }

    private fun buildNotification(contentToPlay: ContentToPlay, bitmap: Bitmap?) {
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                applicationContext,
                contentToPlay.content.title,
                app_name,
                if (!contentToPlay.content.audioUrl.isNullOrEmpty()) 1 else -1,
                object : PlayerNotificationManager.MediaDescriptionAdapter {
                    override fun createCurrentContentIntent(player: Player?) =
                            PendingIntent.getActivity(
                                    applicationContext,
                                    0,
                                    Intent(applicationContext, MainActivity::class.java).apply {
                                        action = OPEN_FROM_NOTIFICATION_ACTION
                                        putExtra(OPEN_CONTENT_FROM_NOTIFICATION_KEY, contentToPlay)
                                    },
                                    PendingIntent.FLAG_UPDATE_CURRENT)

                    override fun getCurrentContentText(player: Player?) =
                            contentToPlay.content.description

                    override fun getCurrentContentTitle(player: Player?) =
                            contentToPlay.content.title

                    override fun getCurrentLargeIcon(player: Player?,
                                                     callback: PlayerNotificationManager.BitmapCallback?) =
                            bitmap
                },
                object : PlayerNotificationManager.NotificationListener {
                    override fun onNotificationStarted(notificationId: Int, notification: Notification) {
                        startForeground(notificationId, notification)
                        player?.playWhenReady = true
                        wakeAndwifiLockManager(true)
                    }

                    override fun onNotificationCancelled(notificationId: Int) {
                        stopService()
                    }
                }).apply {
            setSmallIcon(ic_coinverse_notification_24dp)
        }
        playerNotificationManager?.setPlayer(player)
    }

    private inner class PlayerEventListener : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playWhenReady == false) stopForeground(false)
            val newSeekPositionMillis = player?.currentPosition!!
            if (player?.currentPosition!! > 0L && newSeekPositionMillis > seekToPositionMillis
                    && playOrPausePressed == false && playbackState == Player.STATE_BUFFERING)
                seekToPositionMillis = newSeekPositionMillis.toInt()
            updateActionsAndAnalytics(content, getWatchPercent(player?.currentPosition!!.toDouble(),
                            seekToPositionMillis.toDouble(), player?.duration!!.toDouble()))
        }

        override fun onPositionDiscontinuity(@Player.DiscontinuityReason reason: Int) {
            // The user has performed a seek whilst in the error state. Update the resume position so
            // that if the user then retries, playback resumes from the position to which they seeked.
            if (player?.playbackError != null) updateStartPosition()
        }

        override fun onPlayerError(e: ExoPlaybackException?) {
            if (isBehindLiveWindow(e!!)) {
                clearStartPosition()
            } else updateStartPosition()
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
            // Not implemented.
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
        wakeAndwifiLockManager(false)
        stopForeground(true)
        stopSelf()
    }

    private fun isBehindLiveWindow(e: ExoPlaybackException) =
            if (e.type != ExoPlaybackException.TYPE_SOURCE) false
            else {
                while (e.sourceException != null) if (e.sourceException is BehindLiveWindowException) true
                Crashlytics.log(Log.ERROR, LOG_TAG, "Audio error: ${e.sourceException?.cause.toString()}")
                false
            }

    private fun clearStartPosition() {
        startAutoPlay = true
        startWindow = C.INDEX_UNSET
        startPosition = C.TIME_UNSET
    }

    private fun wakeAndwifiLockManager(enabled: Boolean) {
        if (enabled) {
            wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, getString(wake_lock_media)).apply {
                this.acquire(10000)
            }
            wifiLock = (getSystemService(Context.WIFI_SERVICE) as WifiManager).createWifiLock(
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF, getString(wifi_lock_media)).apply {
                this.acquire()
            }
        } else if (!enabled && wakeLock != null && wakeLock!!.isHeld) wakeLock?.release()
        else if (!enabled && wifiLock != null && wifiLock!!.isHeld) wifiLock?.release()
    }
}