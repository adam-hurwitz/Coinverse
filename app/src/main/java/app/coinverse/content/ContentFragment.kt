package app.coinverse.content

import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ProgressBar.GONE
import android.widget.ProgressBar.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import app.coinverse.R.*
import app.coinverse.R.id.*
import app.coinverse.R.layout.fb_native_ad_item
import app.coinverse.R.layout.native_ad_item
import app.coinverse.content.adapter.ContentAdapter
import app.coinverse.content.adapter.ItemTouchHelper
import app.coinverse.content.models.ContentResult.ContentToPlay
import app.coinverse.content.models.ContentViewEffect
import app.coinverse.content.models.ContentViewEvent
import app.coinverse.content.models.ContentViewEvent.*
import app.coinverse.content.models.FeedViewState
import app.coinverse.databinding.FragmentContentBinding
import app.coinverse.home.HomeViewModel
import app.coinverse.user.SignInDialogFragment
import app.coinverse.utils.*
import app.coinverse.utils.Enums.ContentType.YOUTUBE
import app.coinverse.utils.Enums.FeedType.*
import app.coinverse.utils.Enums.PaymentStatus.FREE
import app.coinverse.utils.livedata.Event
import app.coinverse.utils.livedata.EventObserver
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

private val LOG_TAG = ContentFragment::class.java.simpleName

class ContentFragment : Fragment() {
    private val viewEvent: LiveData<Event<ContentViewEvent>> get() = _viewEvent
    private val _viewEvent = MutableLiveData<Event<ContentViewEvent>>()
    private lateinit var feedType: Enums.FeedType
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var binding: FragmentContentBinding
    private lateinit var contentViewModel: ContentViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var adapter: ContentAdapter
    private lateinit var moPubAdapter: MoPubRecyclerAdapter
    private var savedRecyclerPosition: Int = 0
    private var clearAdjacentAds = false
    private var openContentFromNotification = false
    private var openContentFromNotificationContentToPlay: ContentToPlay? = null

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
            if (homeViewModel.accountType.value == FREE)
                _viewEvent.value = Event(UpdateAds())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getFeedType()
        analytics = getInstance(FirebaseApp.getInstance().applicationContext)
        contentViewModel = ViewModelProviders.of(this).get(ContentViewModel::class.java)
        homeViewModel = ViewModelProviders.of(activity!!).get(HomeViewModel::class.java)
        if (savedInstanceState == null) _viewEvent.value =
                Event(FeedLoad(feedType, homeViewModel.timeframe.value!!,
                        homeViewModel.isRealtime.value!!))
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
        initAdapters()
        observeViewState()
        observeViewEffects()
    }

    override fun onResume() {
        super.onResume()
        viewEvent.observe(viewLifecycleOwner, EventObserver { event ->
            contentViewModel.processEvent(event)
        })
    }

    override fun onDestroy() {
        if (homeViewModel.accountType.value == FREE) moPubAdapter.destroy()
        super.onDestroy()
    }

    fun swipeToRefresh() {
        _viewEvent.value = Event(SwipeToRefresh(
                feedType, homeViewModel.timeframe.value!!, homeViewModel.isRealtime.value!!))
    }

    private fun initAdapters() {
        val paymentStatus = homeViewModel.accountType.value
        contentRecyclerView.layoutManager = LinearLayoutManager(context)
        adapter = ContentAdapter(contentViewModel, _viewEvent).apply {
            this.contentSelected.observe(viewLifecycleOwner, EventObserver { contentSelected ->
                _contentViewEvent.value = Event(ContentSelected(
                        getAdapterPosition(contentSelected.position), contentSelected.content))
            })
        }
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
                    .mainImageId(native_main_image)
                    .iconImageId(native_icon_image)
                    .callToActionId(native_cta)
                    .privacyInformationIconImageId(native_privacy_information_icon_image)
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
            moPubAdapter.setContentChangeStrategy(MOVE_ALL_ADS_WITH_CONTENT)
            contentRecyclerView.adapter = moPubAdapter
        } else /* PAID */ contentRecyclerView.adapter = adapter
        ItemTouchHelper(_viewEvent).apply {
            this.build(context!!, paymentStatus!!, feedType,
                    if (paymentStatus == FREE) moPubAdapter else null)
                    .attachToRecyclerView(contentRecyclerView)
        }
    }

    private fun observeViewState() {
        contentViewModel.feedViewState.observe(viewLifecycleOwner, Observer { viewState ->
            setToolbar(viewState)
            viewState.contentList.observe(viewLifecycleOwner, Observer { contentList ->
                adapter.submitList(contentList)
                _viewEvent.value = Event(FeedLoadComplete(contentList.isNotEmpty()))
                if (contentList.isNotEmpty())
                    if (savedRecyclerPosition != 0) {
                        contentRecyclerView.layoutManager?.scrollToPosition(
                                if (savedRecyclerPosition >= adapter.itemCount) adapter.itemCount - 1
                                else savedRecyclerPosition)
                        savedRecyclerPosition = 0
                    }
                openContentFromNotification()
            })
            viewState.contentToPlay.observe(viewLifecycleOwner, EventObserver { contentToPlay ->
                when (feedType) {
                    MAIN, DISMISSED ->
                        if (childFragmentManager.findFragmentByTag(CONTENT_DIALOG_FRAGMENT_TAG) == null)
                            ContentDialogFragment().newInstance(Bundle().apply {
                                putParcelable(CONTENT_TO_PLAY_KEY, contentToPlay)
                            }).show(childFragmentManager, CONTENT_DIALOG_FRAGMENT_TAG)
                    // Launches content from saved bottom sheet screen via HomeFragment.
                    SAVED -> homeViewModel.setSavedContentToPlay(contentToPlay)
                }
            })
            viewState.contentLabeled.observe(viewLifecycleOwner, EventObserver { contentLabeled ->
                //TODO - Add undo feature here.
                if (homeViewModel.accountType.value == FREE) {
                    val moPubPosition = moPubAdapter.getAdjustedPosition(contentLabeled.position)
                    if ((moPubAdapter.isAd(moPubPosition - 1) && moPubAdapter.isAd(moPubPosition + 1))) {
                        clearAdjacentAds = true
                        moPubAdapter.refreshAds(AD_UNIT_ID, RequestParameters.Builder().keywords(MOPUB_KEYWORDS).build())
                    }
                }
            })
        })
    }

    private fun observeViewEffects() {
        contentViewModel.viewEffect.observe(viewLifecycleOwner, EventObserver { effect ->
            when (effect) {
                is ContentViewEffect.SignIn -> SignInDialogFragment.newInstance(Bundle().apply {
                    putString(SIGNIN_TYPE_KEY, Enums.SignInType.DIALOG.name)
                }).show(fragmentManager, SIGNIN_DIALOG_FRAGMENT_TAG)
                is ContentViewEffect.NotifyItemChanged -> adapter.notifyItemChanged(effect.position)
                is ContentViewEffect.EnableSwipeToRefresh -> homeViewModel.enableSwipeToRefresh(effect.isEnabled)
                is ContentViewEffect.SwipeToRefresh -> homeViewModel.setSwipeToRefreshState(effect.isEnabled)
                is ContentViewEffect.ContentSwiped -> FirebaseAuth.getInstance().currentUser.let { user ->
                    _viewEvent.value = Event(ContentLabeled(
                            feedType,
                            effect.actionType,
                            user,
                            getAdapterPosition(effect.position),
                            adapter.getContent(getAdapterPosition(effect.position)),
                            if (feedType == MAIN) adapter.itemCount == 1 else false))
                }
                is ContentViewEffect.SnackBar -> when (feedType) {
                    MAIN -> snackbarWithText(effect.text, this.parentFragment?.view!!)
                    SAVED, DISMISSED -> snackbarWithText(effect.text, contentFragment)
                }
                is ContentViewEffect.ShareContentIntent ->
                    effect.contentRequest.observe(viewLifecycleOwner, EventObserver { content ->
                        startActivity(createChooser(Intent(ACTION_SEND).apply {
                            this.type = CONTENT_SHARE_TYPE
                            this.putExtra(EXTRA_SUBJECT, CONTENT_SHARE_SUBJECT_PREFFIX + content.title)
                            this.putExtra(EXTRA_TEXT,
                                    "$SHARED_VIA_COINVERSE '${content.title}' - ${content.creator}" +
                                            content.audioUrl.let { audioUrl ->
                                                if (!audioUrl.isNullOrBlank()) {
                                                    this.putExtra(EXTRA_STREAM, Uri.parse(content.previewImage))
                                                    this.type = SHARE_CONTENT_IMAGE_TYPE
                                                    this.addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                                                    "$AUDIOCAST_SHARE_MESSAGE $audioUrl"
                                                } else
                                                    if (content.contentType == YOUTUBE)
                                                        VIDEO_SHARE_MESSAGE + content.url
                                                    else SOURCE_SHARE_MESSAGE + content.url
                                            })
                        }, CONTENT_SHARE_DIALOG_TITLE))
                    })
                is ContentViewEffect.OpenContentSourceIntent ->
                    startActivity(Intent(ACTION_VIEW).setData(Uri.parse(effect.url)))
                is ContentViewEffect.ScreenEmpty -> {
                    if (!effect.isEmpty) emptyContent.visibility = GONE
                    else {
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
                                    contentRecyclerView.visibility = View.INVISIBLE
                                }
                            })
                        }
                        emptyContent.confirmation.setOnClickListener { view: View ->
                            if (feedType == SAVED && homeViewModel.bottomSheetState.value == STATE_EXPANDED)
                            //TODO - Add to HomeViewModel ViewState.
                                homeViewModel.setBottomSheetState(STATE_COLLAPSED)
                            else if (feedType == DISMISSED) view.findNavController().navigateUp()
                        }
                        when (feedType) {
                            MAIN -> {
                                emptyContent.title.text = getString(string.no_content_title)
                                emptyContent.emptyInstructions.text = getString(string.no_feed_content_instructions)
                            }
                            SAVED -> {
                                emptyContent.emptyImage.setImageDrawable(AppCompatResources.getDrawable(context!!, drawable.ic_coinverse_48dp))
                                emptyContent.title.text = getString(string.no_saved_content_title)
                                emptyContent.swipe_right_one.setImageDrawable(AppCompatResources.getDrawable(context!!, drawable.ic_chevron_right_color_accent_24dp))
                                emptyContent.swipe_right_two.setImageDrawable(AppCompatResources.getDrawable(context!!, drawable.ic_chevron_right_color_accent_fade_one_24dp))
                                emptyContent.swipe_right_three.setImageDrawable(AppCompatResources.getDrawable(context!!, drawable.ic_chevron_right_color_accent_fade_two_24dp))
                                emptyContent.swipe_right_four.setImageDrawable(AppCompatResources.getDrawable(context!!, drawable.ic_chevron_right_color_accent_fade_three_24dp))
                                emptyContent.swipe_right_five.setImageDrawable(AppCompatResources.getDrawable(context!!, drawable.ic_chevron_right_color_accent_fade_four_24dp))
                                emptyContent.emptyInstructions.text = getString(string.no_saved_content_instructions)
                                emptyContent.confirmation.visibility = View.VISIBLE
                            }
                            DISMISSED -> {
                                emptyContent.emptyImage.setImageDrawable(AppCompatResources.getDrawable(context!!, drawable.ic_dismiss_planet_light_48dp))
                                emptyContent.title.text = getString(string.no_dismissed_content_title)
                                emptyContent.swipe_right_one.setImageDrawable(AppCompatResources.getDrawable(context!!, drawable.ic_chevron_left_color_accent_24dp))
                                emptyContent.swipe_right_two.setImageDrawable(AppCompatResources.getDrawable(context!!, drawable.ic_chevron_left_color_accent_fade_one_24dp))
                                emptyContent.swipe_right_three.setImageDrawable(AppCompatResources.getDrawable(context!!, drawable.ic_chevron_left_color_accent_fade_two_24dp))
                                emptyContent.swipe_right_four.setImageDrawable(AppCompatResources.getDrawable(context!!, drawable.ic_chevron_left_color_accent_fade_three_24dp))
                                emptyContent.swipe_right_five.setImageDrawable(AppCompatResources.getDrawable(context!!, drawable.ic_chevron_left_color_accent_fade_four_24dp))
                                emptyContent.emptyInstructions.text = getString(string.no_dismissed_content_instructions)
                                emptyContent.confirmation.visibility = View.VISIBLE
                            }
                        }
                    }
                }
                is ContentViewEffect.UpdateAds -> {
                    moPubAdapter.loadAds(AD_UNIT_ID, RequestParameters.Builder().keywords(MOPUB_KEYWORDS).build())
                    moPubAdapter.setAdLoadedListener(object : MoPubNativeAdLoadedListener {
                        override fun onAdRemoved(position: Int) {}
                        override fun onAdLoaded(position: Int) {
                            if (moPubAdapter.isAd(position + 1) || moPubAdapter.isAd(position - 1))
                                moPubAdapter.refreshAds(AD_UNIT_ID, RequestParameters.Builder().keywords(MOPUB_KEYWORDS).build())
                            if (clearAdjacentAds) {
                                clearAdjacentAds = false
                                adapter.notifyDataSetChanged()
                            }
                        }
                    })
                }
            }
        })
    }

    private fun getFeedType() {
        feedType = (arguments!!.getParcelable<ContentToPlay>(OPEN_CONTENT_FROM_NOTIFICATION_KEY)).let {
            if (it == null) valueOf(ContentFragmentArgs.fromBundle(arguments!!).feedType)
            else {
                openContentFromNotification = true
                openContentFromNotificationContentToPlay = it
                it.content.feedType
            }
        }
    }

    private fun setToolbar(feedViewState: FeedViewState) {
        if (feedViewState.toolbar.isSupportActionBarEnabled) {
            binding.actionbar.toolbar.title = ""
            (activity as AppCompatActivity).setSupportActionBar(binding.actionbar.toolbar)
            (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun getAdapterPosition(originalPosition: Int) =
            if (homeViewModel.accountType.value!! == FREE)
                moPubAdapter.getOriginalPosition(originalPosition)
            else originalPosition

    private fun openContentFromNotification() {
        if (openContentFromNotification)
            openContentFromNotificationContentToPlay?.let {
                _viewEvent.value = Event(ContentSelected(it.position, it.content))
                contentRecyclerView.layoutManager?.scrollToPosition(it.position)
                openContentFromNotification = false
            }
    }
}