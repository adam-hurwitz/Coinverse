package app.coinverse.content.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import app.coinverse.content.AudioFragment
import app.coinverse.content.AudioViewEventType.AudioPlayerLoad
import app.coinverse.content.AudioViewEvents
import app.coinverse.content.ContentPlayer
import app.coinverse.content.ContentRepository
import app.coinverse.feed.Content
import app.coinverse.utils.Resource.Companion.error
import app.coinverse.utils.Resource.Companion.success
import app.coinverse.utils.Status.ERROR
import app.coinverse.utils.Status.SUCCESS
import com.crashlytics.android.Crashlytics
import kotlinx.coroutines.flow.collect

/**
 * Todo: Refactor with Model-View-Intent.
 * See [app.coinverse.feed.FeedViewModel].
 **/
class AudioViewModel(val repository: ContentRepository) : ViewModel(), AudioViewEvents {
    val LOG_TAG = AudioViewModel::class.java.simpleName

    var contentPlaying = Content()

    val contentPlayer: LiveData<ContentPlayer> get() = _contentPlayer
    private var _contentPlayer = MutableLiveData<ContentPlayer>()

    /** View events */
    fun attachEvents(fragment: AudioFragment) {
        fragment.initEvents(this)
    }

    override fun audioPlayerLoad(event: AudioPlayerLoad) {
        _contentPlayer = getAudioPlayer(event.contentId, event.filePath, event.previewImageUrl)
    }

    /**
     * Get audiocast player for PlayerNotificationManager in [AudioService].
     *
     * @param contentId String ID of content
     * @param filePath String location in Google Cloud Storage
     * @param imageUrl String preview image of content
     * @return MediatorLiveData<Event<ContentPlayer>> audio player
     */
    private fun getAudioPlayer(contentId: String, filePath: String, imageUrl: String) =
            getContentUri(contentId, filePath).combinePlayerData(bitmapToByteArray(imageUrl)) { a, b ->
                ContentPlayer(uri = a.data!!, image = b.data!!)
            }

    /**
     * Sets the value to the result of a function that is called when both `LiveData`s have data
     * or when they receive updates after that.
     *
     * @receiver LiveData<A> the result of [getContentUri]
     * @param other LiveData<B> the result of [bitmapToByteArray]
     * @param onChange Function2<A, B, T> retrieves value from [getContentUri] and
     * [bitmapToByteArray]
     * @return MediatorLiveData<T> content mp3 file and formatted preview image
     */
    private fun <T, A, B> LiveData<A>.combinePlayerData(other: LiveData<B>, onChange: (A, B) -> T) =
            MediatorLiveData<T>().also { result ->
                var source1emitted = false
                var source2emitted = false
                val mergeF = {
                    val source1Value = this.value
                    val source2Value = other.value
                    if (source1emitted && source2emitted)
                        result.value = onChange.invoke(source1Value!!, source2Value!!)
                }
                result.addSource(this) {
                    source1emitted = true
                    mergeF.invoke()
                }
                result.addSource(other) {
                    source2emitted = true
                    mergeF.invoke()
                }
            }

    /**
     * Retrieves content mp3 file from Google Cloud Storage
     *
     * @param contentId String ID of content
     * @param filePath String Google Cloud Storage location
     * @return LiveData<(Event<ContentUri>?)> content mp3 file
     */
    private fun getContentUri(contentId: String, filePath: String) = liveData {
        repository.getContentUri(contentId, filePath).collect { resource ->
            when (resource.status) {
                SUCCESS -> emit(success(resource.data!!.uri))
                ERROR -> {
                    Crashlytics.log(Log.ERROR, LOG_TAG, resource.message)
                    emit(error(resource.message!!, null))
                }
            }
        }
    }

    /**
     * Converts content image Bitmap preview to ByteArray
     *
     * @param url String content image preview url
     * @return LiveData<(Event<ContentBitmap>?)> content preview image as ByteArray
     */
    private fun bitmapToByteArray(url: String) = liveData {
        repository.bitmapToByteArray(url).collect { resource ->
            when (resource.status) {
                SUCCESS -> emit(success(resource.data))
                ERROR -> {
                    Crashlytics.log(Log.WARN, LOG_TAG, "bitmapToByteArray error or null - ${resource.message}")
                    emit(error(resource.message!!, null))
                }
            }
        }
    }

}