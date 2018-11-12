package app.coinverse.user

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import app.coinverse.Enums.SignInType.DIALOG
import app.coinverse.Enums.SignInType.FULLSCREEN
import app.coinverse.databinding.FragmentSignInBinding
import app.coinverse.databinding.FragmentSignInDialogBinding
import app.coinverse.home.HomeViewModel
import app.coinverse.utils.RC_SIGN_IN
import app.coinverse.utils.SIGNIN_TYPE_KEY
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_sign_in_dialog.*

class SignInDialogFragment : DialogFragment() {

    private lateinit var homeViewModel: HomeViewModel

    companion object {
        fun newInstance(bundle: Bundle) = SignInDialogFragment().apply { arguments = bundle }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeViewModel = ViewModelProviders.of(activity!!).get(HomeViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            when (arguments?.getInt(SIGNIN_TYPE_KEY)) {
                DIALOG.code -> FragmentSignInDialogBinding.inflate(inflater, container, false).root
                FULLSCREEN.code -> FragmentSignInBinding.inflate(inflater, container, false).root
                else -> FragmentSignInDialogBinding.inflate(inflater, container, false).root
            }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        signIn.setOnClickListener {
            startActivityForResult(
                    AuthUI.getInstance().createSignInIntentBuilder()
                            .setAvailableProviders(listOf(AuthUI.IdpConfig.GoogleBuilder().build()))
                            .build(),
                    RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
                homeViewModel.user.value = user
                dismiss()
            } else {
                println(String.format("sign_in fail:%s", response?.error?.errorCode))
            }
        }
    }
}
