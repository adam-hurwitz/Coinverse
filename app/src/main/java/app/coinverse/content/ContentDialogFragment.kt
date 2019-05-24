package app.coinverse.content

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import app.coinverse.R
import app.coinverse.content.models.ContentResult
import app.coinverse.content.room.CoinverseDatabase
import app.coinverse.databinding.FragmentContentDialogBinding
import app.coinverse.utils.CONTENT_SELECTED_KEY
import app.coinverse.utils.Enums.ContentType.*
import app.coinverse.utils.getDialogDisplayHeight
import app.coinverse.utils.getDialogDisplayWidth

class ContentDialogFragment : DialogFragment() {
    private var LOG_TAG = ContentDialogFragment::class.java.simpleName

    private lateinit var contentToPlay: ContentResult.ContentToPlay
    private lateinit var binding: FragmentContentDialogBinding
    private lateinit var coinverseDatabase: CoinverseDatabase

    fun newInstance(bundle: Bundle) = ContentDialogFragment().apply { arguments = bundle }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentToPlay = arguments!!.getParcelable(CONTENT_SELECTED_KEY)!!
        coinverseDatabase = CoinverseDatabase.getAppDatabase(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentContentDialogBinding.inflate(inflater, container, false)
        if (savedInstanceState == null && childFragmentManager.findFragmentById(R.id.dialog_content) == null)
            childFragmentManager.beginTransaction().replace(R.id.dialog_content,
                    when (contentToPlay.content.contentType) {
                        ARTICLE -> AudioFragment().newInstance(arguments!!)
                        YOUTUBE -> YouTubeFragment().newInstance(arguments!!)
                        NONE -> throw(IllegalArgumentException("contentType expected, contentType is 'NONE'"))
                    }
            ).commit()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        dialog.window!!.setLayout(getDialogDisplayWidth(context!!), getDialogDisplayHeight(context!!))
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        dialog.window!!.setLayout(getDialogDisplayWidth(context!!), getDialogDisplayHeight(context!!))
    }
}