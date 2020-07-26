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
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import app.coinverse.analytics.Analytics
import app.coinverse.content.ContentDialogFragment
import app.coinverse.databinding.FragmentFeedBinding
import app.coinverse.dependencyInjection.Component
import app.coinverse.feed.adapter.FeedAdapter
import app.coinverse.feed.adapter.initItemTouchHelper
import app.coinverse.feed.state.FeedView
import app.coinverse.feed.state.FeedViewIntent
import app.coinverse.feed.state.FeedViewIntentType.*
import app.coinverse.feed.state.FeedViewState
import app.coinverse.feed.state.FeedViewState.ClearAdjacentAds
import app.coinverse.feed.state.FeedViewState.Feed
import app.coinverse.feed.state.FeedViewState.OpenContent
import app.coinverse.feed.state.FeedViewState.OpenContentSource
import app.coinverse.feed.state.FeedViewState.ShareContent
import app.coinverse.feed.state.FeedViewState.SignIn
import app.coinverse.feed.state.FeedViewState.SwipeContent
import app.coinverse.feed.state.FeedViewState.SwipeToRefresh
import app.coinverse.feed.state.FeedViewState.UpdateAds
import app.coinverse.home.HomeViewModel
import app.coinverse.user.SignInDialogFragment
import app.coinverse.utils.AD_UNIT_ID
import app.coinverse.utils.AUDIOCAST_SHARE_MESSAGE
import app.coinverse.utils.CONTENT_DIALOG_FRAGMENT_TAG
import app.coinverse.utils.CONTENT_SHARE_DIALOG_TITLE
import app.coinverse.utils.CONTENT_SHARE_SUBJECT_PREFFIX
import app.coinverse.utils.CONTENT_SHARE_TYPE
import app.coinverse.utils.CONTENT_TO_PLAY_KEY
import app.coinverse.utils.ContentType.NONE
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.ERROR
import app.coinverse.utils.Event
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
import app.coinverse.utils.SignInType
import app.coinverse.utils.ToolbarState
import app.coinverse.utils.VIDEO_SHARE_MESSAGE
import app.coinverse.utils.snackbarWithText
import app.topcafes.dependencyinjection.fragmentSavedStateViewModels
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
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

@ExperimentalCoroutinesApi
class FeedFragment : Fragment(), FeedView {
    private val LOG_TAG = FeedFragment::class.java.simpleName

    @Inject
    lateinit var analytics: Analytics
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val intent = FeedViewIntent()
    private var clearAdjacentAds = false
    private var isOpenFromNotification = false
    private var openContentFromNotification: OpenContent? = null

    private lateinit var component: Component
    private lateinit var feedViewModel: FeedViewModel
    private lateinit var feedType: FeedType
    private lateinit var binding: FragmentFeedBinding
    private lateinit var adapter: FeedAdapter
    private lateinit var moPubAdapter: MoPubRecyclerAdapter

    companion object {
        @JvmStatic
        fun newInstance(contentBundle: Bundle) = FeedFragment().apply {
            arguments = contentBundle
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (homeViewModel.accountType.value == FREE)
            intent.updateAds.value = true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        component = (context.applicationContext as App).component
        component.inject(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        feedType = getFeedType()
        analytics.setCurrentScreen(requireActivity(), feedType.name)
        val feedViewModel: FeedViewModel by fragmentSavedStateViewModels { handle ->
            component.feedViewModelFactory().create(
                    feedType = feedType,
                    timeframe = homeViewModel.timeframe.value!!,
                    isRealtime = homeViewModel.isRealtime.value!!
            )
        }
        this.feedViewModel = feedViewModel
        binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapters()
        feedViewModel.bindIntents(this)
        if (savedInstanceState == null) intent.loadFromNetwork.value = true
    }

    override fun onDestroy() {
        if (homeViewModel.accountType.value == FREE) moPubAdapter.destroy()
        super.onDestroy()
    }

    override fun initState() = intent.initState
    override fun loadFromNetwork() = intent.loadFromNetwork.filterNotNull()
    override fun swipeToRefresh() = intent.swipeToRefresh.filterNotNull()
    override fun selectContent() = intent.selectContent.filterNotNull()
    override fun swipeContent() = intent.swipeContent.filterNotNull()
    override fun labelContent() = intent.labelContent.filterNotNull()
    override fun shareContent() = intent.shareContent.filterNotNull()
    override fun openContentSource() = intent.openContentSource.filterNotNull()
    override fun updateAds() = intent.updateAds.filterNotNull()
    override fun render(state: FeedViewState) {
        when (state) {
            is Feed -> renderFeed(state)
            is SwipeToRefresh -> renderSwipeToRefresh(state)
            is OpenContent -> renderOpenContent(state)
            is SignIn -> renderSignIn(state)
            is SwipeContent -> renderSwipeContent(state)
            is UpdateAds -> renderUpdateAds()
            is ClearAdjacentAds -> renderClearAdjacentAds(state)
            is ShareContent -> renderShareContent(state)
            is OpenContentSource -> renderOpenContentSource(state)
        }
    }

    fun initSwipeToRefresh() {
        intent.swipeToRefresh.value = SwipeToRefresh(
                feedType = feedType,
                timeframe = homeViewModel.timeframe.value!!,
                isRealtime = homeViewModel.isRealtime.value!!)
    }

    private fun renderFeed(state: Feed) {
        setToolbar(state.toolbarState)
        adapter.submitList(state.feed)
        setScreenEmpty(state.feed.isEmpty())
        state.error?.let { setSnackbar(state.error) }
        openFromNotification()
    }

    private fun renderSwipeToRefresh(state: SwipeToRefresh) {
        homeViewModel.setSwipeToRefreshState(state.isEnabled)
        if (state.isEnabled == false) homeViewModel.disableSwipeToRefresh()
        state.error?.let { setSnackbar(state.error) }
    }

    private fun renderOpenContent(state: OpenContent) {
        // Loading UI
        if (state.isLoading) adapter.loadingIds.add(state.content.id)
        else adapter.loadingIds.remove(state.content.id)
        // Notify item changed
        val position = getAdapterPosition(state.position)
        if (homeViewModel.accountType.value == FREE)
            moPubAdapter.notifyItemChanged(position)
        else adapter.notifyItemChanged(position)

        if (state.content.contentType != NONE) {
            when (feedType) {
                MAIN, DISMISSED ->
                    if (childFragmentManager.findFragmentByTag(CONTENT_DIALOG_FRAGMENT_TAG) == null)
                        ContentDialogFragment.newInstance(Bundle().apply {
                            putParcelable(CONTENT_TO_PLAY_KEY, state)
                        }).show(childFragmentManager, CONTENT_DIALOG_FRAGMENT_TAG)
                // Launches content from saved bottom sheet screen via HomeFragment.
                SAVED -> homeViewModel.setOpenFromSave(state)
            }
        }
        state.error?.let { setSnackbar(state.error) }
    }

    private fun renderSignIn(state: SignIn) {
        val position = getAdapterPosition(state.position)
        if (homeViewModel.accountType.value == FREE)
            moPubAdapter.notifyItemChanged(position)
        else adapter.notifyItemChanged(position)
        SignInDialogFragment.newInstance(Bundle().apply {
            putString(SIGNIN_TYPE_KEY, SignInType.DIALOG.name)
        }).show(parentFragmentManager, SIGNIN_DIALOG_FRAGMENT_TAG)
    }

    private fun renderSwipeContent(state: SwipeContent) {
        val position = getAdapterPosition(state.position)
        if (position != ERROR) {
            val swipeContent = adapter.getContent(position)
            val user = FirebaseAuth.getInstance().currentUser
            if (swipeContent !== null && user !== null) {
                intent.labelContent.value = LabelContent(
                        feedType = feedType,
                        actionType = state.actionType,
                        user = user,
                        position = position,
                        content = adapter.getContent(position),
                        isMainFeedEmptied = if (feedType == MAIN) adapter.itemCount == 1 else false
                )
            }
        }
    }

    private fun renderUpdateAds() {
        if (homeViewModel.accountType.value == FREE) {
            moPubAdapter.loadAds(AD_UNIT_ID, RequestParameters.Builder().keywords(MOPUB_KEYWORDS).build())
            moPubAdapter.setAdLoadedListener(object : MoPubNativeAdLoadedListener {
                override fun onAdRemoved(position: Int) {}
                override fun onAdLoaded(position: Int) {
                    if (moPubAdapter.isAd(position + 1) || moPubAdapter.isAd(position - 1))
                        moPubAdapter.refreshAds(AD_UNIT_ID, RequestParameters.Builder().keywords(MOPUB_KEYWORDS).build())
                    if (clearAdjacentAds) {
                        clearAdjacentAds = false
                        if (homeViewModel.accountType.value == FREE)
                            moPubAdapter.notifyDataSetChanged()
                        else adapter.notifyDataSetChanged()
                    }
                }
            })
        }
    }

    private fun renderClearAdjacentAds(state: ClearAdjacentAds) {
        if (homeViewModel.accountType.value == FREE && state.error == null) {
            val moPubPosition = moPubAdapter.getAdjustedPosition(state.position)
            if ((moPubAdapter.isAd(moPubPosition - 1)
                            && moPubAdapter.isAd(moPubPosition + 1))) {
                clearAdjacentAds = true
                moPubAdapter.refreshAds(
                        AD_UNIT_ID,
                        RequestParameters.Builder().keywords(MOPUB_KEYWORDS).build()
                )
            }
        }
        state.error?.let { setSnackbar(state.error) }
    }

    private fun renderShareContent(state: ShareContent) {
        val action = Intent(ACTION_SEND)
        action.type = CONTENT_SHARE_TYPE
        action.putExtra(EXTRA_SUBJECT, CONTENT_SHARE_SUBJECT_PREFFIX + state.content?.title)
        action.putExtra(EXTRA_TEXT, buildShareString(state, action))
        val intent = createChooser(action, CONTENT_SHARE_DIALOG_TITLE)
        intent.resolveActivity(requireContext().packageManager)
        startActivity(intent)
    }

    private fun renderOpenContentSource(state: OpenContentSource) {
        val intent = Intent(ACTION_VIEW)
        intent.data = Uri.parse(state.url)
        intent.resolveActivity(requireContext().packageManager)
        startActivity(intent)
    }

    private fun getFeedType() =
            (requireArguments().getParcelable<OpenContent>(OPEN_CONTENT_FROM_NOTIFICATION_KEY)).let {
                if (it == null) FeedType.valueOf(FeedFragmentArgs.fromBundle(requireArguments()).feedType)
                else {
                    isOpenFromNotification = true
                    openContentFromNotification = it
                    it.content.feedType
                }
            }

    private fun setToolbar(toolbarState: ToolbarState) {
        binding.appbar.appBarLayout.visibility = toolbarState.visibility
        binding.appbar.titleToolbar.text = context?.getString(toolbarState.titleRes)
        if (toolbarState.isActionBarEnabled) {
            binding.appbar.toolbar.title = ""
            (activity as AppCompatActivity).setSupportActionBar(binding.appbar.toolbar)
            (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }

    @ExperimentalCoroutinesApi
    private fun initAdapters() {
        val paymentStatus = homeViewModel.accountType.value
        binding.contentRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.contentRecyclerView.setHasFixedSize(true)
        adapter = FeedAdapter(intent)
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
                swipeContent = intent.swipeContent
        ).attachToRecyclerView(binding.contentRecyclerView)
    }

    private fun getAdapterPosition(position: Int) =
            if (homeViewModel.accountType.value!! == FREE) moPubAdapter.getOriginalPosition(position)
            else position

    private fun openFromNotification() {
        if (isOpenFromNotification) {
            openContentFromNotification?.let {
                intent.selectContent.value = Event(SelectContent(it.content, it.position))
                binding.contentRecyclerView.layoutManager?.scrollToPosition(it.position)
                isOpenFromNotification = false
            }
        }
    }

    private fun setSnackbar(error: String) {
        when (feedType) {
            MAIN -> snackbarWithText(resources, error, this.requireParentFragment().requireView())
            SAVED, DISMISSED -> snackbarWithText(resources, error, binding.contentFragment)
        }
    }

    fun buildShareString(state: ShareContent, action: Intent): String? {
        val prefix = "$SHARED_VIA_COINVERSE '${state.content?.title}' - ${state.content?.creator}"
        val postfix = state.content?.audioUrl.let { audioUrl ->
            if (!audioUrl.isNullOrBlank()) {
                action.putExtra(EXTRA_STREAM, Uri.parse(state.content?.previewImage))
                action.type = SHARE_CONTENT_IMAGE_TYPE
                action.addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                "$AUDIOCAST_SHARE_MESSAGE $audioUrl"
            } else
                if (state.content?.contentType == YOUTUBE)
                    VIDEO_SHARE_MESSAGE + state.content.url
                else SOURCE_SHARE_MESSAGE + state.content?.url
        }
        return prefix + postfix
    }

    private fun setScreenEmpty(isEmpty: Boolean) {
        val emptyFeedView = binding.emptyFeedLayout.emptyFeedView
        if (!isEmpty) emptyFeedView.visibility = GONE
        else {
            if (emptyFeedView.visibility == GONE) {
                val fadeIn = AnimationUtils.loadAnimation(context, fade_in)
                emptyFeedView.startAnimation(fadeIn)
                fadeIn.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationRepeat(animation: Animation?) {}
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
                if (feedType == SAVED && homeViewModel.bottomSheetState.value == STATE_EXPANDED)
                    homeViewModel.setBottomSheetState(STATE_COLLAPSED)
                else if (feedType == DISMISSED) view.findNavController().navigateUp()
            }
            when (feedType) {
                MAIN -> {
                    binding.emptyFeedLayout.shootingStarOne.visibility = VISIBLE
                    binding.emptyFeedLayout.earth.visibility = VISIBLE
                    binding.emptyFeedLayout.title.text = getString(string.no_content_title)
                    binding.emptyFeedLayout.emptyInstructions.text = getString(string.no_feed_content_instructions)
                }
                SAVED -> {
                    binding.emptyFeedLayout.shootingStarOne.visibility = GONE
                    binding.emptyFeedLayout.earth.visibility = GONE
                    binding.emptyFeedLayout.emptyImage.setImageDrawable(
                            AppCompatResources.getDrawable(requireContext(), ic_coinverse_48dp)
                    )
                    binding.emptyFeedLayout.title.text = getString(string.no_saved_content_title)
                    binding.emptyFeedLayout.swipeRightOne.setImageDrawable(
                            AppCompatResources.getDrawable(requireContext(),
                                    ic_chevron_right_color_accent_24dp)
                    )
                    binding.emptyFeedLayout.swipeRightTwo.setImageDrawable(
                            AppCompatResources.getDrawable(requireContext(),
                                    ic_chevron_right_color_accent_fade_one_24dp)
                    )
                    binding.emptyFeedLayout.swipeRightThree.setImageDrawable(
                            AppCompatResources.getDrawable(requireContext(),
                                    ic_chevron_right_color_accent_fade_two_24dp)
                    )
                    binding.emptyFeedLayout.swipeRightFour.setImageDrawable(
                            AppCompatResources.getDrawable(requireContext(),
                                    ic_chevron_right_color_accent_fade_three_24dp)
                    )
                    binding.emptyFeedLayout.swipeRightFive.setImageDrawable(
                            AppCompatResources.getDrawable(requireContext(),
                                    ic_chevron_right_color_accent_fade_four_24dp)
                    )
                    binding.emptyFeedLayout.emptyInstructions.text = getString(string.no_saved_content_instructions)
                    binding.emptyFeedLayout.confirmation.visibility = VISIBLE
                }
                DISMISSED -> {
                    binding.emptyFeedLayout.shootingStarOne.visibility = GONE
                    binding.emptyFeedLayout.earth.visibility = GONE
                    binding.emptyFeedLayout.emptyImage.setImageDrawable(
                            AppCompatResources.getDrawable(requireContext(),
                                    ic_dismiss_planet_light_48dp))
                    binding.emptyFeedLayout.title.text = getString(string.no_dismissed_content_title)
                    binding.emptyFeedLayout.swipeRightOne.setImageDrawable(
                            AppCompatResources.getDrawable(requireContext(),
                                    ic_chevron_left_color_accent_24dp)
                    )
                    binding.emptyFeedLayout.swipeRightTwo.setImageDrawable(
                            AppCompatResources.getDrawable(requireContext(),
                                    ic_chevron_left_color_accent_fade_one_24dp))
                    binding.emptyFeedLayout.swipeRightThree.setImageDrawable(
                            AppCompatResources.getDrawable(requireContext(),
                                    ic_chevron_left_color_accent_fade_two_24dp)
                    )
                    binding.emptyFeedLayout.swipeRightFour.setImageDrawable(
                            AppCompatResources.getDrawable(requireContext(),
                                    ic_chevron_left_color_accent_fade_three_24dp)
                    )
                    binding.emptyFeedLayout.swipeRightFive.setImageDrawable(
                            AppCompatResources.getDrawable(requireContext(),
                                    ic_chevron_left_color_accent_fade_four_24dp))
                    binding.emptyFeedLayout.emptyInstructions.text = getString(string.no_dismissed_content_instructions)
                    binding.emptyFeedLayout.confirmation.visibility = VISIBLE
                }
            }
        }
    }
}