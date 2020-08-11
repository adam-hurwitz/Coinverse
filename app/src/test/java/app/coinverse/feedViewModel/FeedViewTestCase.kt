package app.coinverse.feedViewModel

import app.coinverse.feed.Content
import app.coinverse.feed.state.FeedViewIntent.FeedLoad
import app.coinverse.feed.state.FeedViewIntent.SwipeContent
import app.coinverse.feed.state.FeedViewState
import app.coinverse.feed.state.FeedViewState.ClearAdjacentAds
import app.coinverse.feed.state.FeedViewState.OpenContentSource
import app.coinverse.feed.state.FeedViewState.ShareContent
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
        val swipeContentIntent: SwipeContent? = null,
        val swipeToRefreshState: SwipeToRefresh = SwipeToRefresh(),
        val openContentState: FeedViewState.OpenContent? = null,
        val openContentSourceState: OpenContentSource? = null,
        val swipeContentState: FeedViewState.SwipeContent? = null,
        val shareContentState: ShareContent? = null,
        val clearAdjacentAdsState: ClearAdjacentAds? = null
)