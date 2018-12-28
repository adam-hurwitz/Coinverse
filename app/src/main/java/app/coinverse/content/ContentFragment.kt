package app.coinverse.content

import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import app.coinverse.Enums.ContentType.ARTICLE
import app.coinverse.Enums.FeedType.*
import app.coinverse.R
import app.coinverse.content.adapter.ContentAdapter
import app.coinverse.content.adapter.ItemTouchHelper
import app.coinverse.content.models.ContentSelected
import app.coinverse.databinding.FragmentContentBinding
import app.coinverse.home.HomeViewModel
import app.coinverse.utils.*
import app.coinverse.utils.livedata.Event
import app.coinverse.utils.livedata.EventObserver
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.getInstance
import com.google.firebase.auth.FirebaseUser
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.empty_content.view.*
import kotlinx.android.synthetic.main.fragment_content.*
import kotlinx.android.synthetic.main.toolbar.view.*

private val LOG_TAG = ContentFragment::class.java.simpleName

class ContentFragment : Fragment() {

    private lateinit var feedType: String
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var binding: FragmentContentBinding
    private lateinit var contentViewModel: ContentViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var adapter: ContentAdapter

    private val compositeDisposable = CompositeDisposable()

    private var savedRecyclerLayoutState: Parcelable? = null

    companion object {
        @JvmStatic
        fun newInstance(contentBundle: Bundle) = ContentFragment().apply {
            arguments = contentBundle
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        when (feedType) {
            MAIN.name, DISMISSED.name -> {
                if (contentRecyclerView != null)
                    outState.putParcelable(CONTENT_RECYCLER_VIEW_STATE,
                            contentRecyclerView.layoutManager!!.onSaveInstanceState())
            }
            //TODO: Refactor to use outState if possible.
            SAVED.name -> homeViewModel.savedContentState = contentRecyclerView.layoutManager?.onSaveInstanceState()
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            when (feedType) {
                MAIN.name, DISMISSED.name ->
                    savedRecyclerLayoutState = savedInstanceState.getParcelable(CONTENT_RECYCLER_VIEW_STATE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        feedType = ContentFragmentArgs.fromBundle(arguments!!).feedType
        analytics = getInstance(FirebaseApp.getInstance()!!.applicationContext)
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
        initializeAdapter()
        observeSignIn()
        observeNewContentAdded()
        observeContentSelectedLaunchMedia()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    fun setToolbar() {
        when (feedType) {
            SAVED.name -> {
                //TODO: center Saved, style, size
                binding.actionbar.toolbar.savedContentTitle.visibility = View.VISIBLE
            }
            DISMISSED.name -> {
                binding.actionbar.toolbar.title = getString(R.string.dismissed)
                (activity as AppCompatActivity).setSupportActionBar(binding.actionbar.toolbar)
                (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    fun initializeMainContent(isRealtime: Boolean) {
        contentViewModel.initializeMainContent(isRealtime)
    }

    fun initializeCategorizedContent(feedType: String, userId: String) {
        contentViewModel.initializeCategorizedContent(feedType, userId)
    }

    private fun initializeAdapter() {
        adapter = ContentAdapter(contentViewModel)
        contentRecyclerView.layoutManager = LinearLayoutManager(context)
        when (feedType) {
            MAIN.name -> {
                contentViewModel.getMainContentList().observe(viewLifecycleOwner, Observer { homeContentList ->
                    adapter.submitList(homeContentList)
                    if (homeContentList.isNotEmpty()) {
                        emptyContent.visibility = GONE
                        if (savedRecyclerLayoutState != null)
                            contentRecyclerView.layoutManager?.onRestoreInstanceState(savedRecyclerLayoutState)
                    } else setEmptyView()
                })
            }
            SAVED.name, DISMISSED.name -> {
                var newFeedType = NONE
                if (feedType == SAVED.name) newFeedType = SAVED
                else if (feedType == DISMISSED.name) newFeedType = DISMISSED
                contentViewModel.getCategorizedContentList(newFeedType).observe(viewLifecycleOwner, Observer { contentList ->
                    adapter.submitList(contentList)
                    if (contentList.isNotEmpty()) {
                        emptyContent.visibility = GONE
                        if (feedType == SAVED.name && homeViewModel.savedContentState != null)
                            contentRecyclerView.layoutManager?.onRestoreInstanceState(homeViewModel.savedContentState)
                        if (feedType == DISMISSED.name)
                            contentRecyclerView.layoutManager?.onRestoreInstanceState(savedRecyclerLayoutState)
                    } else setEmptyView()
                })
            }
        }
        contentRecyclerView.adapter = adapter
        ItemTouchHelper(homeViewModel).build(context!!, feedType, adapter, fragmentManager!!)
                .attachToRecyclerView(contentRecyclerView)
        observeContentSelected()
        observeContentShared()
        observeContentSourceOpened()
    }

    //TODO: Needs further testing.
    private fun observeNewContentAdded() {
        /*contentViewModel.isNewContentAddedLiveData.observe(viewLifecycleOwner, Observer { isNewContent ->
            if (feedType == MAIN.name && isNewContent) contentRecyclerView.scrollToPosition(0)
        })*/
    }

    private fun observeContentSelected() {
        compositeDisposable.add(adapter.onContentSelected()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ contentSelected ->
                    setContentProgressBar(contentSelected, VISIBLE)
                    contentViewModel.onContentClicked(contentSelected)
                }, { throwable -> Log.e(LOG_TAG, throwable.toString()) }))
    }

    private fun observeContentShared() {
        compositeDisposable.add(adapter.onContentShared()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ content ->
                    startActivity(createChooser(Intent(ACTION_SEND)
                            .setType(CONTENT_SHARE_TYPE)
                            .putExtra(EXTRA_SUBJECT, CONTENT_SHARE_SUBJECT_PREFFIX + content.title)
                            .putExtra(EXTRA_TEXT, CONTENT_SHARE_TEXT_PREFFIX + content.url),
                            CONTENT_SHARE_TITLE))
                }, { throwable -> Log.e(LOG_TAG, throwable.toString()) }))
    }

    private fun observeContentSourceOpened() {
        compositeDisposable.add(adapter.onContentSourceOpened()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ contentUrl ->
                    startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(contentUrl)))
                }, { throwable -> Log.e(LOG_TAG, throwable.toString()) }))
    }


    private fun observeContentSelectedLaunchMedia() {
        contentViewModel.contentSelected.observe(viewLifecycleOwner, EventObserver { contentSelected ->
            val content = contentSelected.content
            when (feedType) {
                MAIN.name, DISMISSED.name -> {
                    if (content.contentType == ARTICLE && content.audioUrl.equals(TTS_CHAR_LIMIT_ERROR)) {
                        homeViewModel.bottomSheetState.value = BottomSheetBehavior.STATE_HIDDEN
                        snackbarWithText(TTS_CHAR_LIMIT_MESSAGE, contentFragment)
                        emptyContent.postDelayed({
                            homeViewModel.bottomSheetState.value = STATE_COLLAPSED
                        }, BOTTOM_SHEET_COLLAPSE_DELAY)
                    } else {
                        if (childFragmentManager.findFragmentByTag(CONTENT_DIALOG_FRAGMENT_TAG) == null)
                            ContentDialogFragment()
                                    .newInstance(Bundle().apply { putParcelable(CONTENT_KEY, content) })
                                    .show(childFragmentManager, CONTENT_DIALOG_FRAGMENT_TAG)
                    }
                }
                // Launch content from saved bottom sheet screen via HomeFragment.
                SAVED.name -> {
                    if (content.contentType == ARTICLE && content.audioUrl.equals(TTS_CHAR_LIMIT_ERROR))
                        snackbarWithText(TTS_CHAR_LIMIT_MESSAGE, contentFragment)
                    else homeViewModel._savedContentSelected.value = Event(content) // Trigger the event by setting a new Event as a new value
                }
            }
            setContentProgressBar(contentSelected, GONE)
        })
    }

    private fun observeSignIn() {
        homeViewModel.user.observe(this, Observer { user: FirebaseUser? ->
            //TODO: Set based on user subscription and preference.
            initializeMainContent(true)
        })
    }

    private fun setContentProgressBar(contentSelected: ContentSelected, visibility: Int) {
        contentViewModel.contentLoadingStatusMap.put(contentSelected.content.id, visibility)
        adapter.notifyItemChanged(contentSelected.position)
    }

    fun setEmptyView() {
        contentRecyclerView.visibility = INVISIBLE
        emptyContent.visibility = VISIBLE
        contentRecyclerView.postDelayed({ contentRecyclerView.visibility = VISIBLE },
                CONTENT_FEED_VISIBILITY_DELAY)
        emptyContent.confirmation.setOnClickListener { view: View ->
            if (homeViewModel.bottomSheetState.value == STATE_EXPANDED)
                homeViewModel.bottomSheetState.value = STATE_COLLAPSED
            else view.findNavController().navigateUp()
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