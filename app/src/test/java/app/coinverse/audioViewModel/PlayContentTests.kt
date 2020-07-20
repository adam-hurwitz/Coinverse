package app.coinverse.audioViewModel

// Todo: Refactor with Model-View-Intent.

/*@ExperimentalCoroutinesApi
@ExtendWith(ContentTestExtension::class)
class PlayContentTests constructor(
        val testDispatcher: TestCoroutineDispatcher,
        val testScope: TestCoroutineScope
) {

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
                coroutineScopeProvider = testScope,
                feedType = test.feedType,
                timeframe = test.timeframe,
                isRealtime = test.isRealtime,
                repository = feedRepository,
                analytics = analytics)
        audioViewModel = AudioViewModel(repository = contentRepository)
        assertContentList(test)
        ContentSelected(test.mockContent, test.mockPosition).also { event ->
            feedViewModel.selectContent(event)
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
                        assertThat(feedViewModel.effect.notifyItemChanged.getOrAwaitValue())
                                .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                        assertThat(feedViewModel.getContentLoadingStatus(test.mockContent.id))
                                .isEqualTo(VISIBLE)
                    }
                    SUCCESS -> {
                        assertThat(feedViewModel.state.contentToPlay.getOrAwaitValue()).isEqualTo(OpenContent(
                                position = test.mockPosition,
                                content = test.mockContent,
                                filePath = MOCK_TXT_FILE_PATH))
                        assertThat(feedViewModel.getContentLoadingStatus(test.mockContent.id))
                                .isEqualTo(GONE)
                        assertThat(feedViewModel.effect.notifyItemChanged.getOrAwaitValue())
                                .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                    }
                    ERROR -> {
                        assertThat(feedViewModel.effect.notifyItemChanged.getOrAwaitValue())
                                .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                        if (test.mockGetAudiocastError.equals(TTS_CHAR_LIMIT_ERROR))
                            assertThat(feedViewModel.effect.snackBar.getOrAwaitValue())
                                    .isEqualTo(SnackBarEffect(TTS_CHAR_LIMIT_ERROR_MESSAGE))
                        else assertThat(feedViewModel.effect.snackBar.getOrAwaitValue())
                                .isEqualTo(SnackBarEffect(MOCK_CONTENT_PLAY_ERROR))
                    }
                }
                SAVED -> HomeViewModel().also { homeViewModel ->
                    OpenContent(
                            position = test.mockPosition,
                            content = test.mockContent,
                            filePath = test.mockFilePath
                    ).also { mockContentToPlay ->
                        homeViewModel.setOpenFromSave(mockContentToPlay)
                        assertThat(homeViewModel.openContentFromSave.getOrAwaitValue())
                                .isEqualTo(mockContentToPlay)
                    }
                }
            }
            YOUTUBE -> when (test.feedType) {
                MAIN, DISMISSED -> {
                    assertThat(feedViewModel.state.contentToPlay.getOrAwaitValue()).isEqualTo(OpenContent(
                            position = test.mockPosition,
                            content = test.mockContent,
                            filePath = test.mockFilePath))
                    assertThat(feedViewModel.getContentLoadingStatus(test.mockContent.id)).isEqualTo(GONE)
                    assertThat(feedViewModel.effect.notifyItemChanged.getOrAwaitValue())
                            .isEqualTo(NotifyItemChangedEffect(test.mockPosition))
                }
                SAVED -> {
                    HomeViewModel().apply {
                        OpenContent(
                                position = test.mockPosition,
                                content = test.mockContent,
                                filePath = test.mockFilePath
                        ).also { mockContentToPlay ->
                            this.setOpenFromSave(mockContentToPlay)
                            assertThat(this.openContentFromSave.getOrAwaitValue()).isEqualTo(mockContentToPlay)
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
}*/
