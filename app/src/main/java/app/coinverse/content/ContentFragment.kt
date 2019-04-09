package app.coinverse.content

import android.content.Intent
import android.content.Intent.*
import android.net.Uri.parse
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import app.coinverse.Enums
import app.coinverse.Enums.ContentType.YOUTUBE
import app.coinverse.Enums.FeedType
import app.coinverse.Enums.FeedType.*
import app.coinverse.Enums.PaymentStatus.FREE
import app.coinverse.Enums.SignInType.DIALOG
import app.coinverse.Enums.Status.SUCCESS
import app.coinverse.R
import app.coinverse.R.anim
import app.coinverse.R.drawable.*
import app.coinverse.R.id.*
import app.coinverse.R.layout.fb_native_ad_item
import app.coinverse.R.layout.native_ad_item
import app.coinverse.R.string.*
import app.coinverse.content.adapter.ContentAdapter
import app.coinverse.content.adapter.ItemTouchHelper
import app.coinverse.content.models.ContentSelected
import app.coinverse.databinding.FragmentContentBinding
import app.coinverse.home.HomeViewModel
import app.coinverse.user.SignInDialogFragment
import app.coinverse.utils.*
import app.coinverse.utils.livedata.EventObserver
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.getInstance
import com.google.firebase.auth.FirebaseAuth
import com.mopub.nativeads.*
import com.mopub.nativeads.FacebookAdRenderer.FacebookViewBinder
import com.mopub.nativeads.FlurryViewBinder.Builder
import com.mopub.nativeads.MoPubRecyclerAdapter.ContentChangeStrategy.MOVE_ALL_ADS_WITH_CONTENT
import kotlinx.android.synthetic.main.empty_content.view.*
import kotlinx.android.synthetic.main.fragment_content.*
import kotlinx.android.synthetic.main.toolbar.view.*

private val LOG_TAG = ContentFragment::class.java.simpleName

class ContentFragment : Fragment() {

    private lateinit var feedType: FeedType
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var binding: FragmentContentBinding
    private lateinit var contentViewModel: ContentViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var adapter: ContentAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var moPubAdapter: MoPubRecyclerAdapter
    private var savedRecyclerPosition: Int = 0

    companion object {
        @JvmStatic
        fun newInstance(contentBundle: Bundle) = ContentFragment().apply {
            arguments = contentBundle
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (contentRecyclerView != null) outState.putInt(CONTENT_RECYCLER_VIEW_POSITION,
                (contentRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            savedRecyclerPosition = savedInstanceState.getInt(CONTENT_RECYCLER_VIEW_POSITION)
            if (homeViewModel.accountType.value == FREE) updateAds(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        feedType = FeedType.valueOf(ContentFragmentArgs.fromBundle(arguments!!).feedType)
        analytics = getInstance(FirebaseApp.getInstance().applicationContext)
        contentViewModel = ViewModelProviders.of(this).get(ContentViewModel::class.java)
        homeViewModel = ViewModelProviders.of(activity!!).get(HomeViewModel::class.java)
        contentViewModel.feedType = feedType
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        analytics.setCurrentScreen(activity!!, feedType.name, null)
        binding = FragmentContentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewmodel = contentViewModel
        binding.actionbar.viewmodel = contentViewModel
        binding.emptyContent.viewmodel = contentViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbar()
        initializeAdapters()
        //TODO: Conslidate Observers into DataModels
        observeViewModel(savedInstanceState)
        observeAdapter()
        observeItemTouchHelper()
    }

    override fun onDestroy() {
        if (homeViewModel.accountType.value == FREE) moPubAdapter.destroy()
        super.onDestroy()
    }

    fun setToolbar() {
        when (feedType) {
            SAVED -> {
                //TODO: center Saved, style, size
                binding.actionbar.toolbar.savedContentTitle.visibility = View.VISIBLE
            }
            DISMISSED -> {
                binding.actionbar.toolbar.title = getString(dismissed)
                (activity as AppCompatActivity).setSupportActionBar(binding.actionbar.toolbar)
                (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    fun initMainContent(isRealtime: Boolean) {
        contentViewModel.initMainContent(isRealtime).observe(viewLifecycleOwner, Observer { status ->
            if (status == SUCCESS && homeViewModel.accountType.value == FREE) updateAds(true)
            Log.v(LOG_TAG, "initMainContent status ${status}")
        })
    }

    fun updateAds(toLoad: Boolean) {
        var toLoad = toLoad
        moPubAdapter.loadAds(AD_UNIT_ID, RequestParameters.Builder().keywords(MOPUB_KEYWORDS).build())
        moPubAdapter.setAdLoadedListener(object : MoPubNativeAdLoadedListener {
            override fun onAdRemoved(position: Int) {}

            override fun onAdLoaded(position: Int) {
                if (moPubAdapter.isAd(position + 1) || moPubAdapter.isAd(position - 1)) {
                    moPubAdapter.refreshAds(AD_UNIT_ID, RequestParameters.Builder().keywords(MOPUB_KEYWORDS).build())
                    moPubAdapter.notifyDataSetChanged()
                } else moPubAdapter.notifyItemRangeInserted(position, 1)
                adapter.notifyDataSetChanged()
                if (toLoad) toLoad = false
            }
        })
    }

    private fun signIn(position: Int) {
        SignInDialogFragment.newInstance(Bundle().apply {
            putString(SIGNIN_TYPE_KEY, DIALOG.name)
        }).show(fragmentManager, SIGNIN_DIALOG_FRAGMENT_TAG)
        adapter.notifyItemChanged(position)
    }

    private fun initializeAdapters() {
        val paymentStatus = homeViewModel.accountType.value
        contentRecyclerView.layoutManager = LinearLayoutManager(context)
        adapter = ContentAdapter(contentViewModel)
        // FREE
        if (paymentStatus == FREE) {
            moPubAdapter = MoPubRecyclerAdapter(activity!!, adapter,
                    MoPubNativeAdPositioning.MoPubServerPositioning())
            moPubAdapter.registerAdRenderer(FacebookAdRenderer(
                    FacebookViewBinder.Builder(fb_native_ad_item)
                            .titleId(native_title)
                            .textId(native_text)
                            .mediaViewId(native_media_view)
                            .adIconViewId(native_icon_image)
                            .adChoicesRelativeLayoutId(native_ad_choices_relative_layout)
                            .advertiserNameId(native_title)
                            .callToActionId(native_cta)
                            .build()))
            val viewBinder = ViewBinder.Builder(native_ad_item)
                    .titleId(native_title)
                    .textId(native_text)
                    .mainImageId(R.id.native_main_image)
                    .iconImageId(native_icon_image)
                    .callToActionId(native_cta)
                    .privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
                    .build()
            moPubAdapter.registerAdRenderer(FlurryNativeAdRenderer(FlurryViewBinder(Builder(viewBinder))))
            moPubAdapter.registerAdRenderer(MoPubVideoNativeAdRenderer(
                    MediaViewBinder.Builder(fb_native_ad_item)
                            .mediaLayoutId(native_media_view)
                            .iconImageId(native_icon_image)
                            .titleId(native_title)
                            .textId(native_text)
                            .privacyInformationIconImageId(native_ad_choices_relative_layout)
                            .build()))
            moPubAdapter.registerAdRenderer(MoPubStaticNativeAdRenderer(viewBinder))
            if (feedType == MAIN) moPubAdapter.setContentChangeStrategy(MOVE_ALL_ADS_WITH_CONTENT)
            contentRecyclerView.adapter = moPubAdapter
        } else /* PAID */ contentRecyclerView.adapter = adapter
        itemTouchHelper = ItemTouchHelper().apply {
            this.build(context!!, paymentStatus!!, feedType,
                    if (paymentStatus == FREE) moPubAdapter else null)
                    .attachToRecyclerView(contentRecyclerView)
        }
    }

    private fun observeViewModel(savedInstanceState: Bundle?) {
        if (savedInstanceState == null && homeViewModel.accountType.value == FREE) updateAds(true)
        when (feedType) {
            MAIN -> {
                contentViewModel.getMainRoomContent().observe(viewLifecycleOwner, Observer { homeContentList ->
                    adapter.submitList(homeContentList)
                    if (homeContentList.isNotEmpty()) {
                        emptyContent.visibility = GONE
                        if (savedRecyclerPosition != 0) {
                            contentRecyclerView.layoutManager?.scrollToPosition(getRestoredAdapterPosition())
                            savedRecyclerPosition = 0
                        }
                    } else setEmptyView()
                })
            }
            SAVED, DISMISSED -> {
                contentViewModel.getCategorizedRoomContent(feedType)
                        .observe(viewLifecycleOwner, Observer { contentList ->
                            adapter.submitList(contentList)
                            //FIXME: Examine LiveData and why it's returning 0 values. Refactor to Observable.
                            if (!(contentList.size == 0 && (adapter.itemCount == 1 || adapter.itemCount == 0))) {
                                emptyContent.visibility = GONE
                                if (savedRecyclerPosition != 0) {
                                    contentRecyclerView.layoutManager?.scrollToPosition(getRestoredAdapterPosition())
                                    savedRecyclerPosition = 0
                                }
                            } else setEmptyView()
                        })
            }
        }
        contentViewModel.contentSelected.observe(viewLifecycleOwner, EventObserver { contentSelected ->
            val content = contentSelected.content
            when (feedType) {
                MAIN, DISMISSED -> {
                    if (content.contentType == Enums.ContentType.ARTICLE && contentSelected.response.equals(TTS_CHAR_LIMIT_ERROR)) {
                        homeViewModel.setBottomSheetState(BottomSheetBehavior.STATE_HIDDEN)
                        snackbarWithText(TTS_CHAR_LIMIT_MESSAGE, contentFragment)
                        emptyContent.postDelayed({
                            homeViewModel.setBottomSheetState(STATE_COLLAPSED)
                        }, BOTTOM_SHEET_COLLAPSE_DELAY)
                    } else
                        if (childFragmentManager.findFragmentByTag(CONTENT_DIALOG_FRAGMENT_TAG) == null)
                            ContentDialogFragment().newInstance(Bundle().apply {
                                putParcelable(CONTENT_SELECTED_KEY, contentSelected)
                            }).show(childFragmentManager, CONTENT_DIALOG_FRAGMENT_TAG)
                }
                // Launch content from saved bottom sheet screen via HomeFragment.
                SAVED -> {
                    if (content.contentType == Enums.ContentType.ARTICLE && contentSelected.response.equals(TTS_CHAR_LIMIT_ERROR))
                        snackbarWithText(TTS_CHAR_LIMIT_MESSAGE, contentFragment)
                    else homeViewModel.setSavedContentSelected(contentSelected)
                }
            }
            setContentProgressBar(contentSelected, View.GONE)
        })
    }

    private fun getRestoredAdapterPosition() =
            if (savedRecyclerPosition >= adapter.itemCount) adapter.itemCount - 1
            else savedRecyclerPosition

    private fun observeAdapter() {
        adapter.onContentSelected.observe(viewLifecycleOwner, EventObserver { contentSelected ->
            setContentProgressBar(contentSelected, View.VISIBLE)
            contentViewModel.onContentClicked(contentSelected)
        })
        adapter.onContentSourceOpened.observe(viewLifecycleOwner, EventObserver { contentUrl ->
            startActivity(Intent(ACTION_VIEW).setData(parse(contentUrl)))
        })
        adapter.onContentShared.observe(viewLifecycleOwner, EventObserver { content ->
            contentViewModel.getContent(content.id).observe(viewLifecycleOwner, EventObserver { content ->
                val shareTextContent =
                        "$SHARED_VIA_COINVERSE '${content.title}' - ${content.creator}" +
                                content.audioUrl.let { audioUrl ->
                                    if (!audioUrl.isNullOrBlank())
                                        "$AUDIOCAST_SHARE_MESSAGE $audioUrl"
                                    else
                                        if (content.contentType == YOUTUBE)
                                            VIDEO_SHARE_MESSAGE + content.url
                                        else SOURCE_SHARE_MESSAGE + content.url
                                }
                startActivity(createChooser(Intent(ACTION_SEND).apply {
                    this.type = CONTENT_SHARE_TYPE
                    this.putExtra(EXTRA_SUBJECT, CONTENT_SHARE_SUBJECT_PREFFIX + content.title)
                    this.putExtra(EXTRA_TEXT, shareTextContent)
                    if (content.contentType != YOUTUBE) {
                        this.putExtra(EXTRA_STREAM, parse(content.previewImage))
                        this.type = SHARE_CONTENT_TYPE
                        this.addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }, CONTENT_SHARE_TITLE))
            })
        })
    }

    private fun observeItemTouchHelper() {
        itemTouchHelper.onChildDraw.observe(viewLifecycleOwner, EventObserver { isDrawing ->
            if (isDrawing) homeViewModel.enableSwipeToRefresh(false)
        })
        itemTouchHelper.onContentSwiped.observe(viewLifecycleOwner, EventObserver { contentSwiped ->
            FirebaseAuth.getInstance().currentUser.let { user ->
                val position =
                        if (homeViewModel.accountType.value!! == FREE)
                            moPubAdapter.getOriginalPosition(contentSwiped.position)
                        else contentSwiped.position
                if (user != null)
                //TODO: Return LiveData of Status.
                    contentViewModel.organizeContent(
                            feedType,
                            contentSwiped.actionType,
                            user,
                            adapter.getContent(position),
                            if (feedType == MAIN) adapter.itemCount == 1 else false)
                            .observe(viewLifecycleOwner, Observer { contentSwipedStatus ->
                                if (contentSwipedStatus.status == SUCCESS && contentSwipedStatus.feedType == MAIN)
                                //TODO: Return LiveData of Status.
                                    contentViewModel.updateActions(contentSwiped.actionType, adapter.getContent(position)!!, user)
                                Log.v(LOG_TAG, "Move to ${contentSwiped.actionType} status: ${contentSwipedStatus.status}")
                            })
                else signIn(position)
            }
        })
    }

    private fun setContentProgressBar(contentSelected: ContentSelected, visibility: Int) {
        contentViewModel.setContentLoadingStatus(contentSelected.content.id, visibility)
        if (homeViewModel.accountType.value == FREE) moPubAdapter.notifyItemChanged(contentSelected.position)
        else adapter.notifyItemChanged(contentSelected.position)
    }

    private fun setEmptyView() {
        if (emptyContent.visibility == GONE) {
            val fadeIn = AnimationUtils.loadAnimation(context, anim.fade_in)
            emptyContent.startAnimation(fadeIn)
            fadeIn.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {/*Do something.*/
                }

                override fun onAnimationEnd(animation: Animation?) {
                    emptyContent.visibility = VISIBLE
                    contentRecyclerView.visibility = VISIBLE
                }

                override fun onAnimationStart(animation: Animation?) {
                    contentRecyclerView.visibility = INVISIBLE
                }
            })
        }
        emptyContent.confirmation.setOnClickListener { view: View ->
            if (homeViewModel.bottomSheetState.value == STATE_EXPANDED)
                homeViewModel.setBottomSheetState(STATE_COLLAPSED)
            else view.findNavController().navigateUp()
        }
        when (feedType) {
            MAIN -> {
                emptyContent.title.text = getString(no_content_title)
                emptyContent.emptyInstructions.text = getString(no_feed_content_instructions)
                emptyContent.confirmation.visibility = GONE
            }
            SAVED -> {
                emptyContent.emptyImage.setImageDrawable(getDrawable(context!!, ic_coinverse_48dp))
                emptyContent.title.text = getString(no_saved_content_title)
                emptyContent.swipe_right_one.setImageDrawable(getDrawable(context!!, ic_chevron_right_color_accent_24dp))
                emptyContent.swipe_right_two.setImageDrawable(getDrawable(context!!, ic_chevron_right_color_accent_fade_one_24dp))
                emptyContent.swipe_right_three.setImageDrawable(getDrawable(context!!, ic_chevron_right_color_accent_fade_two_24dp))
                emptyContent.swipe_right_four.setImageDrawable(getDrawable(context!!, ic_chevron_right_color_accent_fade_three_24dp))
                emptyContent.swipe_right_five.setImageDrawable(getDrawable(context!!, ic_chevron_right_color_accent_fade_four_24dp))
                emptyContent.emptyInstructions.text = getString(no_saved_content_instructions)
            }
            DISMISSED -> {
                emptyContent.emptyImage.setImageDrawable(getDrawable(context!!, ic_dismiss_planet_light_48dp))
                emptyContent.title.text = getString(no_dismissed_content_title)
                emptyContent.swipe_right_one.setImageDrawable(getDrawable(context!!, ic_chevron_left_color_accent_24dp))
                emptyContent.swipe_right_two.setImageDrawable(getDrawable(context!!, ic_chevron_left_color_accent_fade_one_24dp))
                emptyContent.swipe_right_three.setImageDrawable(getDrawable(context!!, ic_chevron_left_color_accent_fade_two_24dp))
                emptyContent.swipe_right_four.setImageDrawable(getDrawable(context!!, ic_chevron_left_color_accent_fade_three_24dp))
                emptyContent.swipe_right_five.setImageDrawable(getDrawable(context!!, ic_chevron_left_color_accent_fade_four_24dp))
                emptyContent.emptyInstructions.text = getString(no_dismissed_content_instructions)
            }
        }
    }
}