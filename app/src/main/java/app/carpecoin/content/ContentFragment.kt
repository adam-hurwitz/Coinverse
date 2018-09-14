package app.carpecoin.content

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import app.carpecoin.Enums.FeedType.*
import app.carpecoin.HomeViewModel
import app.carpecoin.coin.R
import app.carpecoin.coin.databinding.FragmentContentBinding
import app.carpecoin.content.adapter.ContentAdapter
import app.carpecoin.content.adapter.ItemTouchHelper
import app.carpecoin.content.models.Content
import app.carpecoin.content.room.ContentDatabase
import app.carpecoin.utils.Constants
import app.carpecoin.utils.Constants.CREATOR_PARAM
import app.carpecoin.utils.Constants.FEED_TYPE_PARAM
import app.carpecoin.utils.Constants.QUALITY_SCORE_PARAM
import app.carpecoin.utils.Constants.TIMESTAMP_PARAM
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.empty_content.view.*
import kotlinx.android.synthetic.main.fragment_content.*


private val LOG_TAG = ContentFragment::class.java.simpleName

class ContentFragment : Fragment() {
    private lateinit var feedType: String
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var binding: FragmentContentBinding
    private lateinit var contentViewModel: ContentViewModel
    private lateinit var homeViewModel: HomeViewModel

    companion object {
        @JvmStatic
        fun newInstance(contentBundle: Bundle) = ContentFragment().apply {
            arguments = contentBundle
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        feedType = ContentFragmentArgs.fromBundle(arguments).feedType
        analytics = FirebaseAnalytics.getInstance(context!!)
        analytics.setCurrentScreen(activity!!, feedType, null)
        contentViewModel = ViewModelProviders.of(this).get(ContentViewModel::class.java)
        homeViewModel = ViewModelProviders.of(activity!!).get(HomeViewModel::class.java)
        contentViewModel.contentDatabase = ContentDatabase.getAppDatabase(context!!)
        contentViewModel.feedType = feedType
        if (savedInstanceState == null) {
            homeViewModel.isRealtime.observe(this, Observer {
                when (feedType) {
                    MAIN.name -> initializeMainContent(it)
                    SAVED.name, ARCHIVED.name -> initializeCategorizedContent(feedType, homeViewModel.user.value!!.uid)
                }
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentContentBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(this)
        binding.viewmodel = contentViewModel
        binding.actionbar.viewmodel = contentViewModel
        binding.emptyContent.viewmodel = contentViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbar()
        //TODO: Handle savedInstanceState adapter position.
        initializeAdapter()
        observeSignIn()
        observeContentSelected()
        observeContentCategorizedComplete()
    }

    fun setToolbar() {
        when (feedType) {
            SAVED.name -> binding.actionbar.toolbar.title = getString(R.string.saved)
            ARCHIVED.name -> binding.actionbar.toolbar.title = getString(R.string.archived)
        }
        (activity as AppCompatActivity).setSupportActionBar(binding.actionbar.toolbar)
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    fun initializeMainContent(isRealtime: Boolean) {
        contentViewModel.initializeMainContent(isRealtime)
    }

    fun initializeCategorizedContent(feedType: String, userId: String) {
        contentViewModel.initializeCategorizedContent(feedType, userId)
    }

    private fun initializeAdapter() {
        val adapter = ContentAdapter(contentViewModel)
        contentFeedRecyclerView.layoutManager = LinearLayoutManager(context)
        when (feedType) {
            MAIN.name -> {
                contentViewModel.getMainContentList().observe(viewLifecycleOwner, Observer { homeContentList ->
                    adapter.submitList(homeContentList)
                    if (homeContentList.isEmpty()) {
                        setEmptyView()
                    } else {
                        emptyContent.visibility = View.GONE
                    }
                })
            }
            SAVED.name, ARCHIVED.name -> {
                var newFeedType = NONE
                if (feedType == SAVED.name) {
                    newFeedType = SAVED
                } else if (feedType == ARCHIVED.name) {
                    newFeedType = ARCHIVED
                }
                contentViewModel.getCategorizedContentList(newFeedType).observe(viewLifecycleOwner, Observer { contentList ->
                    adapter.submitList(contentList)
                    if (contentList.isEmpty()) {
                        setEmptyView()
                    }
                })
            }
        }
        contentFeedRecyclerView.adapter = adapter
        ItemTouchHelper().build(context!!, feedType, adapter, fragmentManager!!).attachToRecyclerView(contentFeedRecyclerView)
    }

    private fun observeContentSelected() {
        contentViewModel.contentSelected.observe(this, Observer { content ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://${content.id}")))
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, content.id)
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, content.contentTitle)
            bundle.putString(CREATOR_PARAM, content.creator)
            bundle.putString(QUALITY_SCORE_PARAM, content.qualityScore.toString())
            bundle.putString(TIMESTAMP_PARAM, content.timestamp.toString())
            bundle.putString(FEED_TYPE_PARAM, feedType)
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, content.contentType.name)
            analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
        })
    }

    private fun observeSignIn() {
        homeViewModel.user.observe(this, Observer { user: FirebaseUser? ->
            //TODO: Set based on user subscription and preference.
            initializeMainContent(true)
        })
    }

    fun setEmptyView() {
        emptyContent.visibility = View.VISIBLE
        emptyContent.confirmation.setOnClickListener { view: View ->
            view.findNavController().navigateUp()
        }
        when (feedType) {
            MAIN.name -> {
                emptyContent.title.text = getString(R.string.no_content_title)
                emptyContent.emptyInstructions.text = getString(R.string.no_feed_content_instructions)
                emptyContent.confirmation.visibility = View.GONE
            }
            SAVED.name -> {
                emptyContent.emptyImage.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_save_color_accent_24dp))
                emptyContent.title.text = getString(R.string.no_saved_content_title)
                emptyContent.swipe_right_one.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_chevron_right_color_accent_24dp))
                emptyContent.swipe_right_two.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_chevron_right_color_accent_fade_one_24dp))
                emptyContent.swipe_right_three.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_chevron_right_color_accent_fade_two_24dp))
                emptyContent.swipe_right_four.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_chevron_right_color_accent_fade_three_24dp))
                emptyContent.swipe_right_five.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_chevron_right_color_accent_fade_four_24dp))
                emptyContent.emptyInstructions.text = getString(R.string.no_saved_content_instructions)
            }
            ARCHIVED.name -> {
                emptyContent.emptyImage.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_check_color_accent_48dp))
                emptyContent.title.text = getString(R.string.no_archived_content_title)
                emptyContent.swipe_right_one.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_chevron_left_color_accent_24dp))
                emptyContent.swipe_right_two.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_chevron_left_color_accent_fade_one_24dp))
                emptyContent.swipe_right_three.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_chevron_left_color_accent_fade_two_24dp))
                emptyContent.swipe_right_four.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_chevron_left_color_accent_fade_three_24dp))
                emptyContent.swipe_right_five.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_chevron_left_color_accent_fade_four_24dp))
                emptyContent.emptyInstructions.text = getString(R.string.no_archived_content_instructions)
            }
        }
    }

    fun observeContentCategorizedComplete() {
        contentViewModel.categorizeContentComplete.observe(viewLifecycleOwner, Observer { content: Content ->
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, content.id)
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, content.contentTitle)
            bundle.putString(Constants.CREATOR_PARAM, content.creator)
            bundle.putString(Constants.QUALITY_SCORE_PARAM, content.qualityScore.toString())
            bundle.putString(Constants.TIMESTAMP_PARAM, content.timestamp.toString())
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, content.contentType.name)
            analytics.logEvent(content.feedType.name, bundle)
        })
    }

}