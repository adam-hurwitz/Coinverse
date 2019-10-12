package app.coinverse.contentviewmodel

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.paging.PagedList
import app.coinverse.content.models.*
import app.coinverse.utils.ContentType.ARTICLE
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.LCE_STATE
import app.coinverse.utils.LCE_STATE.*
import app.coinverse.utils.asPagedList
import app.coinverse.utils.livedata.Event
import app.coinverse.utils.models.Lce
import app.coinverse.utils.models.Lce.Error
import app.coinverse.utils.models.Lce.Loading

// Constants
const val UI_THREAD = "UI thread"
const val CONSTANTS_CLASS_COMPILED_JAVA = "app.coinverse.utils.ConstantsKt"
const val MOCK_CONTENT_REQUEST_NETWORK_ERROR = "Unable to update feed. Swipe to refresh to try again!"
const val MOCK_CONTENT_REQUEST_SWIPE_TO_REFRESH_ERROR = "Unable to update feed. Please try again later!"
const val MOCK_GET_MAIN_FEED_LIST_ERROR = "Unit test getEffect PagedList result error."
const val MOCK_TXT_FILE_PATH = "mock/sample/textFile.txt"
const val MOCK_PREVIEW_IMAGE = "mockPreviewImage.jpg"
const val MOCK_URL = "https://mockUrl.com"
// Mock Errors
const val MOCK_TTS_CHAR_LIMIT_ERROR = "TTS_CHAR_LIMIT_ERROR"
const val MOCK_TTS_CHAR_LIMIT_ERROR_MESSAGE = "Audiocasts for longer content coming soon!"
const val MOCK_CONTENT_PLAY_ERROR = "Unable to play content. Please try again later."
const val MOCK_GET_CONTENT_URI_ERROR = "Mock getContentUri error."
const val MOCK_GET_BITMAP_TO_BYTEARRAY_ERROR = "Mock getBitmapToByteArray error."
const val MOCK_CONTENT_LABEL_ERROR = "Unable to apply label. Please try again later."

val mockArticleContent = Content(id = "1", contentType = ARTICLE, url = MOCK_URL)
val mockYouTubeContent = Content(id = "1", contentType = YOUTUBE, url = MOCK_URL)
val mockDbContentListForDay = listOf(Content(id = "1"), Content(id = "2"),
        Content(id = "3"))
val mockDbContentListForAll = listOf(Content(id = "1"), Content(id = "2"),
        Content(id = "3"), Content(id = "4"), Content(id = "5"), Content(id = "6"))

fun mockGetMainFeedList(mockFeedList: List<Content>, lceState: LCE_STATE) =
        MutableLiveData<Lce<PagedListResult>>().also { lce ->
            when (lceState) {
                LOADING -> lce.value = Loading()
                CONTENT -> lce.value = Lce.Content(PagedListResult(
                        pagedList = mockQueryMainContentList(mockFeedList),
                        errorMessage = ""))
                ERROR -> lce.value = Error(PagedListResult(
                        pagedList = null,
                        errorMessage = MOCK_GET_MAIN_FEED_LIST_ERROR))
            }
        }

fun mockQueryMainContentList(mockFeedList: List<Content>) =
        MutableLiveData<PagedList<Content>>().also { pagedList ->
            pagedList.value = mockFeedList.asPagedList(PagedList.Config.Builder()
                    .setEnablePlaceholders(false)
                    .setPrefetchDistance(24)
                    .setPageSize(12)
                    .build())
        }

fun mockGetAudiocast(test: PlayContentTest) =
        MutableLiveData<Lce<ContentToPlay>>().also { lce ->
            when (test.lceState) {
                LOADING -> lce.value = Loading()
                CONTENT -> lce.value = Lce.Content(ContentToPlay(
                        position = test.mockPosition,
                        content = test.mockContent,
                        filePath = test.mockFilePath,
                        errorMessage = ""
                ))
                ERROR -> lce.value = Error(ContentToPlay(
                        position = test.mockPosition,
                        content = test.mockContent,
                        filePath = test.mockFilePath,
                        errorMessage = test.mockGetAudiocastError))
            }
        }

fun mockGetContentUri(test: PlayContentTest) =
        MutableLiveData<Lce<ContentPlayer>>().apply {
            when (test.lceState) {
                LOADING -> value = Loading()
                CONTENT -> value = Lce.Content(ContentPlayer(
                        uri = Uri.parse(""),
                        image = test.mockPreviewImageByteArray,
                        errorMessage = ""))
                ERROR -> value = Error(ContentPlayer(
                        uri = Uri.parse(""),
                        image = ByteArray(0),
                        errorMessage = MOCK_GET_CONTENT_URI_ERROR))
            }
        }

fun mockBitmapToByteArray(test: PlayContentTest) =
        MutableLiveData<Lce<ContentBitmap>>().apply {
            when (test.lceState) {
                LOADING -> value = Loading()
                CONTENT -> value = Lce.Content(ContentBitmap(
                        image = test.mockPreviewImageByteArray,
                        errorMessage = ""))
                ERROR -> value = Error(ContentBitmap(
                        image = ByteArray(0),
                        errorMessage = MOCK_GET_BITMAP_TO_BYTEARRAY_ERROR))
            }
        }

fun mockEditContentLabels(test: LabelContentTest) = liveData {
    emit(when (test.lceState) {
        LOADING -> Loading()
        CONTENT -> Lce.Content(ContentLabeled(test.adapterPosition, ""))
        ERROR -> Error(ContentLabeled(test.adapterPosition, MOCK_CONTENT_LABEL_ERROR))
    })
}

fun mockGetContent(test: NavigateContentTest) =
        MutableLiveData<Event<Content>>().apply { value = Event(test.mockContent) }
