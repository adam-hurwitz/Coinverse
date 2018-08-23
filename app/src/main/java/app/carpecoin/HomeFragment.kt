package app.carpecoin

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import app.carpecoin.coin.R
import app.carpecoin.coin.databinding.FragmentHomeBinding
import app.carpecoin.priceGraph.PriceGraphFragment
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import android.content.Intent
import androidx.navigation.Navigation
import app.carpecoin.contentFeed.ContentFeedFragment
import app.carpecoin.utils.Constants.RC_SIGN_IN
import app.carpecoin.utils.setProfileImageUrl
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.fragment_home.*

private const val PRICEGRAPH_FRAGMENT_TAG = "priceGraphFragmentTag"
private const val CONTENTFEED_FRAGMENT_TAG = "contentFeedFragmentTag"

class HomeFragment : Fragment() {

    private var user: FirebaseUser? = null

    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(HomeViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(this)
        binding.viewmodel = viewModel
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null
                && childFragmentManager.findFragmentByTag(PRICEGRAPH_FRAGMENT_TAG) == null
                && childFragmentManager.findFragmentByTag(CONTENTFEED_FRAGMENT_TAG) == null) {
            childFragmentManager.beginTransaction()
                    .replace(priceDataContainer.id, PriceGraphFragment.newInstance(),
                            PRICEGRAPH_FRAGMENT_TAG)
                    .commit()
            childFragmentManager.beginTransaction()
                    .replace(contentFeedContainer.id, ContentFeedFragment.newInstance(),
                            CONTENTFEED_FRAGMENT_TAG)
                    .commit()
        }
        user = viewModel.getCurrentUser()
        setProfileButton(user != null)
        observeDataRealtimeStatus()
        observeDisableSwipeToRefresh()
        observeProfileButton()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                user = viewModel.getCurrentUser()
                setProfileButton(user != null)
                println(String.format("requestCode:%s resultCode:%s user:%s",
                        requestCode, resultCode, user?.displayName))
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                println(String.format("sign_in fail:%s", response?.error?.errorCode))
            }
        }
    }

    //TODO: Fix swipe to refresh and CollapasingToolBar overlap.
    fun observeDataRealtimeStatus() {
        viewModel.isRealtimeDataEnabled.observe(viewLifecycleOwner, Observer { isRealtimeDataEnabled: Boolean ->
            if (isRealtimeDataEnabled) {
                swipeToRefresh.isRefreshing = false
                swipeToRefresh.isEnabled = false
            } else {
                swipeToRefresh.setOnRefreshListener {
                    (fragmentManager?.findFragmentById(R.id.priceDataContainer) as PriceGraphFragment)
                            .initializeData()
                    //TODO: Decide 1 or 2 SwipeToRefresh for screen.
                    /*(fragmentManager?.findFragmentById(R.id.contentFeedContainer) as ContentFeedFragment)
                            .initializeData()*/
                }
            }
        })
    }

    fun observeDisableSwipeToRefresh() {
        viewModel.disableSwipeToRefresh.observe(viewLifecycleOwner, Observer { disableSwipeToRefresh: Boolean ->
            swipeToRefresh.isRefreshing = false
        })
    }

    private fun observeProfileButton() {
        viewModel.profileButtonClick.observe(viewLifecycleOwner, Observer { isClicked ->
            if (user == null) {
                this.startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(listOf(AuthUI.IdpConfig.GoogleBuilder().build()))
                                .build(),
                        RC_SIGN_IN)
            } else {
                val action =
                        HomeFragmentDirections.actionHomeFragmentToProfileFragment(user!!)
                action.setUser(user!!)
                profileButton.setOnClickListener(Navigation.createNavigateOnClickListener(
                        R.id.action_homeFragment_to_profileFragment, action.arguments))
            }
        })
    }

    private fun setProfileButton(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            Glide.with(this)
                    .load(user?.photoUrl.toString())
                    .apply(RequestOptions.circleCropTransform())
                    .into(profileButton)
        } else {
            profileButton.setImageResource(R.drawable.ic_profile_logged_in_24dp)
        }
    }

}
