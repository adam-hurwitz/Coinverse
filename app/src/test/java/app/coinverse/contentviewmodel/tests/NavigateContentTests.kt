package app.coinverse.contentviewmodel.tests

import app.coinverse.content.ContentRepository
import app.coinverse.content.ContentRepository.getContent
import app.coinverse.content.ContentRepository.getMainFeedList
import app.coinverse.content.ContentRepository.queryLabeledContentList
import app.coinverse.content.ContentViewModel
import app.coinverse.content.models.ContentViewEvent.*
import app.coinverse.content.models.OpenContentSourceIntentEffect
import app.coinverse.content.models.UpdateAdsEffect
import app.coinverse.contentviewmodel.*
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.InstantExecutorExtension
import app.coinverse.utils.LCE_STATE.CONTENT
import app.coinverse.utils.observe
import app.coinverse.utils.viewEffects
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@ExtendWith(InstantExecutorExtension::class)
class NavigateContentTests {
    private val mainThreadSurrogate = newSingleThreadContext(UI_THREAD)
    private val contentViewModel = ContentViewModel()

    private fun NavigateContent() = navigateContentTestCases()

    @BeforeAll
    fun beforeAll() {
        mockkObject(ContentRepository)
    }

    @AfterAll
    fun afterAll() {
        unmockkAll() // Re-assigns transformation of object to original state prior to mock.
    }

    @BeforeEach
    fun beforeEach() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @AfterEach
    fun afterEach() {
        Dispatchers.resetMain() // Reset main dispatcher to the original Main dispatcher.
        mainThreadSurrogate.close()
    }

    @ParameterizedTest
    @MethodSource("NavigateContent")
    fun `Navigate Content`(test: NavigateContentTest) = runBlocking {
        mockComponents(test)
        FeedLoad(test.feedType, test.timeframe, false).also { event ->
            contentViewModel.processEvent(event)
        }
        ContentShared(test.mockContent).also { event ->
            contentViewModel.processEvent(event)
            assertThat(contentViewModel.viewEffects().shareContentIntent.observe()
                    .contentRequest.observe())
                    .isEqualTo(test.mockContent)
        }
        ContentSourceOpened(test.mockContent.url).also { event ->
            contentViewModel.processEvent(event)
            assertThat(contentViewModel.viewEffects().openContentSourceIntent.observe())
                    .isEqualTo(OpenContentSourceIntentEffect(test.mockContent.url))
        }
        // Occurs on Fragment 'onViewStateRestored'
        UpdateAds().also { event ->
            contentViewModel.processEvent(event)
            assertThat(contentViewModel.viewEffects().updateAds.observe().javaClass)
                    .isEqualTo(UpdateAdsEffect::class.java)
        }
        verifyTests(test)
    }

    private fun mockComponents(test: NavigateContentTest) {
        // Coinverse - ContentRepository
        every { getMainFeedList(test.isRealtime, any()) } returns mockGetMainFeedList(
                test.mockFeedList, CONTENT)
        every {
            queryLabeledContentList(test.feedType)
        } returns mockQueryMainContentList(test.mockFeedList)
        every { getContent(test.mockContent.id) } returns mockGetContent(test)
    }

    private fun verifyTests(test: NavigateContentTest) {
        verify {
            when (test.feedType) {
                MAIN -> getMainFeedList(test.isRealtime, any())
                SAVED, DISMISSED -> queryLabeledContentList(test.feedType)
            }
        }
    }
}
