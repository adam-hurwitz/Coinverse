package app.coinverse.contentviewmodel

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import app.coinverse.content.models.*
import app.coinverse.utils.*
import app.coinverse.utils.ContentType.ARTICLE
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.LCE_STATE.*
import app.coinverse.utils.livedata.Event
import app.coinverse.utils.models.Lce
import app.coinverse.utils.models.Lce.Error
import app.coinverse.utils.models.Lce.Loading
import kotlinx.coroutines.flow.flow

val mockArticleContent = Content(id = "1", contentType = ARTICLE, url = MOCK_URL)
val mockYouTubeContent = Content(id = "1", contentType = YOUTUBE, url = MOCK_URL)
val mockDbContentListForDay = listOf(Content(id = "1"), Content(id = "2"),
        Content(id = "3"))
val mockDbContentListForAll = listOf(Content(id = "1"), Content(id = "2"),
        Content(id = "3"), Content(id = "4"), Content(id = "5"), Content(id = "6"))

fun mockGetMainFeedList(mockFeedList: List<Content>, lceState: LCE_STATE) = flow {
    when (lceState) {
        LOADING -> emit(Loading())
        CONTENT -> emit(Lce.Content(PagedListResult(
                pagedList = mockQueryMainContentListLiveData(mockFeedList),
                errorMessage = "")))
        ERROR -> emit(Error(PagedListResult(
                pagedList = null,
                errorMessage = MOCK_GET_MAIN_FEED_LIST_ERROR)))
    }
}

fun mockQueryMainContentListFlow(mockFeedList: List<Content>) = flow {
    emit(mockFeedList.asPagedList())
}

fun mockQueryMainContentListLiveData(mockFeedList: List<Content>) = liveData {
    emit(mockFeedList.asPagedList())
}

fun mockGetAudiocast(test: PlayContentTest) = flow {
    when (test.lceState) {
        LOADING -> emit(Loading())
        CONTENT -> emit(Lce.Content(ContentToPlay(
                position = test.mockPosition,
                content = test.mockContent,
                filePath = test.mockFilePath,
                errorMessage = "")))
        ERROR -> emit(Error(ContentToPlay(
                position = test.mockPosition,
                content = test.mockContent,
                filePath = test.mockFilePath,
                errorMessage = test.mockGetAudiocastError)))
    }
}

fun mockGetContentUri(test: PlayContentTest) = flow {
    when (test.lceState) {
        LOADING -> emit(Loading())
        CONTENT -> emit(Lce.Content(ContentPlayer(
                uri = Uri.parse(""),
                image = test.mockPreviewImageByteArray,
                errorMessage = "")))
        ERROR -> emit(Error(ContentPlayer(
                uri = Uri.parse(""),
                image = ByteArray(0),
                errorMessage = MOCK_GET_CONTENT_URI_ERROR)))
    }
}

fun mockBitmapToByteArray(test: PlayContentTest) = flow {
    when (test.lceState) {
        LOADING -> emit(Loading())
        CONTENT -> emit(Lce.Content(ContentBitmap(
                image = test.mockPreviewImageByteArray,
                errorMessage = "")))
        ERROR -> emit(Error(ContentBitmap(
                image = ByteArray(0),
                errorMessage = MOCK_GET_BITMAP_TO_BYTEARRAY_ERROR)))
    }
}

fun mockEditContentLabels(test: LabelContentTest) = flow {
    emit(when (test.lceState) {
        LOADING -> Loading()
        CONTENT -> Lce.Content(ContentLabeled(test.adapterPosition, ""))
        ERROR -> Error(ContentLabeled(test.adapterPosition, MOCK_CONTENT_LABEL_ERROR))
    })
}

fun mockGetContent(test: NavigateContentTest) =
        MutableLiveData<Event<Content>>().apply { value = Event(test.mockContent) }
