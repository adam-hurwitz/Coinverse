package app.carpecoin

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import app.carpecoin.coin.databinding.FragmentProfileBinding
import kotlinx.android.synthetic.main.fragment_profile.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import app.carpecoin.coin.R
import app.carpecoin.firebase.FirestoreCollections.usersCollection
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import app.carpecoin.utils.Constants.ON_BACK_PRESS_DELAY_IN_MILLIS

private var LOG_TAG = ProfileFragment::class.java.simpleName

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var viewModel: HomeViewModel
    private lateinit var user: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(HomeViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        user = ProfileFragmentArgs.fromBundle(arguments).user
        binding.user = user
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.title = user.displayName
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        setClickListeners(view)
    }

    fun setClickListeners(view: View) {
        //TODO: Launch Archived content
        archivedContent.setOnClickListener {
            Toast.makeText(context!!, "TODO: Launch archived content.", Toast.LENGTH_SHORT).show()
        }
        signOut.setOnClickListener {
            var message: Int
            if (FirebaseAuth.getInstance().currentUser != null) {
                AuthUI.getInstance()
                        .signOut(context!!)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                viewModel.user.value = null
                                message = R.string.signed_out
                                Snackbar.make(view, getString(message), Snackbar.LENGTH_SHORT).show()
                                signOut.postDelayed({
                                    activity?.onBackPressed()
                                }, ON_BACK_PRESS_DELAY_IN_MILLIS)
                            }
                            //TODO: Add retry.
                        }
            } else {
                message = R.string.already_signed_out
                Snackbar.make(view, getString(message), Snackbar.LENGTH_SHORT).show()
            }
        }
        delete.setOnClickListener {
            var message: Int
            if (FirebaseAuth.getInstance().currentUser != null) {
                AuthUI.getInstance()
                        .delete(context!!)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                usersCollection.document(viewModel.user.value!!.uid).delete()
                                        .addOnSuccessListener {
                                            viewModel.user.value = null
                                            message = R.string.deleted
                                            Snackbar.make(view, getString(message), Snackbar.LENGTH_SHORT).show()
                                            delete.postDelayed({
                                                activity?.onBackPressed()
                                            }, ON_BACK_PRESS_DELAY_IN_MILLIS)
                                            Log.v(LOG_TAG, String.format("Delete user success:%s", it))
                                        }.addOnFailureListener {
                                            //TODO: Add retry.
                                            Log.v(LOG_TAG, String.format("Delete user failure:%s", it))
                                        }
                            }
                        }.addOnFailureListener {
                            //TODO: Add retry.
                            Log.v(LOG_TAG, String.format("Delete user failure:%s", it))
                        }
            } else {
                message = R.string.unable_to_delete
                Snackbar.make(view, getString(message), Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
