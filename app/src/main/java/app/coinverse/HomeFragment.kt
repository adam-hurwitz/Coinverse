package app.coinverse

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import app.coinverse.Enums.FeedType.MAIN
import app.coinverse.Enums.FeedType.SAVED
import app.coinverse.content.ContentFragment
import app.coinverse.databinding.FragmentHomeBinding
import app.coinverse.firebase.FirestoreCollections.usersCollection
import app.coinverse.priceGraph.PriceFragment
import app.coinverse.user.SignInDialogFragment
import app.coinverse.user.models.User
import app.coinverse.utils.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.fragment_home.*
import java.util.*




private val LOG_TAG = HomeFragment::class.java.simpleName

class HomeFragment : Fragment() {

    private var user: FirebaseUser? = null

    private lateinit var binding: FragmentHomeBinding
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

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
        initSavedBottomSheet()
        initCollapsingToolbarStates()
        setClickListeners()
        observeSignIn()
        initSwipeToRefresh()
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


    private fun initCollapsingToolbarStates() {
        appBar.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                // appBar collapsed.
                if (Math.abs(verticalOffset) - appBarLayout.totalScrollRange == 0) {
                    swipeToRefresh.isEnabled = false
                    if (bottomSheetBehavior.state == STATE_HIDDEN) {
                        bottomSheetBehavior.isHideable = false
                        bottomSheetBehavior.state = STATE_COLLAPSED
                    }
                } else { // appBar expanded.
                    swipeToRefresh.isEnabled = true
                    bottomSheetBehavior.isHideable = true
                    bottomSheetBehavior.state = STATE_HIDDEN
                }
            }
        })
    }

    private fun initProfileButton(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            Glide.with(this)
                    .load(user?.photoUrl.toString())
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
        else fragmentManager!!.beginTransaction()
                .replace(R.id.savedContentContainer, SignInDialogFragment.newInstance()).commit()
        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == STATE_EXPANDED) {
                    homeViewModel.bottomSheetState.value = STATE_EXPANDED
                    appBar.visibility = View.GONE
                }

                if (newState == STATE_EXPANDED) {
                    bottom_handle.visibility = GONE
                    bottom_handle_elevation.visibility = GONE
                }

                if (newState == STATE_COLLAPSED) {
                    appBar.visibility = View.VISIBLE
                    bottom_handle.visibility = VISIBLE
                    bottom_handle_elevation.visibility = VISIBLE
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        observeBottomSheetBackPressed()
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
            } else
                SignInDialogFragment.newInstance().show(fragmentManager, SIGNIN_DIALOG_FRAGMENT_TAG)
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
                                        0.0, 0.0))
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

}
