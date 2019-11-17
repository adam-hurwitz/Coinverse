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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.IBinder
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import app.coinverse.R.string.*
import app.coinverse.analytics.Analytics.setCurrentScreen
import app.coinverse.content.models.ContentToPlay
import app.coinverse.content.models.ContentViewEventType.AudioPlayerLoad
import app.coinverse.content.models.ContentViewEvents
import app.coinverse.databinding.FragmentAudioDialogBinding
import app.coinverse.utils.*
import app.coinverse.utils.PlayerActionType.*
import app.coinverse.utils.livedata.EventObserver
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.util.ErrorMessageProvider
import kotlinx.android.synthetic.main.exo_playback_control_view.*
import kotlinx.android.synthetic.main.exo_playback_control_view.view.*
import kotlinx.android.synthetic.main.fragment_audio_dialog.*

private val LOG_TAG = AudioFragment::class.java.simpleName

/**
 * TODO - Refactor with Unidirectional Data Flow.
 *  See [ContentFragment]
 *  https://medium.com/hackernoon/android-unidirectional-flow-with-livedata-bf24119e747
 **/
class AudioFragment : Fragment() {
    private lateinit var viewEvents: ContentViewEvents
    private var player: SimpleExoPlayer? = null
    private lateinit var contentToPlay: ContentToPlay
    private lateinit var contentViewModel: ContentViewModel

    fun newInstance(bundle: Bundle) = AudioFragment().apply { arguments = bundle }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            if (service is AudioService.AudioServiceBinder)
                playerView.player = service.getExoPlayerInstance().apply {
                    player = this
                }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentToPlay = arguments!!.getParcelable(CONTENT_TO_PLAY_KEY)!!
        contentViewModel = ViewModelProviders.of(this).get(ContentViewModel::class.java)
        contentViewModel.attachEvents(this)
        if (savedInstanceState == null)
            viewEvents.audioPlayerLoad(AudioPlayerLoad(
                    contentToPlay.content.id, contentToPlay.filePath!!,
                    contentToPlay.content.previewImage))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setCurrentScreen(activity!!, AUDIOCAST_VIEW)
        return FragmentAudioDialogBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPlayerView()
        setClickAndTouchListeners()
        observeViewState()
    }

    override fun onStart() {
        super.onStart()
        if (playerView != null) playerView.onResume()
    }

    override fun onResume() {
        super.onResume()
        if (player == null && playerView != null) playerView.onResume()
    }

    fun initEvents(viewEvents: ContentViewEvents) {
        this.viewEvents = viewEvents
    }

    private fun setPlayerView() {
        playerView.setErrorMessageProvider(PlayerErrorMessageProvider())
        playerView.requestFocus()
        playerView.preview.setImageUrl(context!!, contentToPlay.content.previewImage)
        playerView.title.text = contentToPlay.content.title
        playerView.showController()
    }

    private fun setClickAndTouchListeners() {
        exo_play.setOnClickListener {
            context?.startService(Intent(context, AudioService::class.java).apply {
                action = PLAYER_ACTION
                putExtra(PLAYER_KEY, PLAY.name)
                putExtra(PLAY_OR_PAUSE_PRESSED_KEY, true)
            })
        }
        exo_pause.setOnClickListener {
            context?.startService(Intent(context, AudioService::class.java).apply {
                action = PLAYER_ACTION
                putExtra(PLAYER_KEY, PAUSE.name)
                putExtra(PLAY_OR_PAUSE_PRESSED_KEY, true)
            })
        }
    }

    private fun observeViewState() {
        contentViewModel.playerViewState.observe(viewLifecycleOwner, Observer { viewState ->
            viewState?.contentPlayer?.observe(viewLifecycleOwner, EventObserver { contentPlayer ->
                if (contentToPlay.content.id != contentViewModel.contentPlaying.id)
                    if (VERSION.SDK_INT >= VERSION_CODES.O)
                        context?.startService(Intent(context, AudioService::class.java).apply {
                            action = PLAYER_ACTION
                            if (!contentViewModel.contentPlaying.id.isNullOrEmpty())
                                putExtra(PLAYER_KEY, STOP.name)
                        })
                    else context?.stopService(Intent(context, AudioService::class.java))
                contentViewModel.contentPlaying = contentToPlay.content
                context?.bindService(
                        Intent(context, AudioService::class.java).apply {
                            action = CONTENT_SELECTED_ACTION
                            putExtra(CONTENT_TO_PLAY_KEY, contentToPlay.apply {
                                content.audioUrl = contentPlayer.uri.toString()
                            })
                            putExtra(CONTENT_SELECTED_BITMAP_KEY, contentPlayer.image)
                        }, serviceConnection, Context.BIND_AUTO_CREATE)
                ContextCompat.startForegroundService(
                        context!!,
                        Intent(context, AudioService::class.java).apply {
                            action = CONTENT_SELECTED_ACTION
                            putExtra(CONTENT_TO_PLAY_KEY, contentToPlay.apply {
                                content.audioUrl = contentPlayer.uri.toString()
                            })
                            putExtra(CONTENT_SELECTED_BITMAP_KEY, contentPlayer.image)
                        })
            })
        })
    }

    private inner class PlayerErrorMessageProvider : ErrorMessageProvider<ExoPlaybackException> {
        override fun getErrorMessage(e: ExoPlaybackException): Pair<Int, String> {
            var errorString = getString(error_generic)
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                val cause = e.rendererException
                if (cause is MediaCodecRenderer.DecoderInitializationException)
                // Special case for decoder initialization failures.
                    if (cause.decoderName == null)
                        if (cause.cause is MediaCodecUtil.DecoderQueryException)
                            errorString = getString(error_querying_decoders)
                        else if (cause.secureDecoderRequired)
                            errorString = getString(error_no_secure_decoder, cause.mimeType)
                        else errorString = getString(error_no_decoder, cause.mimeType)
                    else errorString = getString(error_instantiating_decoder, cause.decoderName)
            }
            return Pair.create(0, errorString)
        }
    }
}