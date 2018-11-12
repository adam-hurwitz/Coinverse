package app.coinverse.home

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import app.coinverse.BuildConfig.VERSION_NAME
import app.coinverse.Enums
import app.coinverse.Enums.FeedType.MAIN
import app.coinverse.Enums.FeedType.SAVED
import app.coinverse.R
import app.coinverse.content.ContentFragment
import app.coinverse.content.YouTubeDialogFragment
import app.coinverse.databinding.FragmentHomeBinding
import app.coinverse.firebase.FirestoreCollections.usersCollection
import app.coinverse.priceGraph.PriceFragment
import app.coinverse.user.SignInDialogFragment
import app.coinverse.user.models.User
import app.coinverse.utils.*
import app.coinverse.utils.DateAndTime.getTimeAgo
import app.coinverse.utils.livedata.EventObserver
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.fragment_home.*
import java.util.*
import kotlin.collections.ArrayList

private val LOG_TAG = HomeFragment::class.java.simpleName

class HomeFragment : Fragment() {

    private var user: FirebaseUser? = null
    private var isAppBarExpanded = false
    private var isSavedContentExpanded = false

    private lateinit var binding: FragmentHomeBinding
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(APP_BAR_EXPANDED_KEY, isAppBarExpanded)
        outState.putBoolean(SAVED_CONTENT_EXPANDED_KEY, isSavedContentExpanded)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(APP_BAR_EXPANDED_KEY))
                appBar.setExpanded(true)
            else appBar.setExpanded(false)
            if (savedInstanceState.getBoolean(SAVED_CONTENT_EXPANDED_KEY)) {
                swipeToRefresh.isEnabled = false
                bottomSheetBehavior.state = STATE_EXPANDED
                setBottomSheetExpanded()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeViewModel = ViewModelProviders.of(activity!!).get(HomeViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(this)
        binding.viewmodel = homeViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        user = homeViewModel.getCurrentUser()
        initProfileButton(user != null)
        initCollapsingToolbarStates()
        initMessageCenter()
        initSavedBottomSheet()
        setClickListeners()
        observeSignIn()
        initSwipeToRefresh()
        observeSavedContentSelected()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null
                && childFragmentManager.findFragmentByTag(PRICEGRAPH_FRAGMENT_TAG) == null
                && childFragmentManager.findFragmentByTag(CONTENT_FEED_FRAGMENT_TAG) == null) {
            childFragmentManager.beginTransaction()
                    .replace(priceContainer.id, PriceFragment.newInstance(), PRICEGRAPH_FRAGMENT_TAG)
                    .commit()
            val contentBundle = Bundle()
            contentBundle.putString(FEED_TYPE_KEY, MAIN.name)
            childFragmentManager.beginTransaction()
                    .replace(contentContainer.id, ContentFragment.newInstance(contentBundle),
                            CONTENT_FEED_FRAGMENT_TAG)
                    .commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                user = homeViewModel.getCurrentUser()
                initProfileButton(user != null)
            } else println(String.format("sign_in fail:%s", response?.error?.errorCode))
        }
    }

    private fun initMessageCenter() {
        //TODO: Move to ViewModel.
        val messagesList = ArrayList<MessageCenterUpdate>()
        homeViewModel.syncMessageCenterUpdates()
        homeViewModel.messageCenterLiveData.observe(viewLifecycleOwner, Observer { messages ->
            messages.all { message ->
                if (!messagesList.contains(message)) messagesList.add(message)
                true
            }
        })
        var unreadCount = 0.0
        homeViewModel.messageCenterUnreadCountLiveData.observe(this, Observer { count ->
            unreadCount = count
            if (count > 0) messageCenterButton.setImageResource(R.drawable.ic_message_center_unread)
            else messageCenterButton.setImageResource(R.drawable.ic_message_center_read)
        })
        messageCenterButton.setOnClickListener { view ->
            val menu = PopupMenu(context!!, view)
            val header = SpannableString(getString(R.string.message_center))
            header.setSpan(StyleSpan(Typeface.BOLD), 0, header.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            menu.menu.add(Menu.NONE, 0, 0, header)
            for (i in 0..messagesList.size - 1) {
                val message = messagesList[i]
                val id = i + 1
                if (VERSION_NAME.equals(message.versionName)) {
                    val date = message.timestamp
                    val timeAgo = getTimeAgo(context!!, date.time, true)
                    menu.menu.add(Menu.NONE, id, id, String.format("%s (%s)", message.message, timeAgo))
                    if (unreadCount == 0.0) menu.menu.getItem(id).setEnabled(false)
                }
            }
            menu.show()
            homeViewModel.clearUnreadMessageCenterCount()
        }
    }

    private fun initCollapsingToolbarStates() {
        appBar.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                // appBar expanded.
                if (Math.abs(verticalOffset) - appBarLayout.totalScrollRange != 0) {
                    isAppBarExpanded = true
                    swipeToRefresh.isEnabled = true
                    bottomSheetBehavior.isHideable = true
                    bottomSheetBehavior.state = STATE_HIDDEN
                } else { // appBar collapsed.
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
        if (isLoggedIn) {
            Glide.with(this).load(user?.photoUrl.toString())
                    .apply(RequestOptions.circleCropTransform())
                    .into(profileButton)
        } else {
            profileButton.setImageResource(R.drawable.ic_astronaut_color_accent_24dp)
        }
    }

    private fun initSavedBottomSheet() {
        bottomSheetBehavior = from(bottomSheet)
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.peekHeight = SAVED_BOTTOM_SHEET_PEEK_HEIGHT
        bottomSheet.layoutParams.height = getDisplayHeight(context!!)
        if (homeViewModel.user.value != null) initSavedContentFragment()
        else {
            val bundle = Bundle()
            bundle.putInt(SIGNIN_TYPE_KEY, Enums.SignInType.FULLSCREEN.code)
            fragmentManager!!.beginTransaction()
                    .replace(R.id.savedContentContainer, SignInDialogFragment.newInstance(bundle)).commit()
        }
        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == STATE_EXPANDED) {
                    homeViewModel.bottomSheetState.value = STATE_EXPANDED
                    setBottomSheetExpanded()
                }
                if (newState == STATE_COLLAPSED) {
                    isSavedContentExpanded = false
                    appBar.visibility = VISIBLE
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
        appBar.visibility = GONE
        bottom_handle.visibility = GONE
        bottom_handle_elevation.visibility = GONE
    }

    fun observeBottomSheetBackPressed() {
        homeViewModel.bottomSheetState.observe(viewLifecycleOwner, Observer { bottomSheetState ->
            if (bottomSheetState == STATE_COLLAPSED) bottomSheetBehavior.state = STATE_COLLAPSED
        })
    }

    private fun initSavedContentFragment() {
        val bundle = Bundle()
        bundle.putString(FEED_TYPE_KEY, SAVED.name)
        fragmentManager?.beginTransaction()?.replace(
                savedContentContainer.id, ContentFragment.newInstance(bundle), SAVED_CONTENT_TAG)
                ?.commit()
    }

    private fun setClickListeners() {
        profileButton.setOnClickListener { view: View ->
            if (user != null) {
                val action =
                        HomeFragmentDirections.actionHomeFragmentToProfileFragment(user!!)
                action.setUser(user!!)
                view.findNavController().navigate(R.id.action_homeFragment_to_profileFragment, action.arguments)
            } else {
                val bundle = Bundle()
                bundle.putInt(SIGNIN_TYPE_KEY, Enums.SignInType.DIALOG.ordinal)
                SignInDialogFragment.newInstance(bundle).show(fragmentManager, SIGNIN_DIALOG_FRAGMENT_TAG)
            }
        }
    }

    private fun observeSignIn() {
        homeViewModel.user.observe(this, Observer { user: FirebaseUser? ->
            this.user = user
            initProfileButton(user != null)
            if (user != null) {
                //TODO: Replace with Firestore security rule.
                usersCollection.document(user.uid).get().addOnCompleteListener { userQuery ->
                    if (!userQuery.result!!.exists()) {
                        usersCollection.document(user.uid).set(
                                User(user.uid, user.displayName, user.email, user.phoneNumber,
                                        user.photoUrl.toString(),
                                        Date(user.metadata!!.creationTimestamp),
                                        Date(user.metadata!!.lastSignInTimestamp), user.providerId,
                                        0.0, 0.0, 0.0,
                                        0.0, 0.0, 0.0,
                                        0.0, 0.0, 0.0))
                                .addOnSuccessListener {
                                    Log.v(LOG_TAG, String.format("New user added success:%s", it))
                                }.addOnFailureListener {
                                    Log.v(LOG_TAG, String.format("New user added failure:%s", it))
                                }
                    }
                }
                initSavedContentFragment()
            }
        })
    }

    fun initSwipeToRefresh() {
        homeViewModel.isSwipeToRefreshEnabled.observe(viewLifecycleOwner, Observer { isEnabled: Boolean ->
            swipeToRefresh.isEnabled = isEnabled
        })
        homeViewModel.isRefreshing.observe(viewLifecycleOwner, Observer { isRefreshing: Boolean ->
            swipeToRefresh.isRefreshing = isRefreshing
        })
        swipeToRefresh.setOnRefreshListener {
            (childFragmentManager.findFragmentById(R.id.priceContainer) as PriceFragment)
                    .getPrices(false, false)
            (childFragmentManager.findFragmentById(R.id.contentContainer) as ContentFragment)
                    .initializeMainContent(false)
        }
    }

    fun observeSavedContentSelected() {
        homeViewModel.savedContentSelected.observe(viewLifecycleOwner, EventObserver { content ->
            val youtubeBundle = Bundle().apply { putParcelable(CONTENT_KEY, content) }
            YouTubeDialogFragment().newInstance(youtubeBundle).show(childFragmentManager,
                    YOUTUBE_DIALOG_FRAGMENT_TAG)
        })
    }
}