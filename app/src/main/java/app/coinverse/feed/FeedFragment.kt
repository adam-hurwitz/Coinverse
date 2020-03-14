package app.coinverse.feed

import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import app.App
import app.coinverse.R
import app.coinverse.R.anim.fade_in
import app.coinverse.R.drawable.*
import app.coinverse.R.id.*
import app.coinverse.R.layout.fb_native_ad_item
import app.coinverse.R.layout.native_ad_item
import app.coinverse.R.string
import app.coinverse.R.string.*
import app.coinverse.analytics.Analytics
import app.coinverse.content.views.ContentDialogFragment
import app.coinverse.databinding.FragmentFeedBinding
import app.coinverse.feed.adapter.FeedAdapter
import app.coinverse.feed.adapter.initItemTouchHelper
import app.coinverse.feed.models.ContentToPlay
import app.coinverse.feed.models.FeedViewEventType.*
import app.coinverse.feed.models.FeedViewEvents
import app.coinverse.feed.viewmodel.FeedViewModel
import app.coinverse.feed.viewmodel.FeedViewModelFactory
import app.coinverse.home.HomeViewModel
import app.coinverse.user.SignInDialogFragment
import app.coinverse.utils.*
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.PaymentStatus.FREE
import app.coinverse.utils.SignInType.DIALOG
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.firebase.auth.FirebaseAuth
import com.mopub.nativeads.*
import com.mopub.nativeads.FacebookAdRenderer.FacebookViewBinder
import com.mopub.nativeads.FlurryViewBinder.Builder
import com.mopub.nativeads.MoPubRecyclerAdapter.ContentChangeStrategy.MOVE_ALL_ADS_WITH_CONTENT
import kotlinx.android.synthetic.main.empty_feed.view.*
import kotlinx.android.synthetic.main.fragment_feed.*
import kotlinx.android.synthetic.main.toolbar_app.view.*
import javax.inject.Inject

private val LOG_TAG = FeedFragment::class.java.simpleName

class FeedFragment : Fragment() {

    @Inject
    lateinit var analytics: Analytics
    @Inject
    lateinit var repository: FeedRepository

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val feedViewModel: FeedViewModel by viewModels {
        FeedViewModelFactory(
                this,
                repository = repository,
                analytics = analytics,
                feedType = feedType,
                timeframe = homeViewModel.timeframe.value!!,
                isRealtime = homeViewModel.isRealtime.value!!)
    }
    private var clearAdjacentAds = false
    private var openContentFromNotification = false
    private var openContentFromNotificationContentToPlay: ContentToPlay? = null

    private lateinit var viewEvents: FeedViewEvents
    private lateinit var feedType: FeedType
    private lateinit var binding: FragmentFeedBinding
    private lateinit var adapter: FeedAdapter
    private lateinit var moPubAdapter: MoPubRecyclerAdapter

    fun newInstance(contentBundle: Bundle) = FeedFragment().apply {
        arguments = contentBundle
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (homeViewModel.accountType.value == FREE) viewEvents.updateAds(UpdateAds())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context.applicationContext as App).appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getFeedType()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        analytics.setCurrentScreen(requireActivity(), feedType.name)
        feedViewModel.attachEvents(this)
        binding = FragmentFeedBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapters()
        observeViewState()
        observeViewEffects()
    }

    override fun onDestroy() {
        if (homeViewModel.accountType.value == FREE) moPubAdapter.destroy()
        super.onDestroy()
    }

    fun initEvents(viewEvents: FeedViewEvents) {
        this.viewEvents = viewEvents
    }

    fun swipeToRefresh() {
        viewEvents.swipeToRefresh(SwipeToRefresh(
                feedType, homeViewModel.timeframe.value!!, homeViewModel.isRealtime.value!!))
    }

    private fun getFeedType() {
        feedType = (requireArguments().getParcelable<ContentToPlay>(OPEN_CONTENT_FROM_NOTIFICATION_KEY)).let {
            if (it == null) FeedType.valueOf(FeedFragmentArgs.fromBundle(requireArguments()).feedType)
            else {
                openContentFromNotification = true
                openContentFromNotificationContentToPlay = it
                it.content.feedType
            }
        }
    }

    private fun initAdapters() {
        val paymentStatus = homeViewModel.accountType.value
        contentRecyclerView.layoutManager = LinearLayoutManager(context)
        contentRecyclerView.setHasFixedSize(true)
        adapter = FeedAdapter(feedViewModel, viewEvents)
        /** Free account */
        if (paymentStatus == FREE) {
            moPubAdapter = MoPubRecyclerAdapter(
                    requireActivity(),
                    adapter,
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
                    .privacyInformationIconImageId(string.native_privacy_information_icon_image)
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
        } else {
            /** Paid account */
            contentRecyclerView.adapter = adapter
        }
        initItemTouchHelper(
                context = requireContext(),
                resources = resources,
                paymentStatus = paymentStatus!!,
                feedType = feedType,
                moPubAdapter = if (paymentStatus == FREE) moPubAdapter else null,
                viewEvents = viewEvents
        ).attachToRecyclerView(contentRecyclerView)
    }

    private fun observeViewState() {
        setToolbar(feedViewModel.state.toolbarState)
        feedViewModel.state.feedList.observe(viewLifecycleOwner) { pagedList ->
            adapter.submitList(pagedList)
            viewEvents.feedLoadComplete(FeedLoadComplete(pagedList.isNotEmpty()))
            openContentFromNotification()
        }
        feedViewModel.state.contentToPlay.observe(viewLifecycleOwner) { contentToPlay ->
            contentToPlay?.let {
                when (feedType) {
                    MAIN, DISMISSED ->
                        if (childFragmentManager.findFragmentByTag(CONTENT_DIALOG_FRAGMENT_TAG) == null)
                            ContentDialogFragment().newInstance(Bundle().apply {
                                putParcelable(CONTENT_TO_PLAY_KEY, contentToPlay)
                            }).show(childFragmentManager, CONTENT_DIALOG_FRAGMENT_TAG)
                    // Launches content from saved bottom sheet screen via HomeFragment.
                    SAVED -> homeViewModel.setSavedContentToPlay(contentToPlay)
                }
            }
        }
        feedViewModel.state.contentLabeledPosition.observe(viewLifecycleOwner) { position ->
            //TODO: Undo feature
            if (homeViewModel.accountType.value == FREE) {
                position?.let {
                    val moPubPosition = moPubAdapter.getAdjustedPosition(position)
                    if ((moPubAdapter.isAd(moPubPosition - 1) && moPubAdapter.isAd(moPubPosition + 1))) {
                        clearAdjacentAds = true
                        moPubAdapter.refreshAds(AD_UNIT_ID, RequestParameters.Builder().keywords(MOPUB_KEYWORDS).build())
                    }
                }
            }
        }
    }

    private fun observeViewEffects() {
        feedViewModel.effects.signIn.observe(viewLifecycleOwner) {
            SignInDialogFragment().newInstance(Bundle().apply {
                putString(SIGNIN_TYPE_KEY, DIALOG.name)
            }).show(parentFragmentManager, SIGNIN_DIALOG_FRAGMENT_TAG)
        }
        feedViewModel.effects.notifyItemChanged.observe(viewLifecycleOwner) {
            adapter.notifyItemChanged(it.position)
        }
        feedViewModel.effects.enableSwipeToRefresh.observe(viewLifecycleOwner) {
            homeViewModel.enableSwipeToRefresh(it.isEnabled)
        }
        feedViewModel.effects.swipeToRefresh.observe(viewLifecycleOwner) {
            homeViewModel.setSwipeToRefreshState(it.isEnabled)
        }
        feedViewModel.effects.contentSwiped.observe(viewLifecycleOwner) {
            FirebaseAuth.getInstance().currentUser.let { user ->
                viewEvents.contentLabeled(ContentLabeled(
                        feedType = feedType,
                        actionType = it.actionType,
                        user = user,
                        position = getAdapterPosition(it.position),
                        content = adapter.getContent(getAdapterPosition(it.position)),
                        isMainFeedEmptied = if (feedType == MAIN) adapter.itemCount == 1 else false))
            }
        }
        feedViewModel.effects.snackBar.observe(viewLifecycleOwner) {
            when (feedType) {
                MAIN -> snackbarWithText(resources, it.text, this.requireParentFragment().requireView())
                SAVED, DISMISSED -> snackbarWithText(resources, it.text, contentFragment)
            }
        }
        feedViewModel.effects.shareContentIntent.observe(viewLifecycleOwner) {
            it.contentRequest.observe(viewLifecycleOwner) { content ->
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
            }
        }
        feedViewModel.effects.openContentSourceIntent.observe(viewLifecycleOwner) {
            startActivity(Intent(ACTION_VIEW).setData(Uri.parse(it.url)))
        }
        feedViewModel.effects.screenEmpty.observe(viewLifecycleOwner) {
            if (!it.isEmpty) emptyContent.visibility = GONE
            else {
                if (emptyContent.visibility == GONE) {
                    val fadeIn = AnimationUtils.loadAnimation(context, fade_in)
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
                    if (feedType == SAVED && homeViewModel.bottomSheetState.value == STATE_EXPANDED) {
                        //TODO: Add to HomeViewModel ViewState.
                        homeViewModel.setBottomSheetState(STATE_COLLAPSED)
                    } else if (feedType == DISMISSED) view.findNavController().navigateUp()
                }
                when (feedType) {
                    MAIN -> {
                        binding.emptyContent.shootingStarOne.visibility = VISIBLE
                        binding.emptyContent.earth.visibility = VISIBLE
                        emptyContent.title.text = getString(no_content_title)
                        emptyContent.emptyInstructions.text = getString(no_feed_content_instructions)
                    }
                    SAVED -> {
                        binding.emptyContent.shootingStarOne.visibility = GONE
                        binding.emptyContent.earth.visibility = GONE
                        emptyContent.emptyImage.setImageDrawable(getDrawable(requireContext(),
                                ic_coinverse_48dp))
                        emptyContent.title.text = getString(no_saved_content_title)
                        emptyContent.swipe_right_one.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_right_color_accent_24dp))
                        emptyContent.swipe_right_two.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_right_color_accent_fade_one_24dp))
                        emptyContent.swipe_right_three.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_right_color_accent_fade_two_24dp))
                        emptyContent.swipe_right_four.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_right_color_accent_fade_three_24dp))
                        emptyContent.swipe_right_five.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_right_color_accent_fade_four_24dp))
                        emptyContent.emptyInstructions.text =
                                getString(no_saved_content_instructions)
                        emptyContent.confirmation.visibility = VISIBLE
                    }
                    DISMISSED -> {
                        binding.emptyContent.shootingStarOne.visibility = GONE
                        binding.emptyContent.earth.visibility = GONE
                        emptyContent.emptyImage.setImageDrawable(getDrawable(requireContext(),
                                ic_dismiss_planet_light_48dp))
                        emptyContent.title.text = getString(no_dismissed_content_title)
                        emptyContent.swipe_right_one.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_left_color_accent_24dp))
                        emptyContent.swipe_right_two.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_left_color_accent_fade_one_24dp))
                        emptyContent.swipe_right_three.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_left_color_accent_fade_two_24dp))
                        emptyContent.swipe_right_four.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_left_color_accent_fade_three_24dp))
                        emptyContent.swipe_right_five.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_left_color_accent_fade_four_24dp))
                        emptyContent.emptyInstructions.text =
                                getString(no_dismissed_content_instructions)
                        emptyContent.confirmation.visibility = VISIBLE
                    }
                }
            }
        }
        feedViewModel.effects.updateAds.observe(viewLifecycleOwner) {
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

    private fun setToolbar(toolbarState: ToolbarState) {
        binding.appbar.appBarLayout.visibility = feedViewModel.state.toolbarState.visibility
        binding.appbar.appBarLayout.titleToolbar.text = context?.getString(feedViewModel.state.toolbarState.titleRes)
        if (toolbarState.isActionBarEnabled) {
            appbar.toolbar.title = ""
            (activity as AppCompatActivity).setSupportActionBar(appbar.toolbar)
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
                viewEvents.contentSelected(ContentSelected(it.content, it.position))
                contentRecyclerView.layoutManager?.scrollToPosition(it.position)
                openContentFromNotification = false
            }
    }
}