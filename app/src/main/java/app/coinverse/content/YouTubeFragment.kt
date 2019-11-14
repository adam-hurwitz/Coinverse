package app.coinverse.content

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import app.coinverse.BuildConfig
import app.coinverse.R.id.dialog_content
import app.coinverse.analytics.Analytics.setCurrentScreen
import app.coinverse.analytics.Analytics.updateActionsAndAnalytics
import app.coinverse.analytics.Analytics.updateStartActionsAndAnalytics
import app.coinverse.content.models.ContentToPlay
import app.coinverse.databinding.FragmentContentDialogBinding
import app.coinverse.utils.*
import app.coinverse.utils.BuildType.*
import app.coinverse.utils.auth.APP_API_KEY_OPEN_SHARED
import app.coinverse.utils.auth.APP_API_KEY_PRODUCTION
import app.coinverse.utils.auth.APP_API_KEY_STAGING
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerSupportFragment
import kotlinx.coroutines.launch

/**
 * TODO - Refactor with Unidirectional Data Flow.
 *  See [ContentFragment]
 *  https://medium.com/hackernoon/android-unidirectional-flow-with-livedata-bf24119e747
 **/
class YouTubeFragment : Fragment() {

    private var LOG_TAG = YouTubeFragment::class.java.simpleName

    private lateinit var contentToPlay: ContentToPlay
    private lateinit var binding: FragmentContentDialogBinding
    private lateinit var youtubePlayer: YouTubePlayer

    private var seekToPositionMillis = 0

    fun newInstance(bundle: Bundle) = YouTubeFragment().apply { arguments = bundle }

    //TODO: Remove savedInstanceState
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(MEDIA_IS_PLAYING_KEY, youtubePlayer.isPlaying)
        outState.putInt(MEDIA_CURRENT_TIME_KEY, youtubePlayer.currentTimeMillis)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentToPlay = arguments!!.getParcelable(CONTENT_TO_PLAY_KEY)!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        setCurrentScreen(activity!!, YOUTUBE_VIEW)
        binding = FragmentContentDialogBinding.inflate(inflater, container, false)
        val youTubePlayerFragment = YouTubePlayerSupportFragment.newInstance()
        youTubePlayerFragment.initialize(
                when (BuildConfig.BUILD_TYPE) {
                    debug.name -> APP_API_KEY_STAGING
                    release.name -> APP_API_KEY_PRODUCTION
                    open.name -> APP_API_KEY_OPEN_SHARED
                    else -> APP_API_KEY_STAGING
                },
                object : YouTubePlayer.OnInitializedListener {
                    override fun onInitializationSuccess(provider: YouTubePlayer.Provider,
                                                         player: YouTubePlayer,
                                                         wasRestored: Boolean) {
                        if (!wasRestored) {
                            youtubePlayer = player
                            player.setPlayerStateChangeListener(
                                    PlayerStateChangeListener(savedInstanceState))
                            player.setPlaybackEventListener(PlaybackEventListener())
                            Regex(YOUTUBE_ID_REGEX).replace(contentToPlay.content.id, "")
                                    .also { youTubeId ->
                                        if (savedInstanceState == null) player.loadVideo(youTubeId)
                                        else player.loadVideo(youTubeId, savedInstanceState
                                                .getInt(MEDIA_CURRENT_TIME_KEY))
                                    }
                        }
                    }

                    override fun onInitializationFailure(provider: YouTubePlayer.Provider,
                                                         result: YouTubeInitializationResult) {
                        Log.e(LOG_TAG, "onInitializationFailure ${result.name}")
                    }
                })
        childFragmentManager.beginTransaction()
                .replace(dialog_content, youTubePlayerFragment as Fragment).commit()
        return binding.root
    }

    private inner class PlayerStateChangeListener(var savedInstanceState: Bundle?)
        : YouTubePlayer.PlayerStateChangeListener {
        override fun onLoading() {}
        override fun onLoaded(videoId: String) {
            if (savedInstanceState != null && !savedInstanceState!!.getBoolean(MEDIA_IS_PLAYING_KEY))
                youtubePlayer.pause()
        }

        override fun onAdStarted() {}
        override fun onVideoStarted() {
            updateStartActionsAndAnalytics(savedInstanceState, contentToPlay.content)
        }

        override fun onVideoEnded() {}
        override fun onError(reason: YouTubePlayer.ErrorReason) {}
    }

    private inner class PlaybackEventListener : YouTubePlayer.PlaybackEventListener {
        override fun onPlaying() {}
        override fun onBuffering(isBuffering: Boolean) {}
        override fun onStopped() {}
        override fun onPaused() {}
        override fun onSeekTo(newSeekPositionMillis: Int) {
            if (newSeekPositionMillis > seekToPositionMillis)
                seekToPositionMillis = newSeekPositionMillis
        }
    }

    override fun onPause() {
        super.onPause()
        if (youtubePlayer != null)
            lifecycleScope.launch {
                updateActionsAndAnalytics(contentToPlay.content,
                        (youtubePlayer.currentTimeMillis.toDouble() - seekToPositionMillis)
                                / youtubePlayer.durationMillis)
            }
    }
}