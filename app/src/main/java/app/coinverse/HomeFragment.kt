package app.coinverse

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import app.coinverse.Enums.FeedType.MAIN
import app.coinverse.Enums.FeedType.SAVED
import app.coinverse.coin.R
import app.coinverse.coin.databinding.FragmentHomeBinding
import app.coinverse.content.ContentFragment
import app.coinverse.firebase.FirestoreCollections.usersCollection
import app.coinverse.priceGraph.PriceFragment
import app.coinverse.user.SignInDialogFragment
import app.coinverse.user.models.User
import app.coinverse.utils.Constants
import app.coinverse.utils.Constants.SIGNIN_DIALOG_FRAGMENT_TAG
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.fragment_home.*
import java.util.*

private val LOG_TAG = HomeFragment::class.java.simpleName
private const val PRICEGRAPH_FRAGMENT_TAG = "priceGraphFragmentTag"
private const val CONTENTFEED_FRAGMENT_TAG = "contentFeedFragmentTag"
private const val FEED_TYPE = "feedType"

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        user = homeViewModel.getCurrentUser()
        setCollapsingToolbarStates()
        setProfileButton(user != null)
        setClickListeners()
        observeSignIn()
        setRefresh()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null
                && childFragmentManager.findFragmentByTag(PRICEGRAPH_FRAGMENT_TAG) == null
                && childFragmentManager.findFragmentByTag(CONTENTFEED_FRAGMENT_TAG) == null) {
            childFragmentManager.beginTransaction()
                    .replace(priceContainer.id, PriceFragment.newInstance(), PRICEGRAPH_FRAGMENT_TAG)
                    .commit()
            val contentBundle = Bundle()
            contentBundle.putString(FEED_TYPE, MAIN.name)
            childFragmentManager.beginTransaction()
                    .replace(contentContainer.id, ContentFragment.newInstance(contentBundle),
                            CONTENTFEED_FRAGMENT_TAG)
                    .commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                user = homeViewModel.getCurrentUser()
                setProfileButton(user != null)
            } else {
                println(String.format("sign_in fail:%s", response?.error?.errorCode))
            }
        }
    }

    private fun setCollapsingToolbarStates() {
        appBar.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                // appBar collapsed.
                if (Math.abs(verticalOffset) - appBarLayout.totalScrollRange == 0) {
                    swipeToRefresh.isEnabled = false
                    fab.show()
                } else { // appBar expanded.
                    swipeToRefresh.isEnabled = true
                    fab.hide()
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

    private fun setClickListeners() {

        profileButton.setOnClickListener { view: View ->
            if (user != null) {
                val action =
                        HomeFragmentDirections.actionHomeFragmentToProfileFragment(user!!)
                action.setUser(user!!)
                view.findNavController().navigate(R.id.action_homeFragment_to_profileFragment, action.arguments)
            } else {
                SignInDialogFragment.newInstance().show(fragmentManager, SIGNIN_DIALOG_FRAGMENT_TAG)
            }
        }

        fab.setOnClickListener { view: View ->
            if (homeViewModel.user.value != null) {
                val action =
                        HomeFragmentDirections.actionHomeFragmentToContentFragment()
                action.setFeedType(SAVED.name)
                view.findNavController().navigate(R.id.action_homeFragment_to_contentFragment, action.arguments)
            } else {
                SignInDialogFragment.newInstance().show(fragmentManager, SIGNIN_DIALOG_FRAGMENT_TAG)
            }
        }

    }

    private fun observeSignIn() {
        homeViewModel.user.observe(this, Observer { user: FirebaseUser? ->
            this.user = user
            setProfileButton(user != null)
            if (user != null) {
                usersCollection.document(user.uid).get().addOnCompleteListener { userQuery ->
                    if (!userQuery.result.exists()) {
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
            }
        })
    }

    fun setRefresh() {
        swipeToRefresh.setOnRefreshListener {
            (childFragmentManager.findFragmentById(R.id.priceContainer) as PriceFragment)
                    .getPrices(false, false)
            (childFragmentManager.findFragmentById(R.id.contentContainer) as ContentFragment)
                    .initializeMainContent(false)
        }
        homeViewModel.endSwipeToRefresh.observe(viewLifecycleOwner, Observer { disableSwipeToRefresh: Boolean ->
            swipeToRefresh.isRefreshing = false
        })
    }

}
