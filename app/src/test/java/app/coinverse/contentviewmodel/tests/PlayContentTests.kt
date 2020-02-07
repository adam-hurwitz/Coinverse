package app.coinverse.contentviewmodel.tests

import android.net.Uri
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.SavedStateHandle
import app.coinverse.contentviewmodel.*
import app.coinverse.contentviewmodel.testCases.playContentTestCases
import app.coinverse.feed.FeedRepository
import app.coinverse.feed.FeedRepository.bitmapToByteArray
import app.coinverse.feed.FeedRepository.getAudiocast
import app.coinverse.feed.FeedRepository.getContentUri
import app.coinverse.feed.FeedRepository.getMainFeedList
import app.coinverse.feed.FeedRepository.queryLabeledContentList
import app.coinverse.feed.models.AudioViewEventType.AudioPlayerLoad
import app.coinverse.feed.models.Content
import app.coinverse.feed.models.ContentPlayer
import app.coinverse.feed.models.ContentToPlay
import app.coinverse.feed.models.FeedViewEffectType.NotifyItemChangedEffect
import app.coinverse.feed.models.FeedViewEffectType.SnackBarEffect
import app.coinverse.feed.models.FeedViewEventType.ContentSelected
import app.coinverse.feed.viewmodels.AudioViewModel
import app.coinverse.feed.viewmodels.FeedViewModel
import app.coinverse.home.HomeViewModel
import app.coinverse.utils.*
import app.coinverse.utils.ContentType.ARTICLE
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.LCE_STATE.*
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
class PlayContentTests(val testDispatcher: TestCoroutineDispatcher, val audioViewModel: AudioViewModel) {

    private fun PlayContent() = playContentTestCases()
    private lateinit var feedViewModel: FeedViewModel

    @BeforeAll
    fun beforeAll() {
        // Android libraries
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseRemoteConfig::class)
        mockkStatic(Crashlytics::class)
        mockkStatic(Uri::class)
    }

    @ParameterizedTest
    @MethodSource("PlayContent")
    fun `Play Content`(test: PlayContentTest) = testDispatcher.runBlockingTest {
        mockComponents(test)
        feedViewModel = FeedViewModel(SavedStateHandle(), test.feedType, test.timeframe, test.isRealtime)
        assertContentList(test)
        ContentSelected(test.mockPosition, test.mockContent).also { event ->
            feedViewModel.contentSelected(event)
            assertContentSelected(test)
        }
        if (test.mockContent.contentType == ARTICLE)
            AudioPlayerLoad(test.mockContent.id, test.mockFilePath, test.mockPreviewImageUrl).also { event ->
                audioViewModel.audioPlayerLoad(event)
                assertAudioPlayerLoad(test)
            }
        verifyTests(test)
    }

    private fun mockComponents(test: PlayContentTest) {

        // Android libraries
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true)
        every { FirebaseRemoteConfig.getInstance() } returns mockk(relaxed = true)
        every { Crashlytics.log(any(), any(), any()) } returns Unit
        every { Uri.parse(any()) } returns mockk(relaxed = true)

        // Coinverse

        // ContentRepository
        coEvery {
            getMainFeedList(test.isRealtime, any())
        } returns mockGetMainFeedList(test.mockFeedList, CONTENT)
        every {
            queryLabeledContentList(test.feedType)
        } returns mockQueryMainContentListFlow(test.mockFeedList)
        every {
            getAudiocast(ContentSelected(test.mockPosition, test.mockContent))
        } returns mockGetAudiocast(test)
        every {
            getContentUri(test.mockContent.id, test.mockFilePath)
        } returns mockGetContentUri(test)
        coEvery { bitmapToByteArray(test.mockPreviewImageUrl) } returns mockBitmapToByteArray(test)

        // FirebaseRemoteConfig - Constant values
        mockkStatic(CONSTANTS_CLASS_COMPILED_JAVA)
        every { TTS_CHAR_LIMIT_ERROR } returns MOCK_TTS_CHAR_LIMIT_ERROR
        every { TTS_CHAR_LIMIT_ERROR_MESSAGE } returns MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE
        every { CONTENT_PLAY_ERROR } returns MOCK_CONTENT_PLAY_ERROR
    }

    private fun assertContentList(test: PlayContentTest) {
        feedViewModel.feedViewState().contentList.getOrAwaitValue().also { pagedList ->
            assertThat(pagedList).isEqualTo(test.mockFeedList)
        }
    }

    private fun assertContentSelected(test: PlayContentTest) {
        when (test.mockContent.contentType) {
            ARTICLE -> when (test.feedType) {
                MAIN, DISMISSED -> when (test.lceState) {
                    LOADING -> {
                        assertThat(feedViewModel.feedViewState().contentToPlay.observe()).isNull()
                        assertThat(feedViewModel.getContentLoadingStatus(test.mockContent.id))
                                .isEqualTo(VISIBLE)
                        assertThat(feedViewModel.viewEffects().notifyItemChanged.observe())
                                .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                    }
                    CONTENT -> {
                        assertThat(feedViewModel.feedViewState().contentToPlay.observe())
                                .isEqualTo(ContentToPlay(
                                        position = test.mockPosition,
                                        content = test.mockContent,
                                        filePath = MOCK_TXT_FILE_PATH,
                                        errorMessage = ""))
                        assertThat(feedViewModel.getContentLoadingStatus(test.mockContent.id))
                                .isEqualTo(GONE)
                        assertThat(feedViewModel.viewEffects().notifyItemChanged.observe())
                                .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                    }
                    ERROR -> {
                        assertThat(feedViewModel.feedViewState()
                                .contentToPlay.observe()).isNull()
                        assertThat(feedViewModel.viewEffects().notifyItemChanged.observe())
                                .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                        if (test.mockFilePath.equals(TTS_CHAR_LIMIT_ERROR))
                            assertThat(feedViewModel.viewEffects().snackBar.observe())
                                    .isEqualTo(SnackBarEffect(TTS_CHAR_LIMIT_ERROR_MESSAGE))
                        else assertThat(feedViewModel.viewEffects().snackBar.observe())
                                .isEqualTo(SnackBarEffect(MOCK_CONTENT_PLAY_ERROR))
                    }
                }
                SAVED -> HomeViewModel().also { homeViewModel ->
                    ContentToPlay(
                            position = test.mockPosition,
                            content = test.mockContent,
                            filePath = test.mockFilePath,
                            errorMessage = test.mockGetAudiocastError
                    ).also { mockContentToPlay ->
                        homeViewModel.setSavedContentToPlay(mockContentToPlay)
                        assertThat(homeViewModel.savedContentToPlay.observe())
                                .isEqualTo(mockContentToPlay)
                    }
                }
            }
            YOUTUBE -> when (test.feedType) {
                MAIN, DISMISSED -> {
                    assertThat(feedViewModel.feedViewState().contentToPlay.observe())
                            .isEqualTo(ContentToPlay(
                                    position = test.mockPosition,
                                    content = test.mockContent,
                                    filePath = test.mockFilePath,
                                    errorMessage = ""))
                    assertThat(feedViewModel.getContentLoadingStatus(test.mockContent.id))
                            .isEqualTo(GONE)
                    assertThat(feedViewModel.viewEffects().notifyItemChanged.observe())
                            .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                }
                SAVED -> {
                    HomeViewModel().apply {
                        ContentToPlay(
                                position = test.mockPosition,
                                content = test.mockContent,
                                filePath = test.mockFilePath,
                                errorMessage = test.mockGetAudiocastError
                        ).also { mockContentToPlay ->
                            this.setSavedContentToPlay(mockContentToPlay)
                            assertThat(this.savedContentToPlay.observe())
                                    .isEqualTo(mockContentToPlay)
                        }
                    }

                }
            }
        }
    }

    private fun assertAudioPlayerLoad(test: PlayContentTest) {
        when (test.lceState) {
            CONTENT -> {
                assertContentPlayer(test)
                assertContentPlayerChange(test)
            }
            ERROR -> assertContentPlayer(test)
        }
    }

    private fun assertContentPlayer(test: PlayContentTest) {
        assertThat(audioViewModel.playerViewState().contentPlayer.observe())
                .isEqualToComparingFieldByField(ContentPlayer(
                        uri = Uri.parse(""),
                        image = test.mockPreviewImageByteArray,
                        errorMessage = if (test.lceState != ERROR) "" else MOCK_GET_CONTENT_URI_ERROR
                ))
    }

    private fun assertContentPlayerChange(test: PlayContentTest) {
        audioViewModel.contentPlaying = Content()
        assertThat(audioViewModel.contentPlaying.id).isNotEqualTo(test.mockContent.id)
        audioViewModel.contentPlaying = test.mockContent
        assertThat(audioViewModel.contentPlaying.id).isEqualTo(test.mockContent.id)
    }


    private fun verifyTests(test: PlayContentTest) {
        coVerify {
            when (test.feedType) {
                MAIN -> getMainFeedList(test.isRealtime, any())
                SAVED, DISMISSED -> queryLabeledContentList(test.feedType)
            }
            if (test.mockContent.contentType == ARTICLE) {
                getAudiocast(ContentSelected(test.mockPosition, test.mockContent))
                if (test.lceState != LOADING) {
                    getContentUri(test.mockContent.id, test.mockFilePath)
                    bitmapToByteArray(test.mockPreviewImageUrl)
                }
            }
        }
        confirmVerified(FeedRepository)
    }
}
