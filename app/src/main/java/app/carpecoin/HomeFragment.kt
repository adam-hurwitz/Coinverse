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
import app.carpecoin.contentFeed.ContentFeedFragment
import app.carpecoin.priceGraph.PriceGraphFragment
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import android.content.Intent
import androidx.navigation.Navigation
import app.carpecoin.utils.Constants.RC_SIGN_IN
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment : Fragment() {

    private var user: FirebaseUser? = null

    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(this)
        viewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        binding.viewmodel = viewModel
        user = viewModel.getCurrentUser()
        if (savedInstanceState == null) {
            fragmentManager
                    ?.beginTransaction()
                    ?.add(binding.priceDataContainer.id, PriceGraphFragment.newInstance())
                    ?.commit()
            fragmentManager
                    ?.beginTransaction()
                    ?.add(binding.contentFeedContainer.id, ContentFeedFragment.newInstance())
                    ?.commit()
        }
        observeProfileButton()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setProfileButton(user != null)
        super.onViewCreated(view, savedInstanceState)
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
    fun setRefreshStatus(isRealTimeDataEnabled: Boolean) {
        if (isRealTimeDataEnabled) {
            binding.swipeToRefresh.isRefreshing = false
            binding.swipeToRefresh.isEnabled = false
        } else {
            binding.swipeToRefresh.setOnRefreshListener {
                (fragmentManager?.findFragmentById(R.id.priceDataContainer) as PriceGraphFragment)
                        .initializeData()
                //TODO: Decide 1 or 2 SwipeToRefresh for screen.
                /*(fragmentManager?.findFragmentById(R.id.contentFeedContainer) as ContentFeedFragment)
                        .initializeData()*/
            }
        }
    }

    fun disableSwipeToRefresh() {
        binding.swipeToRefresh.isRefreshing = false
    }

    private fun observeUserLoginStatus() {
        viewModel.user.observe(this, Observer { user ->
            setProfileButton(user != null)
        })
    }

    private fun observeProfileButton() {
        viewModel.profileButtonClick.observe(this, Observer { view ->
            if (user == null) {
                this.startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(listOf(AuthUI.IdpConfig.GoogleBuilder().build()))
                                .build(),
                        RC_SIGN_IN)
            } else {
                view.setOnClickListener(Navigation.createNavigateOnClickListener(
                        R.id.action_homeFragment_to_profileFragment, null))

                //TODO: Add to profile screen to log out.
                /*AuthUI.getInstance()
                        .signOut(activity!!)
                        .addOnCompleteListener {
                            Toast.makeText(activity, "Signed out.", Toast.LENGTH_LONG).show()
                        }*/
            }
        })
    }

    private fun setProfileButton(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            this.user = user
            profileButton.setImageResource(R.drawable.ic_profile_logged_in_24dp)
        } else {
            profileButton.setImageResource(R.drawable.ic_profile_logged_out_24dp)
        }
    }

}
