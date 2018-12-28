package app.coinverse.content

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import app.coinverse.BuildConfig
import app.coinverse.R
import app.coinverse.content.models.Content
import app.coinverse.content.room.CoinverseDatabase
import app.coinverse.databinding.FragmentContentDialogBinding
import app.coinverse.utils.*
import app.coinverse.utils.auth.APP_API_ID_PRODUCTION
import app.coinverse.utils.auth.APP_API_ID_STAGING
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerSupportFragment
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

class YouTubeFragment : Fragment() {

    private var LOG_TAG = YouTubeFragment::class.java.simpleName

    private lateinit var analytics: FirebaseAnalytics
    private lateinit var content: Content
    private lateinit var binding: FragmentContentDialogBinding
    private lateinit var contentViewModel: ContentViewModel
    private lateinit var coinverseDatabase: CoinverseDatabase
    private lateinit var youtubePlayer: YouTubePlayer

    private var seekToPositionMillis = 0

    fun newInstance(bundle: Bundle) = YouTubeFragment().apply { arguments = bundle }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(MEDIA_IS_PLAYING_KEY, youtubePlayer.isPlaying)
        outState.putInt(MEDIA_CURRENT_TIME_KEY, youtubePlayer.currentTimeMillis)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics = FirebaseAnalytics.getInstance(FirebaseApp.getInstance()!!.applicationContext)
        content = arguments!!.getParcelable(CONTENT_KEY)!!
        contentViewModel = ViewModelProviders.of(this).get(ContentViewModel::class.java)
        coinverseDatabase = CoinverseDatabase.getAppDatabase(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        analytics.setCurrentScreen(activity!!, YOUTUBE_VIEW, null)
        binding = FragmentContentDialogBinding.inflate(inflater, container, false)
        val youTubePlayerFragment = YouTubePlayerSupportFragment.newInstance()
        val appApiId = if (BuildConfig.DEBUG) APP_API_ID_STAGING else APP_API_ID_PRODUCTION
        youTubePlayerFragment.initialize(appApiId, object : YouTubePlayer.OnInitializedListener {
            override fun onInitializationSuccess(provider: YouTubePlayer.Provider, player: YouTubePlayer, wasRestored: Boolean) {
                if (!wasRestored) {
                    youtubePlayer = player
                    player.setPlayerStateChangeListener(PlayerStateChangeListener(savedInstanceState))
                    player.setPlaybackEventListener(PlaybackEventListener())
                    if (savedInstanceState == null) player.loadVideo(content.id.substring(8))
                    else player.loadVideo(content.id.substring(8),
                            savedInstanceState.getInt(MEDIA_CURRENT_TIME_KEY))
                }
            }

            override fun onInitializationFailure(provider: YouTubePlayer.Provider, result: YouTubeInitializationResult) {
                Log.v(LOG_TAG, String.format("YouTube intialization failed: %s", result.name))
            }
        })
        childFragmentManager.beginTransaction().replace(R.id.dialog_content, youTubePlayerFragment as Fragment).commit()
        return binding.root
    }

    private inner class PlayerStateChangeListener(var savedInstanceState: Bundle?) : YouTubePlayer.PlayerStateChangeListener {
        override fun onLoading() {}
        override fun onLoaded(videoId: String) {
            if (savedInstanceState != null && !savedInstanceState!!.getBoolean(MEDIA_IS_PLAYING_KEY))
                youtubePlayer.pause()
        }

        override fun onAdStarted() {}
        override fun onVideoStarted() {
            updateStartActionsAndAnalytics(savedInstanceState, content, contentViewModel, analytics)
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
            if (newSeekPositionMillis > seekToPositionMillis) seekToPositionMillis = newSeekPositionMillis
        }
    }

    override fun onPause() {
        super.onPause()
        val watchPercent = (youtubePlayer.currentTimeMillis.toDouble() - seekToPositionMillis) / youtubePlayer.durationMillis
        updateActionsAndAnalytics(content, contentViewModel, coinverseDatabase.contentDao(),
                analytics, watchPercent)
    }
}