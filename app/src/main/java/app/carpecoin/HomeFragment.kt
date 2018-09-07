package app.carpecoin

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import app.carpecoin.coin.R
import app.carpecoin.coin.databinding.FragmentHomeBinding
import app.carpecoin.contentFeed.ContentFragment
import app.carpecoin.firebase.FirestoreCollections.usersCollection
import app.carpecoin.priceGraph.PriceFragment
import app.carpecoin.user.SignInDialogFragment
import app.carpecoin.user.models.UserInfo
import app.carpecoin.utils.Constants.RC_SIGN_IN
import app.carpecoin.utils.Constants.SIGNIN_DIALOG_FRAGMENT_TAG
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.fragment_home.*
import java.util.*

private var LOG_TAG = HomeFragment::class.java.simpleName

private const val PRICEGRAPH_FRAGMENT_TAG = "priceGraphFragmentTag"
private const val CONTENTFEED_FRAGMENT_TAG = "contentFeedFragmentTag"

class HomeFragment : Fragment() {

    private var user: FirebaseUser? = null

    private lateinit var binding: FragmentHomeBinding
    private lateinit var homeViewModel: HomeViewModel

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null
                && childFragmentManager.findFragmentByTag(PRICEGRAPH_FRAGMENT_TAG) == null
                && childFragmentManager.findFragmentByTag(CONTENTFEED_FRAGMENT_TAG) == null) {
            childFragmentManager.beginTransaction()
                    .replace(priceContainer.id, PriceFragment.newInstance(),
                            PRICEGRAPH_FRAGMENT_TAG)
                    .commit()
            childFragmentManager.beginTransaction()
                    .replace(contentContainer.id, ContentFragment.newInstance(),
                            CONTENTFEED_FRAGMENT_TAG)
                    .commit()
        }
        user = homeViewModel.getCurrentUser()
        setProfileButton(user != null)
        setRefresh()
        observeProfileButtonClick()
        observeSignIn()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == RESULT_OK) {
                user = homeViewModel.getCurrentUser()
                setProfileButton(user != null)
            } else {
                println(String.format("sign_in fail:%s", response?.error?.errorCode))
            }
        }
    }

    fun setRefresh() {
        swipeToRefresh.setOnRefreshListener {
            (childFragmentManager.findFragmentById(R.id.priceContainer) as PriceFragment)
                    .getPrices(false)
            (childFragmentManager.findFragmentById(R.id.contentContainer) as ContentFragment)
                    .getContent(false)
        }

        appBar.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                if (Math.abs(verticalOffset) - appBarLayout.getTotalScrollRange() == 0) {
                    swipeToRefresh.isEnabled = false // appBar collapsed.
                } else {
                    swipeToRefresh.isEnabled = true // appBar expanded.
                }
            }
        })

        homeViewModel.endSwipeToRefresh.observe(viewLifecycleOwner, Observer { disableSwipeToRefresh: Boolean ->
            swipeToRefresh.isRefreshing = false
        })
    }

    private fun observeProfileButtonClick() {
        homeViewModel.profileButtonClick.observe(viewLifecycleOwner, Observer { isClicked ->
            if (user == null) {
                SignInDialogFragment.newInstance().show(fragmentManager, SIGNIN_DIALOG_FRAGMENT_TAG)
            } else {
                val action =
                        HomeFragmentDirections.actionHomeFragmentToProfileFragment(user!!)
                action.setUser(user!!)
                profileButton.setOnClickListener(Navigation.createNavigateOnClickListener(
                        R.id.action_homeFragment_to_profileFragment, action.arguments))
            }
        })
    }

    private fun observeSignIn() {
        homeViewModel.user.observe(this, Observer { user: FirebaseUser? ->
            this.user = user
            setProfileButton(user != null)
            if (user != null) {
                usersCollection.document(user.uid).get().addOnCompleteListener { userQuery ->
                    if (!userQuery.result.exists()) {
                        usersCollection.document(user.uid).set(
                                UserInfo(user.uid, user.displayName, user.email, user.phoneNumber,
                                        user.photoUrl.toString(),
                                        Date(user.metadata!!.creationTimestamp),
                                        Date(user.metadata!!.lastSignInTimestamp), user.providerId))
                                .addOnSuccessListener {
                                    Log.v(LOG_TAG, String.format("New user added success:%s", it))
                                }.addOnFailureListener {
                                    Log.v(LOG_TAG, String.format("New user added failure:%s", it))
                                }
                    }
                }
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
            profileButton.setImageResource(R.drawable.ic_profile_logged_in_color_accent_24dp)
        }
    }

}
