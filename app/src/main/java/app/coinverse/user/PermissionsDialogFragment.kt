package app.coinverse.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import app.coinverse.databinding.FragmentPermissionsDialogBinding
import app.coinverse.home.HomeViewModel
import app.coinverse.utils.livedata.Event
import kotlinx.android.synthetic.main.fragment_permissions_dialog.*

class PermissionsDialogFragment : DialogFragment() {

    private lateinit var homeViewModel: HomeViewModel

    companion object {
        fun newInstance() = PermissionsDialogFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeViewModel = ViewModelProviders.of(activity!!).get(HomeViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            FragmentPermissionsDialogBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        confirm.setOnClickListener {
            homeViewModel.showLocationPermission.value = Event(true)
            dismiss()
        }
    }
}