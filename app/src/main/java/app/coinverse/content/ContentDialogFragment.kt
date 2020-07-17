package app.coinverse.content

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import app.coinverse.R.id.dialog_content
import app.coinverse.databinding.FragmentContentDialogBinding
import app.coinverse.feed.AudioService
import app.coinverse.feed.state.FeedViewState.OpenContent
import app.coinverse.utils.CONTENT_TO_PLAY_KEY
import app.coinverse.utils.ContentType.ARTICLE
import app.coinverse.utils.ContentType.NONE
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.PLAYER_ACTION
import app.coinverse.utils.PLAYER_KEY
import app.coinverse.utils.PlayerActionType.STOP
import app.coinverse.utils.getDialogDisplayHeight
import app.coinverse.utils.getDialogDisplayWidth


/**
 * Todo: Refactor with Model-View-Intent.
 * See [app.coinverse.feed.FeedFragment].
 **/
class ContentDialogFragment : DialogFragment() {
    private var LOG_TAG = ContentDialogFragment::class.java.simpleName

    private lateinit var openContent: OpenContent
    private lateinit var binding: FragmentContentDialogBinding

    fun newInstance(bundle: Bundle) = ContentDialogFragment().apply { arguments = bundle }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openContent = requireArguments().getParcelable(CONTENT_TO_PLAY_KEY)!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentContentDialogBinding.inflate(inflater, container, false)
        /** Creates dialog Fragment to play media if not already created */
        if (savedInstanceState == null && childFragmentManager.findFragmentById(dialog_content) == null)
            childFragmentManager.beginTransaction().replace(dialog_content,
                    when (openContent.content.contentType) {
                        ARTICLE -> AudioFragment().newInstance(requireArguments())
                        YOUTUBE -> {
                            // ContextCompat.startForegroundService(...) is not used because Service
                            // is being stopped here.
                            context?.startService(Intent(context, AudioService::class.java).apply {
                                action = PLAYER_ACTION
                                putExtra(PLAYER_KEY, STOP.name)
                            })
                            YouTubeFragment().newInstance(requireArguments())
                        }
                        NONE -> throw(IllegalArgumentException("contentType expected, contentType is 'NONE'"))
                    }
            ).commit()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        dialog!!.window!!.setLayout(getDialogDisplayWidth(requireContext()), getDialogDisplayHeight(requireContext()))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        dialog?.window?.setLayout(getDialogDisplayWidth(requireContext()), getDialogDisplayHeight(requireContext()))
    }
}