package app.coinverse.feedViewModel

import android.net.Uri
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import app.coinverse.content.ContentPlayer
import app.coinverse.feed.Content
import app.coinverse.feed.state.FeedViewState.OpenContent
import app.coinverse.utils.ContentType.ARTICLE
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.MOCK_CONTENT_LABEL_ERROR
import app.coinverse.utils.MOCK_GET_BITMAP_TO_BYTEARRAY_ERROR
import app.coinverse.utils.MOCK_GET_CONTENT_URI_ERROR
import app.coinverse.utils.MOCK_GET_MAIN_FEED_ERROR
import app.coinverse.utils.MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE
import app.coinverse.utils.MOCK_TXT_FILE_PATH
import app.coinverse.utils.MOCK_URL
import app.coinverse.utils.Resource.Companion.error
import app.coinverse.utils.Resource.Companion.loading
import app.coinverse.utils.Resource.Companion.success
import app.coinverse.utils.Status
import app.coinverse.utils.Status.ERROR
import app.coinverse.utils.Status.LOADING
import app.coinverse.utils.Status.SUCCESS
import app.coinverse.utils.asPagedList
import kotlinx.coroutines.flow.flow

val mockContentId = "123"
val mockArticleContent = Content(id = mockContentId, contentType = ARTICLE, url = MOCK_URL)
val mockYouTubeContent = Content(id = mockContentId, contentType = YOUTUBE, url = MOCK_URL)
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

fun mockGetAudiocast(test: FeedViewTest) = flow {
    // Todo: YT
    val content = if (test.openContent!!.content.contentType == ARTICLE)
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

fun mockGetContentUri(test: PlayContentTest) = flow {
    when (test.status) {
        LOADING -> emit(loading(null))
        SUCCESS -> emit(success(ContentPlayer(
                uri = Uri.parse(""),
                image = test.mockPreviewImageByteArray)))
        ERROR -> emit(error(
                MOCK_GET_CONTENT_URI_ERROR,
                ContentPlayer(uri = Uri.parse(""), image = ByteArray(0))))
    }
}

fun mockBitmapToByteArray(test: PlayContentTest) = flow {
    when (test.status) {
        LOADING -> emit(loading(null))
        SUCCESS -> emit(success(test.mockPreviewImageByteArray))
        ERROR -> emit(error(MOCK_GET_BITMAP_TO_BYTEARRAY_ERROR, ByteArray(0)))
    }
}

fun mockEditContentLabels(test: LabelContentTest) = flow {
    emit(when (test.status) {
        LOADING -> loading(null)
        SUCCESS -> success(test.adapterPosition)
        ERROR -> error(MOCK_CONTENT_LABEL_ERROR, null)
    })
}

fun mockGetContent(test: NavigateContentTest) = liveData { emit(test.mockContent) }
