package app.coinverse.content

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import app.coinverse.Enums.FeedType.*
import app.coinverse.HomeViewModel
import app.coinverse.R
import app.coinverse.databinding.FragmentContentBinding
import app.coinverse.content.adapter.ContentAdapter
import app.coinverse.content.adapter.ItemTouchHelper
import app.coinverse.utils.Constants.CONTENT_FEED_VISIBILITY_DELAY
import app.coinverse.utils.Constants.CONTENT_KEY
import app.coinverse.utils.Constants.YOUTUBE_DIALOG_FRAGMENT_TAG
import app.coinverse.utils.livedata.EventObserver
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.getInstance
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
        analytics = getInstance(context!!)
        contentViewModel = ViewModelProviders.of(this).get(ContentViewModel::class.java)
        homeViewModel = ViewModelProviders.of(activity!!).get(HomeViewModel::class.java)
        contentViewModel.feedType = feedType
        if (savedInstanceState == null) {
            homeViewModel.isRealtime.observe(this, Observer { isRealtime: Boolean ->
                when (feedType) {
                    MAIN.name -> initializeMainContent(isRealtime)
                    SAVED.name, DISMISSED.name -> initializeCategorizedContent(feedType,
                            homeViewModel.user.value!!.uid)
                }
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        analytics.setCurrentScreen(activity!!, feedType, null)
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
    }

    fun setToolbar() {
        when (feedType) {
            SAVED.name -> binding.actionbar.toolbar.title = getString(R.string.saved)
            DISMISSED.name -> binding.actionbar.toolbar.title = getString(R.string.dismissed)
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
                        emptyContent.visibility = GONE
                    }
                })
            }
            SAVED.name, DISMISSED.name -> {
                var newFeedType = NONE
                if (feedType == SAVED.name) {
                    newFeedType = SAVED
                } else if (feedType == DISMISSED.name) {
                    newFeedType = DISMISSED
                }
                contentViewModel.getCategorizedContentList(newFeedType).observe(viewLifecycleOwner,
                        Observer { contentList ->
                            adapter.submitList(contentList)
                            if (contentList.isEmpty()) {
                                setEmptyView()
                            }
                        })
            }
        }
        contentFeedRecyclerView.adapter = adapter
        ItemTouchHelper(homeViewModel).build(context!!, feedType, adapter, fragmentManager!!)
                .attachToRecyclerView(contentFeedRecyclerView)
    }

    private fun observeContentSelected() {
        contentViewModel.contentSelected.observe(viewLifecycleOwner, EventObserver { content ->
            val youtubeBundle = Bundle().apply { putParcelable(CONTENT_KEY, content) }
            YouTubeDialogFragment().newInstance(youtubeBundle).show(childFragmentManager,
                    YOUTUBE_DIALOG_FRAGMENT_TAG)
        })
    }

    private fun observeSignIn() {
        homeViewModel.user.observe(this, Observer { user: FirebaseUser? ->
            //TODO: Set based on user subscription and preference.
            initializeMainContent(true)
        })
    }

    fun setEmptyView() {
        contentFeedRecyclerView.visibility = INVISIBLE
        emptyContent.visibility = VISIBLE
        contentFeedRecyclerView.postDelayed({ contentFeedRecyclerView.visibility = VISIBLE },
                CONTENT_FEED_VISIBILITY_DELAY)
        emptyContent.confirmation.setOnClickListener { view: View ->
            view.findNavController().navigateUp()
        }
        when (feedType) {
            MAIN.name -> {
                emptyContent.title.text = getString(R.string.no_content_title)
                emptyContent.emptyInstructions.text = getString(R.string.no_feed_content_instructions)
                emptyContent.confirmation.visibility = GONE
            }
            SAVED.name -> {
                emptyContent.emptyImage.setImageDrawable(ContextCompat.getDrawable(context!!,
                        R.drawable.ic_save_planet_dark_48dp))
                emptyContent.title.text = getString(R.string.no_saved_content_title)
                emptyContent.swipe_right_one.setImageDrawable(ContextCompat.getDrawable(context!!,
                        R.drawable.ic_chevron_right_color_accent_24dp))
                emptyContent.swipe_right_two.setImageDrawable(ContextCompat.getDrawable(context!!,
                        R.drawable.ic_chevron_right_color_accent_fade_one_24dp))
                emptyContent.swipe_right_three.setImageDrawable(ContextCompat.getDrawable(context!!,
                        R.drawable.ic_chevron_right_color_accent_fade_two_24dp))
                emptyContent.swipe_right_four.setImageDrawable(ContextCompat.getDrawable(context!!,
                        R.drawable.ic_chevron_right_color_accent_fade_three_24dp))
                emptyContent.swipe_right_five.setImageDrawable(ContextCompat.getDrawable(context!!,
                        R.drawable.ic_chevron_right_color_accent_fade_four_24dp))
                emptyContent.emptyInstructions.text = getString(R.string.no_saved_content_instructions)
            }
            DISMISSED.name -> {
                emptyContent.emptyImage.setImageDrawable(ContextCompat.getDrawable(context!!,
                        R.drawable.ic_dismiss_planet_light_48dp))
                emptyContent.title.text = getString(R.string.no_dismissed_content_title)
                emptyContent.swipe_right_one.setImageDrawable(ContextCompat.getDrawable(context!!,
                        R.drawable.ic_chevron_left_color_accent_24dp))
                emptyContent.swipe_right_two.setImageDrawable(ContextCompat.getDrawable(context!!,
                        R.drawable.ic_chevron_left_color_accent_fade_one_24dp))
                emptyContent.swipe_right_three.setImageDrawable(ContextCompat.getDrawable(context!!,
                        R.drawable.ic_chevron_left_color_accent_fade_two_24dp))
                emptyContent.swipe_right_four.setImageDrawable(ContextCompat.getDrawable(context!!,
                        R.drawable.ic_chevron_left_color_accent_fade_three_24dp))
                emptyContent.swipe_right_five.setImageDrawable(ContextCompat.getDrawable(context!!,
                        R.drawable.ic_chevron_left_color_accent_fade_four_24dp))
                emptyContent.emptyInstructions.text = getString(R.string.no_dismissed_content_instructions)
            }
        }
    }

}