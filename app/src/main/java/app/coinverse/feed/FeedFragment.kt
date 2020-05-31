package app.coinverse.feed

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_VIEW
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.createChooser
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
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
import app.coinverse.App
import app.coinverse.R
import app.coinverse.R.anim.fade_in
import app.coinverse.R.drawable.ic_chevron_left_color_accent_24dp
import app.coinverse.R.drawable.ic_chevron_left_color_accent_fade_four_24dp
import app.coinverse.R.drawable.ic_chevron_left_color_accent_fade_one_24dp
import app.coinverse.R.drawable.ic_chevron_left_color_accent_fade_three_24dp
import app.coinverse.R.drawable.ic_chevron_left_color_accent_fade_two_24dp
import app.coinverse.R.drawable.ic_chevron_right_color_accent_24dp
import app.coinverse.R.drawable.ic_chevron_right_color_accent_fade_four_24dp
import app.coinverse.R.drawable.ic_chevron_right_color_accent_fade_one_24dp
import app.coinverse.R.drawable.ic_chevron_right_color_accent_fade_three_24dp
import app.coinverse.R.drawable.ic_chevron_right_color_accent_fade_two_24dp
import app.coinverse.R.drawable.ic_coinverse_48dp
import app.coinverse.R.drawable.ic_dismiss_planet_light_48dp
import app.coinverse.R.id.native_ad_choices_relative_layout
import app.coinverse.R.id.native_cta
import app.coinverse.R.id.native_icon_image
import app.coinverse.R.id.native_media_view
import app.coinverse.R.id.native_text
import app.coinverse.R.id.native_title
import app.coinverse.R.layout.fb_native_ad_item
import app.coinverse.R.layout.native_ad_item
import app.coinverse.R.string
import app.coinverse.R.string.no_content_title
import app.coinverse.R.string.no_dismissed_content_instructions
import app.coinverse.R.string.no_dismissed_content_title
import app.coinverse.R.string.no_feed_content_instructions
import app.coinverse.R.string.no_saved_content_instructions
import app.coinverse.R.string.no_saved_content_title
import app.coinverse.analytics.Analytics
import app.coinverse.content.views.ContentDialogFragment
import app.coinverse.databinding.FragmentFeedBinding
import app.coinverse.feed.adapter.FeedAdapter
import app.coinverse.feed.adapter.initItemTouchHelper
import app.coinverse.feed.models.ContentToPlay
import app.coinverse.feed.models.FeedViewEvent
import app.coinverse.feed.models.FeedViewEventType.ContentLabeled
import app.coinverse.feed.models.FeedViewEventType.ContentSelected
import app.coinverse.feed.models.FeedViewEventType.FeedLoadComplete
import app.coinverse.feed.models.FeedViewEventType.SwipeToRefresh
import app.coinverse.feed.models.FeedViewEventType.UpdateAds
import app.coinverse.feed.viewmodel.FeedViewModel
import app.coinverse.feed.viewmodel.FeedViewModelFactory
import app.coinverse.home.HomeViewModel
import app.coinverse.user.SignInDialogFragment
import app.coinverse.utils.AD_UNIT_ID
import app.coinverse.utils.AUDIOCAST_SHARE_MESSAGE
import app.coinverse.utils.CONTENT_DIALOG_FRAGMENT_TAG
import app.coinverse.utils.CONTENT_SHARE_DIALOG_TITLE
import app.coinverse.utils.CONTENT_SHARE_SUBJECT_PREFFIX
import app.coinverse.utils.CONTENT_SHARE_TYPE
import app.coinverse.utils.CONTENT_TO_PLAY_KEY
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.ERROR
import app.coinverse.utils.FeedType
import app.coinverse.utils.FeedType.DISMISSED
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.FeedType.SAVED
import app.coinverse.utils.MOPUB_KEYWORDS
import app.coinverse.utils.OPEN_CONTENT_FROM_NOTIFICATION_KEY
import app.coinverse.utils.PaymentStatus.FREE
import app.coinverse.utils.SHARED_VIA_COINVERSE
import app.coinverse.utils.SHARE_CONTENT_IMAGE_TYPE
import app.coinverse.utils.SIGNIN_DIALOG_FRAGMENT_TAG
import app.coinverse.utils.SIGNIN_TYPE_KEY
import app.coinverse.utils.SOURCE_SHARE_MESSAGE
import app.coinverse.utils.SignInType.DIALOG
import app.coinverse.utils.ToolbarState
import app.coinverse.utils.VIDEO_SHARE_MESSAGE
import app.coinverse.utils.snackbarWithText
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.firebase.auth.FirebaseAuth
import com.mopub.nativeads.FacebookAdRenderer
import com.mopub.nativeads.FlurryNativeAdRenderer
import com.mopub.nativeads.FlurryViewBinder
import com.mopub.nativeads.FlurryViewBinder.Builder
import com.mopub.nativeads.MediaViewBinder
import com.mopub.nativeads.MoPubNativeAdLoadedListener
import com.mopub.nativeads.MoPubNativeAdPositioning
import com.mopub.nativeads.MoPubRecyclerAdapter
import com.mopub.nativeads.MoPubRecyclerAdapter.ContentChangeStrategy.MOVE_ALL_ADS_WITH_CONTENT
import com.mopub.nativeads.MoPubStaticNativeAdRenderer
import com.mopub.nativeads.MoPubVideoNativeAdRenderer
import com.mopub.nativeads.RequestParameters
import com.mopub.nativeads.ViewBinder
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private lateinit var viewEvent: FeedViewEvent
    private lateinit var feedType: FeedType
    private lateinit var binding: FragmentFeedBinding
    private lateinit var adapter: FeedAdapter
    private lateinit var moPubAdapter: MoPubRecyclerAdapter

    fun newInstance(contentBundle: Bundle) = FeedFragment().apply {
        arguments = contentBundle
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (homeViewModel.accountType.value == FREE) viewEvent.updateAds(UpdateAds())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context.applicationContext as App).component.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getFeedType()
    }

    @ExperimentalCoroutinesApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        analytics.setCurrentScreen(requireActivity(), feedType.name)
        feedViewModel.launchViewEvents(this)
        binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapters()
        initViewStates()
        initViewEffects()
    }

    override fun onDestroy() {
        if (homeViewModel.accountType.value == FREE) moPubAdapter.destroy()
        super.onDestroy()
    }

    fun attachViewEvents(viewEvent: FeedViewEvent) {
        this.viewEvent = viewEvent
    }

    fun swipeToRefresh() {
        viewEvent.swipeToRefresh(SwipeToRefresh(
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

    @ExperimentalCoroutinesApi
    private fun initAdapters() {
        val paymentStatus = homeViewModel.accountType.value
        binding.contentRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.contentRecyclerView.setHasFixedSize(true)
        adapter = FeedAdapter(feedViewModel, viewEvent)
        moPubAdapter = MoPubRecyclerAdapter(
                requireActivity(),
                adapter,
                MoPubNativeAdPositioning.MoPubServerPositioning())
        /** Free account */
        if (paymentStatus == FREE) {
            moPubAdapter.registerAdRenderer(FacebookAdRenderer(
                    FacebookAdRenderer.FacebookViewBinder.Builder(fb_native_ad_item)
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
            binding.contentRecyclerView.adapter = moPubAdapter
        } else {
            /** Paid account */
            binding.contentRecyclerView.adapter = adapter
        }
        initItemTouchHelper(
                context = requireContext(),
                resources = resources,
                paymentStatus = paymentStatus!!,
                feedType = feedType,
                moPubAdapter = if (paymentStatus == FREE) moPubAdapter else null,
                viewEvent = viewEvent
        ).attachToRecyclerView(binding.contentRecyclerView)
    }

    @ExperimentalCoroutinesApi
    private fun initViewStates() {
        setToolbar(feedViewModel.state.toolbarState)
        feedViewModel.state.feedList.observe(viewLifecycleOwner) { pagedList ->
            adapter.submitList(pagedList)
            viewEvent.feedLoadComplete(FeedLoadComplete(pagedList.isNotEmpty()))
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
                position.let {
                    val moPubPosition = moPubAdapter.getAdjustedPosition(position)
                    if ((moPubAdapter.isAd(moPubPosition - 1)
                                    && moPubAdapter.isAd(moPubPosition + 1))) {
                        clearAdjacentAds = true
                        moPubAdapter.refreshAds(AD_UNIT_ID, RequestParameters.Builder().keywords(MOPUB_KEYWORDS).build())
                    }
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    private fun initViewEffects() {
        feedViewModel.effect.signIn.observe(viewLifecycleOwner) {
            SignInDialogFragment().newInstance(Bundle().apply {
                putString(SIGNIN_TYPE_KEY, DIALOG.name)
            }).show(parentFragmentManager, SIGNIN_DIALOG_FRAGMENT_TAG)
        }
        feedViewModel.effect.notifyItemChanged.observe(viewLifecycleOwner) {
            adapter.notifyItemChanged(it.position)
        }
        feedViewModel.effect.enableSwipeToRefresh.observe(viewLifecycleOwner) {
            homeViewModel.enableSwipeToRefresh(it.isEnabled)
        }
        feedViewModel.effect.swipeToRefresh.observe(viewLifecycleOwner) {
            homeViewModel.setSwipeToRefreshState(it.isEnabled)
        }
        feedViewModel.effect.contentSwiped.observe(viewLifecycleOwner) {
            val contentSwipedPosition = getAdapterPosition(it.position)
            if (contentSwipedPosition != ERROR) {
                val contentSwiped = adapter.getContent(contentSwipedPosition)
                val user = FirebaseAuth.getInstance().currentUser
                if (contentSwiped !== null && user !== null) {
                    viewEvent.contentLabeled(ContentLabeled(
                            feedType = feedType,
                            actionType = it.actionType,
                            user = user,
                            position = contentSwipedPosition,
                            content = adapter.getContent(contentSwipedPosition),
                            isMainFeedEmptied = if (feedType == MAIN) adapter.itemCount == 1 else false))
                }
            }
        }
        feedViewModel.effect.snackBar.observe(viewLifecycleOwner) {
            when (feedType) {
                MAIN -> snackbarWithText(resources, it.text, this.requireParentFragment().requireView())
                SAVED, DISMISSED -> snackbarWithText(resources, it.text, binding.contentFragment)
            }
        }
        feedViewModel.effect.shareContentIntent.observe(viewLifecycleOwner) {
            it.contentRequest.observe(viewLifecycleOwner) { content ->
                val intent = createChooser(Intent(ACTION_SEND).apply {
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
                }, CONTENT_SHARE_DIALOG_TITLE)
                intent.resolveActivity(requireContext().packageManager)?.let {
                    startActivity(intent)
                }
            }
        }
        feedViewModel.effect.openContentSourceIntent.observe(viewLifecycleOwner) {
            val intent = Intent(ACTION_VIEW).setData(Uri.parse(it.url))
            intent.resolveActivity(requireContext().packageManager)?.let {
                startActivity(intent)
            }
        }
        feedViewModel.effect.screenEmpty.observe(viewLifecycleOwner) {
            val emptyFeedView = binding.emptyFeedLayout.emptyFeedView
            if (!it.isEmpty)
                emptyFeedView.visibility = GONE
            else {
                if (emptyFeedView.visibility == GONE) {
                    val fadeIn = AnimationUtils.loadAnimation(context, fade_in)
                    emptyFeedView.startAnimation(fadeIn)
                    fadeIn.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationRepeat(animation: Animation?) {/*Do something.*/
                        }

                        override fun onAnimationEnd(animation: Animation?) {
                            emptyFeedView.visibility = VISIBLE
                            binding.contentRecyclerView.visibility = VISIBLE
                        }

                        override fun onAnimationStart(animation: Animation?) {
                            binding.contentRecyclerView.visibility = INVISIBLE
                        }
                    })
                }
                binding.emptyFeedLayout.confirmation.setOnClickListener { view: View ->
                    if (feedType == SAVED && homeViewModel.bottomSheetState.value == STATE_EXPANDED) {
                        //TODO: Add to HomeViewModel ViewState.
                        homeViewModel.setBottomSheetState(STATE_COLLAPSED)
                    } else if (feedType == DISMISSED) view.findNavController().navigateUp()
                }
                when (feedType) {
                    MAIN -> {
                        binding.emptyFeedLayout.shootingStarOne.visibility = VISIBLE
                        binding.emptyFeedLayout.earth.visibility = VISIBLE
                        binding.emptyFeedLayout.title.text = getString(no_content_title)
                        binding.emptyFeedLayout.emptyInstructions.text = getString(no_feed_content_instructions)
                    }
                    SAVED -> {

                        binding.emptyFeedLayout.shootingStarOne.visibility = GONE
                        binding.emptyFeedLayout.earth.visibility = GONE
                        binding.emptyFeedLayout.emptyImage.setImageDrawable(getDrawable(requireContext(),
                                ic_coinverse_48dp))
                        binding.emptyFeedLayout.title.text = getString(no_saved_content_title)
                        binding.emptyFeedLayout.swipeRightOne.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_right_color_accent_24dp))
                        binding.emptyFeedLayout.swipeRightTwo.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_right_color_accent_fade_one_24dp))
                        binding.emptyFeedLayout.swipeRightThree.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_right_color_accent_fade_two_24dp))
                        binding.emptyFeedLayout.swipeRightFour.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_right_color_accent_fade_three_24dp))
                        binding.emptyFeedLayout.swipeRightFive.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_right_color_accent_fade_four_24dp))
                        binding.emptyFeedLayout.emptyInstructions.text = getString(no_saved_content_instructions)
                        binding.emptyFeedLayout.confirmation.visibility = VISIBLE
                    }
                    DISMISSED -> {
                        binding.emptyFeedLayout.shootingStarOne.visibility = GONE
                        binding.emptyFeedLayout.earth.visibility = GONE
                        binding.emptyFeedLayout.emptyImage.setImageDrawable(getDrawable(requireContext(),
                                ic_dismiss_planet_light_48dp))
                        binding.emptyFeedLayout.title.text = getString(no_dismissed_content_title)
                        binding.emptyFeedLayout.swipeRightOne.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_left_color_accent_24dp))
                        binding.emptyFeedLayout.swipeRightTwo.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_left_color_accent_fade_one_24dp))
                        binding.emptyFeedLayout.swipeRightThree.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_left_color_accent_fade_two_24dp))
                        binding.emptyFeedLayout.swipeRightFour.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_left_color_accent_fade_three_24dp))
                        binding.emptyFeedLayout.swipeRightFive.setImageDrawable(getDrawable(requireContext(),
                                ic_chevron_left_color_accent_fade_four_24dp))
                        binding.emptyFeedLayout.emptyInstructions.text = getString(no_dismissed_content_instructions)
                        binding.emptyFeedLayout.confirmation.visibility = VISIBLE
                    }
                }
            }
        }
        feedViewModel.effect.updateAds.observe(viewLifecycleOwner) {
            if (homeViewModel.accountType.value == FREE) {
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
    }

    private fun setToolbar(toolbarState: ToolbarState) {
        binding.appbar.appBarLayout.visibility = feedViewModel.state.toolbarState.visibility
        binding.appbar.titleToolbar.text = context?.getString(feedViewModel.state.toolbarState.titleRes)
        if (toolbarState.isActionBarEnabled) {
            binding.appbar.toolbar.title = ""
            (activity as AppCompatActivity).setSupportActionBar(binding.appbar.toolbar)
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
                viewEvent.contentSelected(ContentSelected(it.content, it.position))
                binding.contentRecyclerView.layoutManager?.scrollToPosition(it.position)
                openContentFromNotification = false
            }
    }
}