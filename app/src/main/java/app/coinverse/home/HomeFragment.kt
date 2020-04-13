package app.coinverse.home

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.Intent.ACTION_SENDTO
import android.content.Intent.ACTION_VIEW
import android.content.Intent.EXTRA_EMAIL
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.EXTRA_TEXT
import android.net.Uri
import android.os.Build.BRAND
import android.os.Build.MODEL
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import app.coinverse.BuildConfig.VERSION_NAME
import app.coinverse.R
import app.coinverse.R.drawable.ic_astronaut_color_accent_24dp
import app.coinverse.R.string.error_sign_in_anonymously
import app.coinverse.R.string.first_open
import app.coinverse.R.string.logged_out
import app.coinverse.R.string.mail_to
import app.coinverse.analytics.models.UserActionCount
import app.coinverse.content.views.ContentDialogFragment
import app.coinverse.databinding.FragmentHomeBinding
import app.coinverse.feed.FeedFragment
import app.coinverse.feed.models.ContentToPlay
import app.coinverse.firebase.ACCOUNT_DOCUMENT
import app.coinverse.firebase.ACTIONS_DOCUMENT
import app.coinverse.firebase.firebaseApp
import app.coinverse.firebase.usersDocument
import app.coinverse.home.HomeFragmentDirections.actionHomeFragmentToUserFragment
import app.coinverse.priceGraph.PriceFragment
import app.coinverse.user.PermissionsDialogFragment
import app.coinverse.user.SignInDialogFragment
import app.coinverse.user.models.User
import app.coinverse.utils.ABOUT_LINK
import app.coinverse.utils.APP_BAR_EXPANDED_KEY
import app.coinverse.utils.AccountType.READ
import app.coinverse.utils.CONTENT_DIALOG_FRAGMENT_TAG
import app.coinverse.utils.CONTENT_FEED_FRAGMENT_TAG
import app.coinverse.utils.CONTENT_TO_PLAY_KEY
import app.coinverse.utils.FEED_TYPE_KEY
import app.coinverse.utils.FeedType
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.FeedType.SAVED
import app.coinverse.utils.OPEN_CONTENT_FROM_NOTIFICATION_KEY
import app.coinverse.utils.PRICEGRAPH_FRAGMENT_TAG
import app.coinverse.utils.PRIVACY_POLICY_LINK
import app.coinverse.utils.PaymentStatus.FREE
import app.coinverse.utils.RC_SIGN_IN
import app.coinverse.utils.REQUEST_CODE_LOC_PERMISSION
import app.coinverse.utils.SAVED_BOTTOM_SHEET_PEEK_HEIGHT
import app.coinverse.utils.SAVED_CONTENT_EXPANDED_KEY
import app.coinverse.utils.SAVED_CONTENT_TAG
import app.coinverse.utils.SIGNIN_DIALOG_FRAGMENT_TAG
import app.coinverse.utils.SIGNIN_TYPE_KEY
import app.coinverse.utils.SUPPORT_ANDROID_API
import app.coinverse.utils.SUPPORT_BODY
import app.coinverse.utils.SUPPORT_DEVICE
import app.coinverse.utils.SUPPORT_EMAIL
import app.coinverse.utils.SUPPORT_ISSUE
import app.coinverse.utils.SUPPORT_SUBJECT
import app.coinverse.utils.SUPPORT_USER
import app.coinverse.utils.SUPPORT_VERSION
import app.coinverse.utils.SignInType.DIALOG
import app.coinverse.utils.SignInType.FULLSCREEN
import app.coinverse.utils.USER_KEY
import app.coinverse.utils.getDisplayHeight
import app.coinverse.utils.setImageUrlCircle
import app.coinverse.utils.snackbarWithText
import com.crashlytics.android.Crashlytics
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.toolbar_app.appBarLayout
import kotlinx.android.synthetic.main.toolbar_home.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

private val LOG_TAG = HomeFragment::class.java.simpleName

/**
 * TODO: Refactor
 *  1. Refactor with Unidirectional Data Flow. See [app.coinverse.feed.ContentFragment].
 *  https://medium.com/hackernoon/android-unidirectional-flow-with-livedata-bf24119e747
 *  2. Move Firebase calls to Repository.
 *  3. Move Firebase user calls to Cloud Functions.
 **/

class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels()

    private var user: FirebaseUser? = null
    private var isAppBarExpanded = false
    private var isSavedContentExpanded = false
    private var openFromNotificaitonFeedType: FeedType? = null

    private lateinit var binding: FragmentHomeBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(USER_KEY, user)
        outState.putBoolean(APP_BAR_EXPANDED_KEY, isAppBarExpanded)
        outState.putBoolean(SAVED_CONTENT_EXPANDED_KEY, isSavedContentExpanded)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(APP_BAR_EXPANDED_KEY)) appBarLayout.setExpanded(true)
            else appBarLayout.setExpanded(false)
            if (savedInstanceState.getBoolean(SAVED_CONTENT_EXPANDED_KEY)) {
                swipeToRefresh.isEnabled = false
                bottomSheetBehavior.state = STATE_EXPANDED
                setBottomSheetExpanded()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == RESULT_OK) {
                user = homeViewModel.getCurrentUser()
                initProfileButton(user != null && !user!!.isAnonymous)
            } else println(String.format("sign_in fail:%s", response?.error?.errorCode))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getParcelable<ContentToPlay>(OPEN_CONTENT_FROM_NOTIFICATION_KEY)?.let {
            openFromNotificaitonFeedType = it.content.feedType
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        user = homeViewModel.getCurrentUser()
        initProfileButton(user != null && !user!!.isAnonymous)
        initCollapsingToolbarStates()
        observeSignIn(savedInstanceState)
        initSavedBottomSheetContainer(savedInstanceState)
        setClickListeners()
        initSwipeToRefresh()
        observeSavedContentSelected()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null
                && childFragmentManager.findFragmentByTag(PRICEGRAPH_FRAGMENT_TAG) == null) {
            childFragmentManager.beginTransaction()
                    .replace(priceContainer.id, PriceFragment().newInstance(), PRICEGRAPH_FRAGMENT_TAG)
                    .commit()
        }
    }

    private fun initCollapsingToolbarStates() {
        appBarLayout.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                //Appbar expanded.
                if (Math.abs(verticalOffset) - appBarLayout.totalScrollRange != 0) {
                    isAppBarExpanded = true
                    swipeToRefresh.isEnabled = true
                    bottomSheetBehavior.isHideable = true
                    bottomSheetBehavior.state = STATE_HIDDEN
                } else { //Appbar collapsed.
                    isAppBarExpanded = false
                    swipeToRefresh.isEnabled = false
                    if (bottomSheetBehavior.state == STATE_HIDDEN) {
                        bottomSheetBehavior.isHideable = false
                        bottomSheetBehavior.state = STATE_COLLAPSED
                    }
                }
            }
        })
    }

    private fun initProfileButton(isLoggedIn: Boolean) {
        if (isLoggedIn) profileButton.setImageUrlCircle(requireContext(), user?.photoUrl.toString())
        else profileButton.setImageResource(ic_astronaut_color_accent_24dp)
    }

    private fun initSavedBottomSheetContainer(savedInstanceState: Bundle?) {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.peekHeight = SAVED_BOTTOM_SHEET_PEEK_HEIGHT
        bottomSheet.layoutParams.height = getDisplayHeight(requireContext())
        if (savedInstanceState == null
                && (homeViewModel.user.value == null || homeViewModel.user.value!!.isAnonymous))
            childFragmentManager.beginTransaction().replace(
                    R.id.savedContentContainer,
                    SignInDialogFragment().newInstance(Bundle().apply {
                        putString(SIGNIN_TYPE_KEY, FULLSCREEN.name)
                    })).commit()
        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == STATE_EXPANDED) {
                    homeViewModel.setBottomSheetState(STATE_EXPANDED)
                    setBottomSheetExpanded()
                }
                if (newState == STATE_COLLAPSED) {
                    isSavedContentExpanded = false
                    appBarLayout.visibility = VISIBLE
                    bottom_handle_logo.visibility = VISIBLE
                    bottom_handle.visibility = VISIBLE
                    bottom_handle_elevation.visibility = VISIBLE
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        observeBottomSheetBackPressed()
    }

    private fun setBottomSheetExpanded() {
        isSavedContentExpanded = true
        appBarLayout.visibility = GONE
        bottom_handle_logo.visibility = GONE
        bottom_handle.visibility = GONE
        bottom_handle_elevation.visibility = GONE
    }

    private fun observeBottomSheetBackPressed() {
        homeViewModel.bottomSheetState.observe(viewLifecycleOwner) { bottomSheetState ->
            if (bottomSheetState == STATE_COLLAPSED) bottomSheetBehavior.state = STATE_COLLAPSED
            if (bottomSheetState == STATE_HIDDEN) {
                bottomSheetBehavior.isHideable = true
                bottomSheetBehavior.state = STATE_HIDDEN
            }
        }
    }

    private fun setClickListeners() {
        appStatus.setOnClickListener { view: View ->
            val intent = Intent(ACTION_VIEW).setData(Uri.parse(ABOUT_LINK))
            intent.resolveActivity(requireContext().packageManager)?.let {
                startActivity(intent)
            }
        }
        profileButton.setOnClickListener { view: View ->
            if (user != null && !user!!.isAnonymous)
                view.findNavController().navigate(R.id.action_homeFragment_to_userFragment,
                        actionHomeFragmentToUserFragment(user!!).apply { user = user }.arguments)
            else
                SignInDialogFragment().newInstance(Bundle().apply {
                    putString(SIGNIN_TYPE_KEY, DIALOG.name)
                }).show(childFragmentManager, SIGNIN_DIALOG_FRAGMENT_TAG)
        }
        submitBugButton.setOnClickListener { view: View ->
            val intent = Intent(ACTION_SENDTO)
                    .putExtra(EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                    .putExtra(EXTRA_SUBJECT, "${SUPPORT_SUBJECT} $VERSION_NAME")
                    .putExtra(EXTRA_TEXT, "${SUPPORT_BODY} " +
                            "${SUPPORT_ISSUE}" +
                            "${SUPPORT_VERSION} $VERSION_NAME" +
                            "${SUPPORT_ANDROID_API} $SDK_INT" +
                            "${SUPPORT_DEVICE} ${BRAND.substring(0, 1).toUpperCase()
                                    + BRAND.substring(1)}, $MODEL" +
                            "${SUPPORT_USER +
                                    if (user != null && !user!!.isAnonymous) user!!.uid
                                    else getString(logged_out)}")
            intent.data = Uri.parse(getString(mail_to))
            intent.resolveActivity(requireActivity().packageManager)?.let {
                startActivity(intent)
            }
        }
        menuHomeButton.setOnClickListener { view: View ->
            PopupMenu(requireContext(), view).apply {
                this.menuInflater.inflate(R.menu.menu_home, this.menu)
                this.show()
                this.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.privacy_policy -> {
                            val intent = Intent(ACTION_VIEW).setData(Uri.parse(PRIVACY_POLICY_LINK))
                            intent.resolveActivity(requireActivity().packageManager)?.let {
                                startActivity(intent)
                            }
                            true
                        }
                        else -> false
                    }
                }
            }
        }
    }

    private fun observeSignIn(savedInstanceState: Bundle?) {
        homeViewModel.user.observe(viewLifecycleOwner) { user: FirebaseUser? ->
            this.user = user
            initProfileButton(user != null && !user.isAnonymous)
            lifecycleScope.launch {
                /** Signed in */
                if (user != null && !user.isAnonymous) {
                    Crashlytics.setUserIdentifier(user.uid)
                    val userSnapshot = usersDocument.collection(user.uid)
                            .document(ACCOUNT_DOCUMENT).get().await()
                    /** Create user if user one does not exist. */
                    if (!userSnapshot.exists()) {
                        usersDocument.collection(user.uid).document(ACCOUNT_DOCUMENT).set(
                                User(user.uid, user.displayName, user.email, user.phoneNumber,
                                        user.photoUrl.toString(),
                                        Timestamp(Date(user.metadata!!.creationTimestamp)),
                                        Timestamp(Date(user.metadata!!.lastSignInTimestamp)),
                                        user.providerId, FREE, READ))
                                .addOnSuccessListener {
                                    Log.v(LOG_TAG, String.format("New user account data success:%s", it))
                                }.addOnFailureListener {
                                    Log.v(LOG_TAG, String.format("New user account data failure:%s", it))
                                }
                        usersDocument.collection(user.uid).document(ACTIONS_DOCUMENT).set(
                                UserActionCount(0.0, 0.0, 0.0,
                                        0.0, 0.0, 0.0,
                                        0.0, 0.0)
                        ).addOnSuccessListener {
                            Crashlytics.setUserIdentifier(user.uid)
                            Log.v(LOG_TAG, String.format(
                                    "New user action data success:%s", it))
                        }.addOnFailureListener {
                            Log.v(LOG_TAG, String.format(
                                    "New user action data failure:%s", it))
                        }
                    }
                    if ((childFragmentManager.findFragmentByTag(CONTENT_FEED_FRAGMENT_TAG) == null
                                    && savedInstanceState == null)
                            || savedInstanceState?.getParcelable<FirebaseUser>(USER_KEY) == null) {
                        initMainFeedFragment()
                        initSavedContentFragment()
                    }
                }
                /** Signed out */
                else if (childFragmentManager.findFragmentByTag(CONTENT_FEED_FRAGMENT_TAG) == null &&
                        savedInstanceState == null) {
                    try {
                        FirebaseAuth.getInstance(firebaseApp(true)).signInAnonymously().await()
                        Crashlytics.log(Log.VERBOSE, LOG_TAG, "observeSignIn anonymous success")
                    } catch (exception: FirebaseAuthException) {
                        Crashlytics.log(Log.ERROR, LOG_TAG, "observeSignIn ${exception.localizedMessage}")
                        snackbarWithText(resources, getString(error_sign_in_anonymously), contentContainer)
                        makeText(context, "Authentication failed.", LENGTH_SHORT).show()
                    }
                    initMainFeedFragment()
                }
            }
        }
    }

    private fun initMainFeedFragment() {
        if (homeViewModel.accountType.value == FREE) getLocationPermissionCheck()
        childFragmentManager.beginTransaction().replace(
                contentContainer.id,
                FeedFragment().newInstance(Bundle().apply {
                    putString(FEED_TYPE_KEY, MAIN.name)
                    openFromNotificaitonFeedType?.let { if (it == MAIN) putAll(arguments) }
                }), CONTENT_FEED_FRAGMENT_TAG
        ).commit()
    }

    private fun initSavedContentFragment() {
        childFragmentManager.beginTransaction().replace(
                savedContentContainer.id,
                FeedFragment().newInstance(Bundle().apply {
                    putString(FEED_TYPE_KEY, SAVED.name)
                    if (openFromNotificaitonFeedType == SAVED) {
                        bottomSheetBehavior.state = STATE_EXPANDED
                        setBottomSheetExpanded()
                        swipeToRefresh.isEnabled = false
                        putAll(arguments)
                    }
                }),
                SAVED_CONTENT_TAG
        ).commit()
    }

    private fun getLocationPermissionCheck() {
        if (checkSelfPermission(requireActivity(), ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            val pref = activity?.getPreferences(MODE_PRIVATE) ?: return
            if (pref.getBoolean(getString(first_open), true)
                    || shouldShowRequestPermissionRationale(requireActivity(), ACCESS_COARSE_LOCATION)) {
                with(pref.edit()) {
                    putBoolean(getString(first_open), false)
                    apply()
                }
                PermissionsDialogFragment().newInstance().show(childFragmentManager, null)
                homeViewModel.showLocationPermission.observe(viewLifecycleOwner) { showPermission ->
                    if (showPermission) requestPermissions(arrayOf(ACCESS_COARSE_LOCATION), REQUEST_CODE_LOC_PERMISSION)
                }
            } else requestPermissions(arrayOf(ACCESS_COARSE_LOCATION), REQUEST_CODE_LOC_PERMISSION)
        }
    }

    private fun initSwipeToRefresh() {
        homeViewModel.isSwipeToRefreshEnabled.observe(viewLifecycleOwner) { isEnabled: Boolean ->
            swipeToRefresh.isEnabled = isEnabled
        }
        homeViewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing: Boolean ->
            swipeToRefresh.isRefreshing = isRefreshing
        }
        swipeToRefresh.setOnRefreshListener {
            if (homeViewModel.accountType.value == FREE) getLocationPermissionCheck()
            (childFragmentManager.findFragmentById(R.id.priceContainer) as PriceFragment)
                    .getPrices(homeViewModel.isRealtime.value!!, false)
            (childFragmentManager.findFragmentById(R.id.contentContainer) as FeedFragment)
                    .swipeToRefresh()
        }
    }

    private fun observeSavedContentSelected() {
        homeViewModel.savedContentToPlay.observe(viewLifecycleOwner) { contentToPlay ->
            if (childFragmentManager.findFragmentByTag(CONTENT_DIALOG_FRAGMENT_TAG) == null)
                ContentDialogFragment().newInstance(Bundle().apply {
                    putParcelable(CONTENT_TO_PLAY_KEY, contentToPlay)
                }).show(childFragmentManager, CONTENT_DIALOG_FRAGMENT_TAG)
        }
    }
}