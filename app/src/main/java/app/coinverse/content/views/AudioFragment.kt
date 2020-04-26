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

package app.coinverse.content.views

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import app.coinverse.App
import app.coinverse.analytics.Analytics
import app.coinverse.content.AudioViewEventType.AudioPlayerLoad
import app.coinverse.content.AudioViewEvents
import app.coinverse.content.ContentRepository
import app.coinverse.content.viewmodel.AudioViewModel
import app.coinverse.content.viewmodel.AudioViewModelFactory
import app.coinverse.databinding.FragmentAudioDialogBinding
import app.coinverse.feed.AudioService
import app.coinverse.feed.models.ContentToPlay
import app.coinverse.utils.AUDIOCAST_VIEW
import app.coinverse.utils.CONTENT_SELECTED_ACTION
import app.coinverse.utils.CONTENT_SELECTED_BITMAP_KEY
import app.coinverse.utils.CONTENT_TO_PLAY_KEY
import app.coinverse.utils.PLAYER_ACTION
import app.coinverse.utils.PLAYER_KEY
import app.coinverse.utils.PLAY_OR_PAUSE_PRESSED_KEY
import app.coinverse.utils.PlayerActionType
import app.coinverse.utils.PlayerActionType.PAUSE
import app.coinverse.utils.PlayerActionType.PLAY
import app.coinverse.utils.setImageUrlRounded
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.android.synthetic.main.exo_playback_control_view.*
import kotlinx.android.synthetic.main.exo_playback_control_view.view.*
import kotlinx.android.synthetic.main.fragment_audio_dialog.*
import javax.inject.Inject

private val LOG_TAG = AudioFragment::class.java.simpleName

/**
 * TODO: Refactor with Unidirectional Data Flow. See [FeedFragment].
 *  https://medium.com/hackernoon/android-unidirectional-flow-with-livedata-bf24119e747
 **/
class AudioFragment : Fragment() {
    @Inject
    lateinit var analytics: Analytics

    @Inject
    lateinit var repository: ContentRepository
    private val audioViewModel: AudioViewModel by viewModels {
        AudioViewModelFactory(this, repository = repository)
    }
    private var player: SimpleExoPlayer? = null
    private lateinit var viewEvents: AudioViewEvents
    private lateinit var contentToPlay: ContentToPlay

    fun newInstance(bundle: Bundle) = AudioFragment().apply { arguments = bundle }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            if (service is AudioService.AudioServiceBinder)
                playerView.player = service.getExoPlayerInstance().apply { player = this }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {}
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context.applicationContext as App).component.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentToPlay = requireArguments().getParcelable(CONTENT_TO_PLAY_KEY)!!
        audioViewModel.attachEvents(this)
        if (savedInstanceState == null)
            viewEvents.audioPlayerLoad(AudioPlayerLoad(
                    contentToPlay.content.id, contentToPlay.filePath!!,
                    contentToPlay.content.previewImage))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        analytics.setCurrentScreen(requireActivity(), AUDIOCAST_VIEW)
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

    fun initEvents(viewEvents: AudioViewEvents) {
        this.viewEvents = viewEvents
    }

    private fun setPlayerView() {
        playerView.requestFocus()
        playerView.preview.setImageUrlRounded(requireContext(), contentToPlay.content.previewImage)
        playerView.titleToolbar.text = contentToPlay.content.title
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
        audioViewModel.contentPlayer.observe(viewLifecycleOwner) { contentPlayer ->
            if (contentToPlay.content.id != audioViewModel.contentPlaying.id)
                if (SDK_INT >= VERSION_CODES.O)
                    context?.startService(Intent(context, AudioService::class.java).apply {
                        action = PLAYER_ACTION
                        if (!audioViewModel.contentPlaying.id.isNullOrEmpty())
                            putExtra(PLAYER_KEY, PlayerActionType.STOP.name)
                    })
                else context?.stopService(Intent(context, AudioService::class.java))
            audioViewModel.contentPlaying = contentToPlay.content
            context?.bindService(
                    Intent(context, AudioService::class.java).apply {
                        action = CONTENT_SELECTED_ACTION
                        putExtra(CONTENT_TO_PLAY_KEY, contentToPlay.apply {
                            content.audioUrl = contentPlayer.uri.toString()
                        })
                        putExtra(CONTENT_SELECTED_BITMAP_KEY, contentPlayer.image)
                    }, serviceConnection, Context.BIND_AUTO_CREATE)
            ContextCompat.startForegroundService(
                    requireContext(),
                    Intent(context, AudioService::class.java).apply {
                        action = CONTENT_SELECTED_ACTION
                        putExtra(CONTENT_TO_PLAY_KEY, contentToPlay.apply {
                            content.audioUrl = contentPlayer.uri.toString()
                        })
                        putExtra(CONTENT_SELECTED_BITMAP_KEY, contentPlayer.image)
                    })
        }
    }
}