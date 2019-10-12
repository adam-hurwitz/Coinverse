package app.coinverse.contentviewmodel.tests

import android.net.Uri
import android.view.View.GONE
import android.view.View.VISIBLE
import app.coinverse.content.ContentRepository
import app.coinverse.content.ContentRepository.bitmapToByteArray
import app.coinverse.content.ContentRepository.getAudiocast
import app.coinverse.content.ContentRepository.getContentUri
import app.coinverse.content.ContentRepository.getMainFeedList
import app.coinverse.content.ContentRepository.queryLabeledContentList
import app.coinverse.content.ContentViewModel
import app.coinverse.content.models.*
import app.coinverse.content.models.ContentViewEvent.*
import app.coinverse.contentviewmodel.*
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
class PlayContentTests {
    private val mainThreadSurrogate = newSingleThreadContext(UI_THREAD)
    private val contentViewModel = ContentViewModel()

    private fun PlayContent() = playContentTestCases()

    @BeforeAll
    fun beforeAll() {

        // Android libraries
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseRemoteConfig::class)
        mockkStatic(Crashlytics::class)
        mockkStatic(Uri::class)

        // Coinverse
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
    @MethodSource("PlayContent")
    fun `Play Content`(test: PlayContentTest) = runBlocking {
        mockComponents(test)
        FeedLoad(test.feedType, test.timeframe, false).also { event ->
            contentViewModel.processEvent(event)
        }
        ContentSelected(test.mockPosition, test.mockContent).also { event ->
            contentViewModel.processEvent(event)
            assertContentSelected(test)
        }
        if (test.mockContent.contentType == ARTICLE)
            AudioPlayerLoad(test.mockContent.id, test.mockFilePath, test.mockPreviewImageUrl)
                    .also { event ->
                        contentViewModel.processEvent(event)
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
        every { getMainFeedList(test.isRealtime, any()) } returns mockGetMainFeedList(
                test.mockFeedList, CONTENT)
        every {
            queryLabeledContentList(test.feedType)
        } returns mockQueryMainContentList(test.mockFeedList)
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

    private fun assertContentSelected(test: PlayContentTest) {
        when (test.mockContent.contentType) {
            ARTICLE -> when (test.feedType) {
                MAIN, DISMISSED -> when (test.lceState) {
                    LOADING -> {
                        assertThat(contentViewModel.feedViewState().contentToPlay
                                .observe()).isNull()
                        assertThat(contentViewModel.getContentLoadingStatus(test.mockContent.id))
                                .isEqualTo(VISIBLE)
                        assertThat(contentViewModel.viewEffects().notifyItemChanged.observe())
                                .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                    }
                    CONTENT -> {
                        assertThat(contentViewModel.feedViewState().contentToPlay.observe())
                                .isEqualTo(ContentToPlay(
                                        position = test.mockPosition,
                                        content = test.mockContent,
                                        filePath = MOCK_TXT_FILE_PATH,
                                        errorMessage = ""))
                        assertThat(contentViewModel.getContentLoadingStatus(test.mockContent.id))
                                .isEqualTo(GONE)
                        assertThat(contentViewModel.viewEffects().notifyItemChanged.observe())
                                .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                    }
                    ERROR -> {
                        assertThat(contentViewModel.feedViewState()
                                .contentToPlay.observe()).isNull()
                        assertThat(contentViewModel.viewEffects().notifyItemChanged.observe())
                                .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                        if (test.mockFilePath.equals(TTS_CHAR_LIMIT_ERROR))
                            assertThat(contentViewModel.viewEffects().snackBar.observe())
                                    .isEqualTo(SnackBarEffect(TTS_CHAR_LIMIT_ERROR_MESSAGE))
                        else assertThat(contentViewModel.viewEffects().snackBar.observe())
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
                    assertThat(contentViewModel.feedViewState().contentToPlay.observe())
                            .isEqualTo(ContentToPlay(
                                    position = test.mockPosition,
                                    content = test.mockContent,
                                    filePath = test.mockFilePath,
                                    errorMessage = ""))
                    assertThat(contentViewModel.getContentLoadingStatus(test.mockContent.id))
                            .isEqualTo(GONE)
                    assertThat(contentViewModel.viewEffects().notifyItemChanged.observe())
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
        assertThat(contentViewModel.playerViewState().contentPlayer.observe())
                .isEqualToComparingFieldByField(ContentPlayer(
                        uri = Uri.parse(""),
                        image = test.mockPreviewImageByteArray,
                        errorMessage = if (test.lceState != ERROR) "" else MOCK_GET_CONTENT_URI_ERROR
                ))
    }

    private fun assertContentPlayerChange(test: PlayContentTest) {
        contentViewModel.contentPlaying = Content()
        assertThat(contentViewModel.contentPlaying.id).isNotEqualTo(test.mockContent.id)
        contentViewModel.contentPlaying = test.mockContent
        assertThat(contentViewModel.contentPlaying.id).isEqualTo(test.mockContent.id)
    }


    private fun verifyTests(test: PlayContentTest) {
        coVerify {
            when (test.feedType) {
                MAIN -> getMainFeedList(test.isRealtime, any())
                SAVED, DISMISSED -> queryLabeledContentList(test.feedType)
            }
            if (test.mockContent.contentType == ARTICLE) {
                getAudiocast(ContentSelected(test.mockPosition, test.mockContent))
                getContentUri(test.mockContent.id, test.mockFilePath)
                if (test.lceState != LOADING) bitmapToByteArray(test.mockPreviewImageUrl)
            }
        }
        confirmVerified(ContentRepository)
    }
}
