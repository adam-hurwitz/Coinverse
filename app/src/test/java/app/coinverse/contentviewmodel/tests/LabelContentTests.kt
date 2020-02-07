package app.coinverse.contentviewmodel.tests

import androidx.lifecycle.SavedStateHandle
import app.coinverse.analytics.Analytics
import app.coinverse.analytics.Analytics.labelContentFirebaseAnalytics
import app.coinverse.analytics.Analytics.updateActionAnalytics
import app.coinverse.contentviewmodel.LabelContentTest
import app.coinverse.contentviewmodel.mockEditContentLabels
import app.coinverse.contentviewmodel.mockGetMainFeedList
import app.coinverse.contentviewmodel.mockQueryMainContentListFlow
import app.coinverse.contentviewmodel.testCases.labelContentTestCases
import app.coinverse.feed.FeedRepository
import app.coinverse.feed.FeedRepository.editContentLabels
import app.coinverse.feed.FeedRepository.getMainFeedList
import app.coinverse.feed.FeedRepository.queryLabeledContentList
import app.coinverse.feed.models.FeedViewEffectType.*
import app.coinverse.feed.models.FeedViewEventType.*
import app.coinverse.feed.viewmodels.FeedViewModel
import app.coinverse.home.HomeViewModel
import app.coinverse.utils.*
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.LCE_STATE.CONTENT
import app.coinverse.utils.LCE_STATE.ERROR
import com.crashlytics.android.Crashlytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.*
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@ExtendWith(ContentTestExtension::class)
class LabelContentTests(val testDispatcher: TestCoroutineDispatcher) {

    private fun LabelContent() = labelContentTestCases()
    private lateinit var feedViewModel: FeedViewModel

    @BeforeAll
    fun beforeAll() {

        // Android libraries
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseRemoteConfig::class)
        mockkStatic(Crashlytics::class)

        // Coinverse
        mockkObject(Analytics)
    }

    @ParameterizedTest
    @MethodSource("LabelContent")
    fun `Label Content`(test: LabelContentTest) = testDispatcher.runBlockingTest {
        mockComponents(test)
        feedViewModel = FeedViewModel(SavedStateHandle(), test.feedType, test.timeframe, test.isRealtime)
        assertContentList(test)
        ContentSwipeDrawed(test.isDrawed).also { event ->
            feedViewModel.contentSwipeDrawed(event)
            assertEnableSwipeToRefresh()
        }
        ContentSwiped(test.feedType, test.actionType, test.adapterPosition).also { event ->
            feedViewModel.contentSwiped(event)
            assertContentLabeled(test)
        }
        verifyTests(test)
    }

    private fun assertContentList(test: LabelContentTest) {
        feedViewModel.feedViewState().contentList.getOrAwaitValue().also { pagedList ->
            assertThat(pagedList).isEqualTo(test.mockFeedList)
        }
    }

    private fun assertEnableSwipeToRefresh() {
        HomeViewModel().apply {
            enableSwipeToRefresh(
                    feedViewModel.viewEffects().enableSwipeToRefresh.observe().isEnabled)
            assertThat(isSwipeToRefreshEnabled.getOrAwaitValue()).isEqualTo(false)
        }
    }

    private fun assertContentLabeled(test: LabelContentTest) {
        feedViewModel.viewEffects().contentSwiped.observe().also { contentSwipedEffect ->
            assertThat(contentSwipedEffect).isEqualTo(ContentSwipedEffect(
                    feedType = test.feedType,
                    actionType = test.actionType,
                    position = test.adapterPosition))
            ContentLabeled(
                    feedType = contentSwipedEffect.feedType,
                    actionType = contentSwipedEffect.actionType,
                    user = test.mockUser(),
                    position = contentSwipedEffect.position,
                    content = test.mockContent,
                    isMainFeedEmptied = false).also { event ->
                feedViewModel.contentLabeled(event)
                if (test.isUserSignedIn) {
                    when (test.lceState) {
                        CONTENT -> {
                            assertThat(feedViewModel.feedViewState().contentLabeled.observe())
                                    .isEqualTo(app.coinverse.feed.models.ContentLabeled(
                                            position = test.adapterPosition, errorMessage = ""))
                            assertThat(feedViewModel.viewEffects().notifyItemChanged.observe())
                                    .isEqualTo(NotifyItemChangedEffect(position = test.adapterPosition))
                        }
                        ERROR -> {
                            assertThat(feedViewModel.feedViewState().contentLabeled.observe()).isNull()
                            assertThat(feedViewModel.viewEffects().snackBar.observe())
                                    .isEqualTo(SnackBarEffect(text = MOCK_CONTENT_LABEL_ERROR))
                        }
                    }
                } else {
                    feedViewModel.feedViewState().contentLabeled.observe()
                    assertThat(feedViewModel.viewEffects().notifyItemChanged.observe())
                            .isEqualTo(NotifyItemChangedEffect(test.adapterPosition))
                    assertThat(feedViewModel.viewEffects().signIn.observe())
                            .isEqualTo(SignInEffect(true))
                }
            }
        }
    }

    private fun mockComponents(test: LabelContentTest) {

        // Android libraries
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true)
        every { FirebaseRemoteConfig.getInstance() } returns mockk(relaxed = true)
        every { Crashlytics.log(any(), any(), any()) } returns Unit

        // Coinverse

        // ContentRepository
        coEvery { getMainFeedList(test.isRealtime, any()) } returns mockGetMainFeedList(
                test.mockFeedList, CONTENT)
        every {
            editContentLabels(test.feedType, test.actionType, test.mockContent, any(), test.adapterPosition)
        } returns mockEditContentLabels(test)
        every { labelContentFirebaseAnalytics(test.mockContent) } returns mockk(relaxed = true)
        every {
            updateActionAnalytics(test.actionType, test.mockContent, any())
        } returns mockk(relaxed = true)
        every {
            queryLabeledContentList(test.feedType)
        } returns mockQueryMainContentListFlow(test.mockFeedList)

        // FirebaseRemoteConfig - Constant values
        mockkStatic(CONSTANTS_CLASS_COMPILED_JAVA)
        every { CONTENT_LABEL_ERROR } returns MOCK_CONTENT_LABEL_ERROR
    }

    private fun verifyTests(test: LabelContentTest) {
        coVerify {
            if (test.isUserSignedIn)
                editContentLabels(test.feedType, test.actionType, test.mockContent, any(), test.adapterPosition)
            when (test.feedType) {
                MAIN -> getMainFeedList(test.isRealtime, any())
                SAVED, DISMISSED -> queryLabeledContentList(test.feedType)
            }
        }
        confirmVerified(FeedRepository)
    }
}