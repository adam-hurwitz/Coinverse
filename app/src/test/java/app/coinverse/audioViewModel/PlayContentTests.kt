package app.coinverse.audioViewModel

import android.net.Uri
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.SavedStateHandle
import app.coinverse.analytics.Analytics
import app.coinverse.content.AudioViewEventType.AudioPlayerLoad
import app.coinverse.content.ContentRepository
import app.coinverse.content.viewmodel.AudioViewModel
import app.coinverse.feedViewModel.*
import app.coinverse.feed.FeedRepository
import app.coinverse.feed.models.Content
import app.coinverse.feed.models.ContentPlayer
import app.coinverse.feed.models.ContentToPlay
import app.coinverse.feed.models.FeedViewEffectType.NotifyItemChangedEffect
import app.coinverse.feed.models.FeedViewEffectType.SnackBarEffect
import app.coinverse.feed.models.FeedViewEventType.ContentSelected
import app.coinverse.feed.viewmodel.FeedViewModel
import app.coinverse.home.HomeViewModel
import app.coinverse.utils.*
import app.coinverse.utils.ContentType.ARTICLE
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.Status.*
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
class PlayContentTests(val testDispatcher: TestCoroutineDispatcher) {

    private fun PlayContent() = playContentTestCases()
    private val feedRepository = mockkClass(FeedRepository::class)
    private val contentRepository = mockkClass(ContentRepository::class)
    private val analytics = mockkClass(Analytics::class)
    private lateinit var feedViewModel: FeedViewModel
    private lateinit var audioViewModel: AudioViewModel

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
        feedViewModel = FeedViewModel(
                repository = feedRepository,
                analytics = analytics,
                feedType = test.feedType,
                timeframe = test.timeframe,
                isRealtime = test.isRealtime)
        audioViewModel = AudioViewModel(repository = contentRepository)
        assertContentList(test)
        ContentSelected(test.mockContent, test.mockPosition).also { event ->
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
            feedRepository.getMainFeedNetwork(test.isRealtime, any())
        } returns mockGetMainFeedList(test.mockFeedList, SUCCESS)
        every {
            feedRepository.getLabeledFeedRoom(test.feedType)
        } returns mockQueryMainContentListFlow(test.mockFeedList)
        every {
            feedRepository.getAudiocast(ContentSelected(test.mockContent, test.mockPosition))
        } returns mockGetAudiocast(test)
        every {
            contentRepository.getContentUri(test.mockContent.id, test.mockFilePath)
        } returns mockGetContentUri(test)
        coEvery { contentRepository.bitmapToByteArray(test.mockPreviewImageUrl) } returns mockBitmapToByteArray(test)

        // FirebaseRemoteConfig - Constant values
        mockkStatic(CONSTANTS_CLASS_COMPILED_JAVA)
        every { TTS_CHAR_LIMIT_ERROR } returns MOCK_TTS_CHAR_LIMIT_ERROR
        every { TTS_CHAR_LIMIT_ERROR_MESSAGE } returns MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE
        every { CONTENT_PLAY_ERROR } returns MOCK_CONTENT_PLAY_ERROR
    }

    private fun assertContentList(test: PlayContentTest) {
        feedViewModel.state.feedList.getOrAwaitValue().also { pagedList ->
            assertThat(pagedList).isEqualTo(test.mockFeedList)
        }
    }

    private fun assertContentSelected(test: PlayContentTest) {
        when (test.mockContent.contentType) {
            ARTICLE -> when (test.feedType) {
                MAIN, DISMISSED -> when (test.status) {
                    LOADING -> {
                        assertThat(feedViewModel.getContentLoadingStatus(test.mockContent.id))
                                .isEqualTo(VISIBLE)
                        assertThat(feedViewModel.effects.notifyItemChanged.getOrAwaitValue())
                                .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                    }
                    SUCCESS -> {
                        assertThat(feedViewModel.state.contentToPlay.getOrAwaitValue()).isEqualTo(ContentToPlay(
                                position = test.mockPosition,
                                content = test.mockContent,
                                filePath = MOCK_TXT_FILE_PATH))
                        assertThat(feedViewModel.getContentLoadingStatus(test.mockContent.id))
                                .isEqualTo(GONE)
                        assertThat(feedViewModel.effects.notifyItemChanged.getOrAwaitValue())
                                .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                    }
                    ERROR -> {
                        assertThat(feedViewModel.effects.notifyItemChanged.getOrAwaitValue())
                                .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                        if (test.mockGetAudiocastError.equals(TTS_CHAR_LIMIT_ERROR))
                            assertThat(feedViewModel.effects.snackBar.getOrAwaitValue())
                                    .isEqualTo(SnackBarEffect(TTS_CHAR_LIMIT_ERROR_MESSAGE))
                        else assertThat(feedViewModel.effects.snackBar.getOrAwaitValue())
                                .isEqualTo(SnackBarEffect(MOCK_CONTENT_PLAY_ERROR))
                    }
                }
                SAVED -> HomeViewModel().also { homeViewModel ->
                    ContentToPlay(
                            position = test.mockPosition,
                            content = test.mockContent,
                            filePath = test.mockFilePath
                    ).also { mockContentToPlay ->
                        homeViewModel.setSavedContentToPlay(mockContentToPlay)
                        assertThat(homeViewModel.savedContentToPlay.getOrAwaitValue())
                                .isEqualTo(mockContentToPlay)
                    }
                }
            }
            YOUTUBE -> when (test.feedType) {
                MAIN, DISMISSED -> {
                    assertThat(feedViewModel.state.contentToPlay.getOrAwaitValue()).isEqualTo(ContentToPlay(
                            position = test.mockPosition,
                            content = test.mockContent,
                            filePath = test.mockFilePath))
                    assertThat(feedViewModel.getContentLoadingStatus(test.mockContent.id)).isEqualTo(GONE)
                    assertThat(feedViewModel.effects.notifyItemChanged.getOrAwaitValue())
                            .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                }
                SAVED -> {
                    HomeViewModel().apply {
                        ContentToPlay(
                                position = test.mockPosition,
                                content = test.mockContent,
                                filePath = test.mockFilePath
                        ).also { mockContentToPlay ->
                            this.setSavedContentToPlay(mockContentToPlay)
                            assertThat(this.savedContentToPlay.getOrAwaitValue()).isEqualTo(mockContentToPlay)
                        }
                    }

                }
            }
        }
    }

    private fun assertAudioPlayerLoad(test: PlayContentTest) {
        if (test.status == SUCCESS) {
            assertContentPlayer(test)
            assertContentPlayerChange(test)
        }
    }

    private fun assertContentPlayer(test: PlayContentTest) {
        assertThat(audioViewModel.contentPlayer.getOrAwaitValue()).isEqualToComparingFieldByField(
                ContentPlayer(uri = Uri.parse(""), image = test.mockPreviewImageByteArray))
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
                MAIN -> feedRepository.getMainFeedNetwork(test.isRealtime, any())
                SAVED, DISMISSED -> feedRepository.getLabeledFeedRoom(test.feedType)
            }
            if (test.mockContent.contentType == ARTICLE) {
                feedRepository.getAudiocast(ContentSelected(test.mockContent, test.mockPosition))
                if (test.status != LOADING && test.status != ERROR) {
                    contentRepository.getContentUri(test.mockContent.id, test.mockFilePath)
                    contentRepository.bitmapToByteArray(test.mockPreviewImageUrl)
                }
            }
        }
        confirmVerified(feedRepository)
        confirmVerified(contentRepository)
    }
}