package app.coinverse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import app.coinverse.R.id.dismissedContentFragment
import app.coinverse.R.id.homeFragment
import app.coinverse.R.layout.activity_main
import app.coinverse.content.models.ContentToPlay
import app.coinverse.databinding.ActivityMainBinding
import app.coinverse.home.HomeViewModel
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.OPEN_CONTENT_FROM_NOTIFICATION_KEY
import app.coinverse.utils.OPEN_FROM_NOTIFICATION_ACTION
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val LOG_TAG = MainActivity::class.java.simpleName

    private lateinit var binding: ActivityMainBinding
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        binding = setContentView(this, activity_main)
        openContentFromNotification()
    }

    override fun onSupportNavigateUp() = findNavController(navHostFragment.view!!).navigateUp()

    override fun onBackPressed() {
        if (homeViewModel.bottomSheetState.value == STATE_EXPANDED)
            homeViewModel.setBottomSheetState(STATE_COLLAPSED)
        else super.onBackPressed()
    }

    private fun openContentFromNotification() {
        if (intent.action == OPEN_FROM_NOTIFICATION_ACTION)
            when (
                intent.getParcelableExtra<ContentToPlay>(OPEN_CONTENT_FROM_NOTIFICATION_KEY)?.let {
                    it.content.feedType
                }) {
                MAIN, SAVED ->
                    navHostFragment.findNavController().navigate(homeFragment, Bundle().also { bundle ->
                        bundle.putAll(intent.extras)
                    })
                DISMISSED ->
                    navHostFragment.findNavController().navigate(dismissedContentFragment, Bundle().also { bundle ->
                        bundle.putAll(intent.extras)
                    })
            }
    }
}