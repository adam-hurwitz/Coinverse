package app.coinverse.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import app.coinverse.databinding.FragmentPermissionsDialogBinding
import app.coinverse.home.HomeViewModel
import kotlinx.android.synthetic.main.fragment_permissions_dialog.*

class PermissionsDialogFragment : DialogFragment() {

    private val homeViewModel: HomeViewModel by activityViewModels()

    fun newInstance() = PermissionsDialogFragment()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            FragmentPermissionsDialogBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        confirm.setOnClickListener {
            homeViewModel.setShowLocationPermission(true)
            dismiss()
        }
    }
}