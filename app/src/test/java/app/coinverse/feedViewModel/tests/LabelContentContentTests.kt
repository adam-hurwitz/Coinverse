package app.coinverse.feedViewModel.tests

// Todo: Refactor with Model-View-Intent.

/*@ExperimentalCoroutinesApi
@ExtendWith(ContentTestExtension::class)
class LabelContentContentTests(
        val testDispatcher: TestCoroutineDispatcher,
        val testScope: TestCoroutineScope
) {

    private fun LabelContent() = labelContentTestCases()
    private val repository = mockkClass(FeedRepository::class)
    private val analytics = mockkClass(Analytics::class)
    private lateinit var feedViewModel: FeedViewModel

    @BeforeAll
    fun beforeAll() {

        // Android libraries
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseRemoteConfig::class)
        mockkStatic(Crashlytics::class)
    }

    @ParameterizedTest
    @MethodSource("LabelContent")
    fun `Label Content`(test: LabelContentTest) = testDispatcher.runBlockingTest {
        mockComponents(test)
        feedViewModel = FeedViewModel(
                coroutineScopeProvider = testScope,
                feedType = test.feedType,
                timeframe = test.timeframe,
                isRealtime = test.isRealtime,
                repository = repository,
                analytics = analytics)
        assertContentList(test)
        ContentSwipeDrawed(test.isDrawed).also { event ->
            feedViewModel.contentSwipeDrawed(event)
            assertEnableSwipeToRefresh()
        }
        ContentSwiped(test.feedType, test.actionType, test.adapterPosition).also { event ->
            feedViewModel.swipeContent(event)
            assertContentLabeled(test)
        }
        verifyTests(test)
    }

    private fun assertContentList(test: LabelContentTest) {
        feedViewModel.state.feedList.getOrAwaitValue().also { pagedList ->
            assertThat(pagedList).isEqualTo(test.mockFeedList)
        }
    }

    private fun assertEnableSwipeToRefresh() {
        HomeViewModel().apply {
            disableSwipeToRefresh(feedViewModel.effect.enableSwipeToRefresh.getOrAwaitValue().isEnabled)
            assertThat(disableSwipeToRefresh.getOrAwaitValue()).isEqualTo(false)
        }
    }

    private fun assertContentLabeled(test: LabelContentTest) {
        feedViewModel.effect.contentSwiped.getOrAwaitValue().also { contentSwipedEffect ->
            assertThat(contentSwipedEffect).isEqualTo(ContentSwipedEffect(
                    feedType = test.feedType,
                    actionType = test.actionType,
                    position = test.adapterPosition))
            LabelContent(
                    feedType = contentSwipedEffect.feedType,
                    actionType = contentSwipedEffect.actionType,
                    user = test.mockUser(),
                    position = contentSwipedEffect.position,
                    content = test.mockContent,
                    isMainFeedEmptied = false).also { event ->
                feedViewModel.labelContent(event)
                if (test.isUserSignedIn) {
                    when (test.status) {
                        SUCCESS -> {
                            assertThat(feedViewModel.state.contentLabeledPosition.getOrAwaitValue())
                                    .isEqualTo(test.adapterPosition)
                        }
                        ERROR -> {
                            assertThat(feedViewModel.effect.snackBar.getOrAwaitValue())
                                    .isEqualTo(SnackBarEffect(text = MOCK_CONTENT_LABEL_ERROR))
                        }
                    }
                } else {
                    assertThat(feedViewModel.effect.notifyItemChanged.getOrAwaitValue())
                            .isEqualTo(NotifyItemChangedEffect(test.adapterPosition))
                    assertThat(feedViewModel.effect.signIn.getOrAwaitValue()).isEqualTo(SignInEffect(true))
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
        coEvery {
            repository.getMainFeedNetwork(test.isRealtime, any())
        } returns mockGetMainFeedList(test.mockFeedList, SUCCESS)
        every {
            repository.editContentLabels(test.feedType, test.actionType, test.mockContent, any(),
                    test.adapterPosition)
        } returns mockEditContentLabels(test)
        every {
            analytics.labelContentFirebaseAnalytics(test.mockContent)
        } returns mockk(relaxed = true)
        every {
            analytics.updateActionAnalytics(test.actionType, test.mockContent, any())
        } returns mockk(relaxed = true)
        every {
            repository.getLabeledFeedRoom(test.feedType)
        } returns mockQueryMainContentListFlow(test.mockFeedList)

        // FirebaseRemoteConfig - Constant values
        mockkStatic(CONSTANTS_CLASS_COMPILED_JAVA)
        every { CONTENT_LABEL_ERROR } returns MOCK_CONTENT_LABEL_ERROR
    }

    private fun verifyTests(test: LabelContentTest) {
        coVerify {
            if (test.isUserSignedIn)
                repository.editContentLabels(test.feedType, test.actionType, test.mockContent, any(),
                        test.adapterPosition)
            when (test.feedType) {
                MAIN -> repository.getMainFeedNetwork(test.isRealtime, any())
                SAVED, DISMISSED -> repository.getLabeledFeedRoom(test.feedType)
            }
        }
        confirmVerified(repository)
    }
}*/
