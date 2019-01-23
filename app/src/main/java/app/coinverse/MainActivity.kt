package app.coinverse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation.findNavController
import app.coinverse.R.layout
import app.coinverse.databinding.ActivityMainBinding
import app.coinverse.firebase.FirestoreHelper
import app.coinverse.home.HomeViewModel
import app.coinverse.utils.AD_UNIT_ID
import app.coinverse.utils.resourcesUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.mopub.common.MoPub
import com.mopub.common.SdkConfiguration
import com.mopub.common.SdkInitializationListener
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private val LOG_TAG = MainActivity::class.java.simpleName

    private lateinit var binding: ActivityMainBinding
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirestoreHelper.initialize(this)
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        resourcesUtil = resources
        MoPub.initializeSdk(this, SdkConfiguration.Builder(AD_UNIT_ID).build(), initSdkListener())
        binding = setContentView(this, layout.activity_main)
    }

    override fun onSupportNavigateUp() = findNavController(navHostFragment.view!!).navigateUp()

    override fun onBackPressed() {
        if (homeViewModel.bottomSheetState.value == STATE_EXPANDED)
            homeViewModel.bottomSheetState.value = STATE_COLLAPSED
        else super.onBackPressed()
    }

    private fun initSdkListener() = SdkInitializationListener { /* MoPub SDK initialized.*/ }
}