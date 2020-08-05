package app.coinverse.feedViewModel

import app.coinverse.feed.Content
import app.coinverse.feed.state.FeedViewIntentType
import app.coinverse.feed.state.FeedViewIntentType.FeedLoad
import app.coinverse.feed.state.FeedViewState
import app.coinverse.feed.state.FeedViewState.SwipeContent
import app.coinverse.feed.state.FeedViewState.SwipeToRefresh
import app.coinverse.utils.Status
import kotlinx.coroutines.ExperimentalCoroutinesApi

// Use same 'ContentType' within each test.
@ExperimentalCoroutinesApi
data class FeedViewTestCase constructor(
        val status: Status,
        val isLoggedIn: Boolean = true,
        val mockFeedList: List<Content>,
        val mockContent: Content? = Content(),
        val error: String? = null,
        val feedLoadIntent: FeedLoad,
        val swipeContentIntent: FeedViewIntentType.SwipeContent? = null,
        val swipeToRefreshState: SwipeToRefresh = SwipeToRefresh(),
        val openContentState: FeedViewState.OpenContent? = null,
        val openContentSourceState: FeedViewState.OpenContentSource? = null,
        val swipeContentState: SwipeContent? = null,
        val shareContentState: FeedViewState.ShareContent? = null,
        val clearAdjacentAdsState: FeedViewState.ClearAdjacentAds? = null
)