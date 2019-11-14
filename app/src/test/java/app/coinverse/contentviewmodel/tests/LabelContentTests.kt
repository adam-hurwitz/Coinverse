package app.coinverse.contentviewmodel.tests

import app.coinverse.analytics.Analytics
import app.coinverse.analytics.Analytics.labelContentFirebaseAnalytics
import app.coinverse.analytics.Analytics.updateActionAnalytics
import app.coinverse.content.ContentRepository
import app.coinverse.content.ContentRepository.editContentLabels
import app.coinverse.content.ContentRepository.getMainFeedList
import app.coinverse.content.ContentRepository.queryLabeledContentList
import app.coinverse.content.ContentViewModel
import app.coinverse.content.models.ContentEffectType.*
import app.coinverse.content.models.ContentViewEvents.*
import app.coinverse.contentviewmodel.*
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
class LabelContentTests(val testDispatcher: TestCoroutineDispatcher,
                        val contentViewModel: ContentViewModel) {

    private fun LabelContent() = labelContentTestCases()

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
        FeedLoad(test.feedType, test.timeframe, false).also { event ->
            contentViewModel.processEvent(event)
            assertContentList(test)
        }
        ContentSwipeDrawed(test.isDrawed).also { event ->
            contentViewModel.processEvent(event)
            assertEnableSwipeToRefresh()
        }
        ContentSwiped(test.feedType, test.actionType, test.adapterPosition).also { event ->
            contentViewModel.processEvent(event)
            assertContentLabeled(test)
        }
        verifyTests(test)
    }

    private fun assertContentList(test: LabelContentTest) {
        contentViewModel.feedViewState().contentList.getOrAwaitValue().also { pagedList ->
            assertThat(pagedList).isEqualTo(test.mockFeedList)
        }
    }

    private fun assertEnableSwipeToRefresh() {
        HomeViewModel().apply {
            enableSwipeToRefresh(
                    contentViewModel.viewEffects().enableSwipeToRefresh.observe().isEnabled)
            assertThat(isSwipeToRefreshEnabled.getOrAwaitValue()).isEqualTo(false)
        }
    }

    private fun assertContentLabeled(test: LabelContentTest) {
        contentViewModel.viewEffects().contentSwiped.observe().also { contentSwipedEffect ->
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
                contentViewModel.processEvent(event)
                if (test.isUserSignedIn) {
                    when (test.lceState) {
                        CONTENT -> {
                            assertThat(contentViewModel.feedViewState().contentLabeled.observe())
                                    .isEqualTo(app.coinverse.content.models.ContentLabeled(
                                            position = test.adapterPosition, errorMessage = ""))
                            assertThat(contentViewModel.viewEffects().notifyItemChanged.observe())
                                    .isEqualTo(NotifyItemChangedEffect(position = test.adapterPosition))
                        }
                        ERROR -> {
                            assertThat(contentViewModel.feedViewState().contentLabeled.observe()).isNull()
                            assertThat(contentViewModel.viewEffects().snackBar.observe())
                                    .isEqualTo(SnackBarEffect(text = MOCK_CONTENT_LABEL_ERROR))
                        }
                    }
                } else {
                    contentViewModel.feedViewState().contentLabeled.observe()
                    assertThat(contentViewModel.viewEffects().notifyItemChanged
                            .observe())
                            .isEqualTo(NotifyItemChangedEffect(test.adapterPosition))
                    assertThat(contentViewModel.viewEffects().signIn
                            .observe()).isEqualTo(SignInEffect(true))
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
        confirmVerified(ContentRepository)
    }
}