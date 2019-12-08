package app.coinverse.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import app.coinverse.R
import app.coinverse.R.string.*
import app.coinverse.analytics.Analytics.setCurrentScreen
import app.coinverse.databinding.FragmentUserBinding
import app.coinverse.firebase.firebaseApp
import app.coinverse.home.HomeViewModel
import app.coinverse.utils.FeedType.DISMISSED
import app.coinverse.utils.PROFILE_VIEW
import app.coinverse.utils.Status.SUCCESS
import app.coinverse.utils.livedata.EventObserver
import app.coinverse.utils.snackbarWithText
import com.crashlytics.android.Crashlytics
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.getInstance
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar.make
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val LOG_TAG = UserFragment::class.java.simpleName

/**
 * TODO: Refactor
 *  1. Refactor with Unidirectional Data Flow. See [app.coinverse.content.ContentViewModel].
 *  https://medium.com/hackernoon/android-unidirectional-flow-with-livedata-bf24119e747
 *  2. Move Firebase calls to Repository.
 **/

class UserFragment : Fragment() {
    private lateinit var binding: FragmentUserBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var user: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCurrentScreen(activity!!, PROFILE_VIEW)
        userViewModel = ViewModelProviders.of(activity!!).get(UserViewModel::class.java)
        homeViewModel = ViewModelProviders.of(activity!!).get(HomeViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentUserBinding.inflate(inflater, container, false)
        binding.viewmodel = homeViewModel
        user = UserFragmentArgs.fromBundle(arguments!!).user
        binding.user = user
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbar()
        setClickListeners()
    }

    fun setToolbar() {
        toolbar.title = user.displayName
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    fun setClickListeners() {
        dismissedContent.setOnClickListener { view: View ->
            view.findNavController().navigate(R.id.action_userFragment_to_dismissedContentFragment,
                    UserFragmentDirections.actionUserFragmentToDismissedContentFragment().apply {
                        feedType = DISMISSED.name
                    }.arguments)
        }

        signOut.setOnClickListener { view: View ->
            var message: Int
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                try {
                    lifecycleScope.launch {
                        getInstance().signOut(context!!).await()
                        homeViewModel.setUser(null)
                        message = signed_out
                        FirebaseAuth.getInstance(firebaseApp(true)).signInAnonymously().await()
                        Snackbar.make(view, getString(message), LENGTH_SHORT).show()
                        activity?.onBackPressed()
                    }
                } catch (exception: FirebaseAuthException) {
                    //TODO: Add retry.
                    Crashlytics.log(Log.ERROR, LOG_TAG, "observeSignIn ${exception.localizedMessage}")
                    snackbarWithText(getString(error_sign_in_anonymously), contentContainer)
                    Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            } else {
                message = already_signed_out
                Snackbar.make(view, getString(message), LENGTH_SHORT).show()
            }
        }
        delete.setOnClickListener { view: View ->
            FirebaseAuth.getInstance().currentUser?.let { user ->
                userViewModel.deleteUser(user).observe(viewLifecycleOwner, EventObserver { status ->
                    if (status == SUCCESS)
                        lifecycleScope.launch {
                            try {
                                AuthUI.getInstance().signOut(context!!).await()
                                homeViewModel.setUser(null)
                                Snackbar.make(view, getString(deleted), LENGTH_SHORT).show()
                                activity?.onBackPressed()
                                FirebaseAuth.getInstance(firebaseApp(true)).signInAnonymously().await()
                                Crashlytics.log(Log.VERBOSE, LOG_TAG, "observeSignIn anonymous success")
                            } catch (e: FirebaseAuthException) {
                                Crashlytics.log(Log.ERROR, LOG_TAG, "observeSignIn ${e.localizedMessage}")
                                snackbarWithText(getString(error_sign_in_anonymously), contentContainer)
                                Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    else make(view, getString(unable_to_delete), LENGTH_SHORT).show()
                })
            }
        }
    }
}
