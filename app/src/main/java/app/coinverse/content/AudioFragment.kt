/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.coinverse.content

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import app.coinverse.R
import app.coinverse.content.models.Content
import app.coinverse.content.room.CoinverseDatabase
import app.coinverse.databinding.FragmentAudioDialogBinding
import app.coinverse.utils.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.google.android.exoplayer2.Player.STATE_READY
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.extractor.Extractor
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoRendererEventListener
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.getInstance
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.exo_playback_control_view.*
import kotlinx.android.synthetic.main.exo_playback_control_view.view.*
import kotlinx.android.synthetic.main.fragment_audio_dialog.*
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

class AudioFragment : Fragment() {
    private var LOG_TAG = AudioFragment::class.java.simpleName

    private lateinit var analytics: FirebaseAnalytics
    private lateinit var content: Content
    private lateinit var binding: FragmentAudioDialogBinding
    private lateinit var contentViewModel: ContentViewModel
    private lateinit var coinverseDatabase: CoinverseDatabase

    private var savedInstanceState: Bundle? = Bundle()
    private var storage = FirebaseStorage.getInstance()
    private var player: SimpleExoPlayer? = null
    private var mediaSource: MediaSource? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var trackSelectorParameters: DefaultTrackSelector.Parameters? = null
    private var startAutoPlay: Boolean = false
    private var startWindow: Int = 0
    private var startPosition: Long = 0
    private var oldSeekToPositionMillis = 0
    private var seekToPositionMillis = 0L
    private var playOrPausePressed = false
    private var updateSeekTo = false

    private val DEFAULT_COOKIE_MANAGER: CookieManager

    init {
        DEFAULT_COOKIE_MANAGER = CookieManager()
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
    }

    fun newInstance(bundle: Bundle) = AudioFragment().apply { arguments = bundle }

    override fun onSaveInstanceState(outState: Bundle) {
        updateTrackSelectorParameters()
        updateStartPosition()
        outState.putParcelable(KEY_TRACK_SELECTOR_PARAMETERS, trackSelectorParameters)
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay)
        outState.putInt(KEY_WINDOW, startWindow)
        outState.putLong(KEY_POSITION, startPosition)
        outState.putLong(KEY_SEEK_TO_POSITION_MILLIS, seekToPositionMillis)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.savedInstanceState = savedInstanceState
        analytics = getInstance(FirebaseApp.getInstance()!!.applicationContext)
        content = arguments!!.getParcelable(CONTENT_KEY)!!
        contentViewModel = ViewModelProviders.of(this).get(ContentViewModel::class.java)
        coinverseDatabase = CoinverseDatabase.getAppDatabase(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        analytics.setCurrentScreen(activity!!, AUDIOCAST_VIEW, null)
        binding = FragmentAudioDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPlayerView()
        setClickAndTouchListeners()
        setSavedInstanceState(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
        if (playerView != null) playerView.onResume()
    }

    override fun onResume() {
        super.onResume()
        if (player == null) {
            initializePlayer()
            if (playerView != null) playerView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (playerView != null && player != null) {
            playerView.onPause()
            updateActionsAndAnalytics(content, contentViewModel, coinverseDatabase.contentDao(),
                    analytics, getWatchPercent(player?.currentPosition!!.toDouble(),
                    seekToPositionMillis.toDouble(), player?.duration!!.toDouble()))
        }
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        activity!!.window.clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (playerView != null) playerView.onPause()
        releasePlayer()
    }

    private fun setPlayerView() {
        if (CookieHandler.getDefault() !== DEFAULT_COOKIE_MANAGER)
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER)
        playerView.setErrorMessageProvider(PlayerErrorMessageProvider())
        playerView.requestFocus()
        playerView.preview.setImageUrl(context!!, content.previewImage)
        playerView.title.text = content.title
        activity!!.window.addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setClickAndTouchListeners() {
        exo_play.setOnClickListener {
            playOrPausePressed = true
            player?.playWhenReady = true
        }
        exo_pause.setOnClickListener {
            playOrPausePressed = true
            player?.playWhenReady = false
        }
        exo_progress.setOnTouchListener { v, event ->
            oldSeekToPositionMillis = player?.currentPosition!!.toInt()
            playOrPausePressed = false
            false
        }
    }

    private fun setSavedInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            trackSelectorParameters = savedInstanceState.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS)
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY)
            startWindow = savedInstanceState.getInt(KEY_WINDOW)
            startPosition = savedInstanceState.getLong(KEY_POSITION)
            seekToPositionMillis = savedInstanceState.getLong(KEY_SEEK_TO_POSITION_MILLIS)
        } else {
            trackSelectorParameters = DefaultTrackSelector.ParametersBuilder().build()
            clearStartPosition()
        }
    }

    private fun initializePlayer() {
        storage.reference.child(content.audioUrl).downloadUrl.addOnSuccessListener { url ->
            val haveStartPosition = startWindow != C.INDEX_UNSET
            if (haveStartPosition) player?.seekTo(startWindow, startPosition)
            player?.prepare(mediaSource, !haveStartPosition, false)
            if (player == null && context != null) {
                trackSelector = DefaultTrackSelector(AdaptiveTrackSelection.Factory())
                trackSelector?.parameters = trackSelectorParameters
                player = ExoPlayerFactory.newSimpleInstance(
                        context,
                        AudioOnlyRenderersFactory(context!!),
                        trackSelector)
                player?.addListener(PlayerEventListener())
                player?.addAnalyticsListener(EventLogger(trackSelector))
                player?.playWhenReady = startAutoPlay
                playerView.player = player
                mediaSource = ExtractorMediaSource.Factory(
                        DefaultDataSourceFactory(
                                context,
                                Util.getUserAgent(context, getString(R.string.app_name))))
                        .setExtractorsFactory(Mp3ExtractorsFactory())
                        .createMediaSource(url)
            }
        }.addOnFailureListener { Log.e(LOG_TAG, "initializePlayer error: ${it.message}") }
    }

    private fun updateTrackSelectorParameters() {
        if (trackSelector != null) trackSelectorParameters = trackSelector?.parameters
    }

    private fun updateStartPosition() {
        if (player != null) {
            startAutoPlay = player?.playWhenReady!!
            startWindow = player?.currentWindowIndex!!
            startPosition = Math.max(0, player?.contentPosition!!)
        }
    }

    private fun isBehindLiveWindow(e: ExoPlaybackException): Boolean {
        if (e.type != ExoPlaybackException.TYPE_SOURCE) return false
        val cause: Throwable? = e.sourceException
        while (cause != null) if (cause is BehindLiveWindowException) return true
        Log.e(LOG_TAG, "Audio error: ${cause?.cause.toString()}")
        return false
    }

    private fun clearStartPosition() {
        startAutoPlay = true
        startWindow = C.INDEX_UNSET
        startPosition = C.TIME_UNSET
    }

    private fun releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters()
            updateStartPosition()
            player?.release()
            player = null
            mediaSource = null
            trackSelector = null
        }
    }

    private inner class PlayerEventListener : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            // Audiocast 'start' event.
            if (playbackState == STATE_READY && savedInstanceState == null
                    && player?.currentPosition == 0L && oldSeekToPositionMillis == 0)
                updateStartActionsAndAnalytics(savedInstanceState, content, contentViewModel, analytics)
            // Audiocast seekTo.
            val newSeekPositionMillis = player?.currentPosition!!
            if (player?.currentPosition!! > 0L && newSeekPositionMillis > seekToPositionMillis
                    && playOrPausePressed == false) {
                // Check if user adjusted seekTo.
                var updateSeekToInstanceState = true
                if (savedInstanceState != null) updateSeekToInstanceState = false
                if (playbackState == STATE_BUFFERING && (updateSeekToInstanceState || updateSeekTo))
                    seekToPositionMillis = newSeekPositionMillis
                if (savedInstanceState != null) updateSeekTo = true
            }
        }

        override fun onPositionDiscontinuity(@Player.DiscontinuityReason reason: Int) {
            // The user has performed a seek whilst in the error state. Update the resume position so
            // that if the user then retries, playback resumes from the position to which they seeked.
            if (player?.playbackError != null) updateStartPosition()
        }

        override fun onPlayerError(e: ExoPlaybackException?) {
            if (isBehindLiveWindow(e!!)) {
                clearStartPosition()
                initializePlayer()
            } else updateStartPosition()
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
            // Not implemented.
        }
    }

    private inner class PlayerErrorMessageProvider : ErrorMessageProvider<ExoPlaybackException> {
        override fun getErrorMessage(e: ExoPlaybackException): Pair<Int, String> {
            var errorString = getString(R.string.error_generic)
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                val cause = e.rendererException
                if (cause is MediaCodecRenderer.DecoderInitializationException)
                // Special case for decoder initialization failures.
                    if (cause.decoderName == null)
                        if (cause.cause is MediaCodecUtil.DecoderQueryException)
                            errorString = getString(R.string.error_querying_decoders)
                        else if (cause.secureDecoderRequired)
                            errorString = getString(R.string.error_no_secure_decoder, cause.mimeType)
                        else errorString = getString(R.string.error_no_decoder, cause.mimeType)
                    else errorString = getString(R.string.error_instantiating_decoder, cause.decoderName)
            }
            return Pair.create(0, errorString)
        }
    }

    private inner class AudioOnlyRenderersFactory(var context: Context) : RenderersFactory {
        override fun createRenderers(eventHandler: Handler?,
                                     videoRendererEventListener: VideoRendererEventListener?,
                                     audioRendererEventListener: AudioRendererEventListener?,
                                     textRendererOutput: TextOutput?,
                                     metadataRendererOutput: MetadataOutput?,
                                     drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?): Array<Renderer> {
            return arrayOf(MediaCodecAudioRenderer(context,
                    MediaCodecSelector.DEFAULT, eventHandler, audioRendererEventListener))
        }

    }

    private inner class Mp3ExtractorsFactory : ExtractorsFactory {
        override fun createExtractors(): Array<Extractor> {
            return arrayOf(Mp3Extractor())
        }
    }
}