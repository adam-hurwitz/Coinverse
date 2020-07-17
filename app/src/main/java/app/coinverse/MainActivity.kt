package app.coinverse

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import app.coinverse.R.id.dismissedContentFragment
import app.coinverse.R.id.homeFragment
import app.coinverse.databinding.ActivityMainBinding
import app.coinverse.feed.state.FeedViewState.OpenContent
import app.coinverse.home.HomeViewModel
import app.coinverse.utils.FeedType.DISMISSED
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.FeedType.SAVED
import app.coinverse.utils.OPEN_CONTENT_FROM_NOTIFICATION_KEY
import app.coinverse.utils.OPEN_FROM_NOTIFICATION_ACTION
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val LOG_TAG = MainActivity::class.java.simpleName
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMainBinding.inflate(layoutInflater).root)
        openContentFromNotification()
    }

    override fun onSupportNavigateUp() =
            findNavController(navHostFragment.requireView()).navigateUp()

    override fun onBackPressed() {
        if (homeViewModel.bottomSheetState.value == STATE_EXPANDED)
            homeViewModel.setBottomSheetState(STATE_COLLAPSED)
        else super.onBackPressed()
    }

    private fun openContentFromNotification() {
        if (intent.action == OPEN_FROM_NOTIFICATION_ACTION)
            when (
                intent.getParcelableExtra<OpenContent>(OPEN_CONTENT_FROM_NOTIFICATION_KEY)?.let {
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