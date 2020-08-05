package app.coinverse.utils

import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import app.coinverse.feed.Content
import app.coinverse.feed.state.FeedViewState.OpenContent
import app.coinverse.feedViewModel.FeedViewTestCase
import app.coinverse.utils.ContentType.ARTICLE
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.Resource.Companion.error
import app.coinverse.utils.Resource.Companion.loading
import app.coinverse.utils.Resource.Companion.success
import app.coinverse.utils.Status.ERROR
import app.coinverse.utils.Status.LOADING
import app.coinverse.utils.Status.SUCCESS
import kotlinx.coroutines.flow.flow

val mockContentId = "123"
val mockArticleContent = Content(id = mockContentId, contentType = ARTICLE, url = MOCK_AUDIO_URL)
val mockYouTubeContent = Content(id = mockContentId, contentType = YOUTUBE, url = MOCK_YOUTUBE_URL)
val mockFeedPosition = 1
val mockNetworkContentListForDay = listOf(
        Content(id = "1"),
        Content(id = "2"),
        Content(id = "3"))
val mockDbContentListForDay = listOf(
        Content(id = "4"),
        Content(id = "5"),
        Content(id = "6"))
val mockDbContentListForAll = listOf(
        Content(id = "1"),
        Content(id = "2"),
        Content(id = "3"),
        Content(id = "4"),
        Content(id = "5"),
        Content(id = "6"))

fun mockGetMainFeedList(status: Status, mockFeedList: List<Content>) = flow {
    when (status) {
        LOADING -> emit(loading(mockFeedList.asPagedList()))
        SUCCESS -> emit(success(mockFeedList.asPagedList()))
        ERROR -> emit(error(MOCK_GET_MAIN_FEED_ERROR, mockFeedList.asPagedList()))
    }
}

fun mockGetMainFeedRoom(mockFeedList: List<Content>) = liveData {
    emit(mockFeedList.asPagedList())
}.asFlow()

fun mockGetAudiocast(test: FeedViewTestCase) = flow {
    val content = if (test.openContentState!!.content.contentType == ARTICLE)
        mockArticleContent else mockYouTubeContent
    when (test.status) {
        LOADING -> emit(loading(null))
        SUCCESS -> emit(success(OpenContent(
                position = mockFeedPosition,
                contentId = mockContentId,
                content = content,
                filePath = MOCK_TXT_FILE_PATH
        )))
        ERROR -> emit(error(MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE, null))
    }
}

fun mockEditContentLabels(test: FeedViewTestCase) = flow {
    emit(when (test.status) {
        LOADING -> loading(null)
        SUCCESS -> success(test.swipeContentIntent!!.position)
        ERROR -> error(MOCK_CONTENT_LABEL_ERROR, null)
    })
}
