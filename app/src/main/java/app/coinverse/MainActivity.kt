package app.coinverse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation.findNavController
import app.coinverse.databinding.ActivityMainBinding
import app.coinverse.firebase.FirestoreHelper
import app.coinverse.home.HomeViewModel
import app.coinverse.utils.resourcesUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        FirestoreHelper.initialize(this)
        resourcesUtil = resources
    }

    override fun onSupportNavigateUp() = findNavController(navHostFragment.view!!).navigateUp()

    override fun onBackPressed() {
        if (homeViewModel.bottomSheetState.value == STATE_EXPANDED)
            homeViewModel.bottomSheetState.value = STATE_COLLAPSED
        else super.onBackPressed()
    }
}