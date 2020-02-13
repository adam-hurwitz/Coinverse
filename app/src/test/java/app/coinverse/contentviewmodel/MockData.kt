package app.coinverse.contentviewmodel

import android.net.Uri
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import app.coinverse.feed.models.Content
import app.coinverse.feed.models.ContentPlayer
import app.coinverse.feed.models.ContentToPlay
import app.coinverse.utils.*
import app.coinverse.utils.ContentType.ARTICLE
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.Resource.Companion.error
import app.coinverse.utils.Resource.Companion.loading
import app.coinverse.utils.Resource.Companion.success
import app.coinverse.utils.Status.*
import kotlinx.coroutines.flow.flow

val mockArticleContent = Content(id = "1", contentType = ARTICLE, url = MOCK_URL)
val mockYouTubeContent = Content(id = "1", contentType = YOUTUBE, url = MOCK_URL)
val mockDbContentListForDay = listOf(Content(id = "1"), Content(id = "2"),
        Content(id = "3"))
val mockDbContentListForAll = listOf(Content(id = "1"), Content(id = "2"),
        Content(id = "3"), Content(id = "4"), Content(id = "5"), Content(id = "6"))

fun mockGetMainFeedList(mockFeedList: List<Content>, status: Status) = flow {
    when (status) {
        LOADING -> emit(loading(null))
        SUCCESS -> emit(success(mockQueryMainContentListLiveData(mockFeedList)))
        ERROR -> emit(error(MOCK_GET_MAIN_FEED_LIST_ERROR, null))
    }
}

fun mockQueryMainContentListFlow(mockFeedList: List<Content>) = flow {
    emit(mockFeedList.asPagedList())
}


fun mockQueryMainContentListLiveData(mockFeedList: List<Content>) = liveData {
    emit(mockFeedList.asPagedList())
}.asFlow()

fun mockGetAudiocast(test: PlayContentTest) = flow {
    when (test.status) {
        LOADING -> emit(loading(null))
        SUCCESS -> emit(success(ContentToPlay(
                position = test.mockPosition,
                content = test.mockContent,
                filePath = test.mockFilePath)))
        ERROR -> emit(error(test.mockGetAudiocastError, null))
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
