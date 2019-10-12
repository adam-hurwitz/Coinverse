package app.coinverse.content

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import app.coinverse.R.id.dialog_content
import app.coinverse.content.models.ContentToPlay
import app.coinverse.databinding.FragmentContentDialogBinding
import app.coinverse.utils.*
import app.coinverse.utils.ContentType.*
import app.coinverse.utils.PlayerActionType.STOP


class ContentDialogFragment : DialogFragment() {
    private var LOG_TAG = ContentDialogFragment::class.java.simpleName

    private lateinit var contentToPlay: ContentToPlay
    private lateinit var binding: FragmentContentDialogBinding

    fun newInstance(bundle: Bundle) = ContentDialogFragment().apply { arguments = bundle }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentToPlay = arguments!!.getParcelable(CONTENT_TO_PLAY_KEY)!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentContentDialogBinding.inflate(inflater, container, false)
        if (savedInstanceState == null && childFragmentManager.findFragmentById(dialog_content) == null)
            childFragmentManager.beginTransaction().replace(dialog_content,
                    when (contentToPlay.content.contentType) {
                        ARTICLE -> AudioFragment().newInstance(arguments!!)
                        YOUTUBE -> {
                            // ContextCompat.startForegroundService(...) is not used because Service
                            // is being stopped here.
                            context?.startService(Intent(context, AudioService::class.java).apply {
                                action = PLAYER_ACTION
                                putExtra(PLAYER_KEY, STOP.name)
                            })
                            YouTubeFragment().newInstance(arguments!!)
                        }
                        NONE -> throw(IllegalArgumentException("contentType expected, contentType is 'NONE'"))
                    }
            ).commit()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        dialog!!.window!!.setLayout(getDialogDisplayWidth(context!!), getDialogDisplayHeight(context!!))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        dialog?.window?.setLayout(getDialogDisplayWidth(context!!), getDialogDisplayHeight(context!!))
    }
}