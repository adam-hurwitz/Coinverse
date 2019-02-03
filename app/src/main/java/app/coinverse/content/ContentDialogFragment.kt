package app.coinverse.content

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import app.coinverse.Enums.ContentType.*
import app.coinverse.R
import app.coinverse.content.models.ContentSelected
import app.coinverse.content.room.CoinverseDatabase
import app.coinverse.databinding.FragmentContentDialogBinding
import app.coinverse.utils.*

class ContentDialogFragment : DialogFragment() {
    private var LOG_TAG = ContentDialogFragment::class.java.simpleName

    private lateinit var contentSelected: ContentSelected
    private lateinit var binding: FragmentContentDialogBinding
    private lateinit var coinverseDatabase: CoinverseDatabase

    fun newInstance(bundle: Bundle) = ContentDialogFragment().apply { arguments = bundle }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentSelected = arguments!!.getParcelable(CONTENT_SELECTED_KEY)!!
        coinverseDatabase = CoinverseDatabase.getAppDatabase(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentContentDialogBinding.inflate(inflater, container, false)
        if (savedInstanceState == null && childFragmentManager.findFragmentById(R.id.dialog_content) == null)
            childFragmentManager.beginTransaction().replace(R.id.dialog_content,
                    when (contentSelected.content.contentType) {
                        ARTICLE -> AudioFragment().newInstance(arguments!!)
                        YOUTUBE -> YouTubeFragment().newInstance(arguments!!)
                        NONE -> throw(IllegalArgumentException("contentType expected, contentType is 'NONE'"))
                    }
            ).commit()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (resources.configuration.orientation == ORIENTATION_PORTRAIT)
            dialog.window!!.setLayout(getDisplayWidth(context!!),
                    (getDisplayHeight(context!!) / CONTENT_DIALOG_PORTRAIT_HEIGHT_DIVISOR))
        else
            dialog.window!!.setLayout((getDisplayWidth(context!!) / CONTENT_DIALOG_LANDSCAPE_WIDTH_DIVISOR).toInt(),
                    (getDisplayHeight(context!!) / CONTENT_DIALOG_LANDSCAPE_HEIGHT_DIVISOR).toInt())
    }
}