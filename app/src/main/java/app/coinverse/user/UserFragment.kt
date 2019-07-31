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
import androidx.navigation.findNavController
import app.coinverse.R
import app.coinverse.R.string.*
import app.coinverse.databinding.FragmentUserBinding
import app.coinverse.firebase.firebaseApp
import app.coinverse.home.HomeViewModel
import app.coinverse.utils.Enums.FeedType.DISMISSED
import app.coinverse.utils.Enums.Status.SUCCESS
import app.coinverse.utils.PROFILE_VIEW
import app.coinverse.utils.SIGN_OUT_ON_BACK_PRESS_DELAY_IN_MILLIS
import app.coinverse.utils.livedata.EventObserver
import app.coinverse.utils.snackbarWithText
import com.crashlytics.android.Crashlytics
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.getInstance
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar.make
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.toolbar.*

private val LOG_TAG = UserFragment::class.java.simpleName

class UserFragment : Fragment() {
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var binding: FragmentUserBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var user: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics = FirebaseAnalytics.getInstance(FirebaseApp.getInstance().applicationContext)
        analytics.setCurrentScreen(activity!!, PROFILE_VIEW, null)
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
            FirebaseAuth.getInstance().currentUser.let { user ->
                if (user != null)
                    getInstance().signOut(context!!).addOnCompleteListener {
                        if (it.isSuccessful) {
                            homeViewModel.setUser(null)
                            message = signed_out
                            make(view, getString(message), LENGTH_SHORT).show()
                            signOut.postDelayed({ activity?.onBackPressed() },
                                    SIGN_OUT_ON_BACK_PRESS_DELAY_IN_MILLIS)
                            //TODO - Create view event, add to Repo, handle result with LCE
                            FirebaseAuth.getInstance(firebaseApp(true)).signInAnonymously()
                                    .addOnCompleteListener(activity!!) { task ->
                                        if (task.isSuccessful)
                                            Crashlytics.log(Log.VERBOSE, LOG_TAG, "observeSignIn anonymous success")
                                        else {
                                            Crashlytics.log(Log.ERROR, LOG_TAG, "observeSignIn ${task.exception}")
                                            snackbarWithText(getString(error_sign_in_anonymously), contentContainer)
                                            Toast.makeText(context, "Authentication failed.",
                                                    Toast.LENGTH_SHORT).show()
                                        }
                                    }
                        } //TODO: Add retry.
                    }
                else {
                    message = already_signed_out
                    Snackbar.make(view, getString(message), LENGTH_SHORT).show()
                }
            }
        }
        delete.setOnClickListener { view: View ->
            FirebaseAuth.getInstance().currentUser?.let { user ->
                userViewModel.deleteUser(user).observe(viewLifecycleOwner, EventObserver { status ->
                    if (status == SUCCESS)
                        AuthUI.getInstance().signOut(context!!).addOnCompleteListener { status ->
                            if (status.isSuccessful) {
                                homeViewModel.setUser(null)
                                make(view, getString(deleted), LENGTH_SHORT).show()
                                delete.postDelayed({
                                    activity?.onBackPressed()
                                }, SIGN_OUT_ON_BACK_PRESS_DELAY_IN_MILLIS)
                                //TODO - Create view event, add to Repo, handle result with LCE
                                FirebaseAuth.getInstance(firebaseApp(true)).signInAnonymously()
                                        .addOnCompleteListener(activity!!) { task ->
                                            if (task.isSuccessful)
                                                Crashlytics.log(Log.VERBOSE, LOG_TAG, "observeSignIn anonymous success")
                                            else {
                                                Crashlytics.log(Log.ERROR, LOG_TAG, "observeSignIn ${task.exception}")
                                                snackbarWithText(getString(error_sign_in_anonymously), contentContainer)
                                                Toast.makeText(context, "Authentication failed.",
                                                        Toast.LENGTH_SHORT).show()
                                            }
                                        }
                            }
                        }
                    else make(view, getString(unable_to_delete), LENGTH_SHORT).show()
                })
            }
        }
    }
}
