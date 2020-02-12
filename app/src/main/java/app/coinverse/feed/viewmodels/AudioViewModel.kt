package app.coinverse.feed.viewmodels

import android.util.Log
import androidx.lifecycle.*
import app.coinverse.feed.models.*
import app.coinverse.feed.models.AudioViewEventType.AudioPlayerLoad
import app.coinverse.feed.network.ContentRepository
import app.coinverse.feed.views.AudioFragment
import app.coinverse.utils.models.Lce
import com.crashlytics.android.Crashlytics
import kotlinx.coroutines.flow.collect

/**
 * TODO: Refactor with Unidirectional Data Flow. See [FeedViewModel].
 * See more: https://medium.com/hackernoon/android-unidirectional-flow-with-livedata-bf24119e747
 **/
class AudioViewModel : ViewModel(), AudioViewEvents {
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

    // TODO: Refactor MediatorLiveData to Coroutine Flow - https://kotlinlang.org/docs/reference/coroutines/flow.html
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
                ContentPlayer(
                        uri = a.uri,
                        image = b.image,
                        errorMessage = getAudioPlayerErrors(a, b))
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
        ContentRepository.getContentUri(contentId, filePath).collect { lce ->
            when (lce) {
                is Lce.Content -> emit(ContentUri(lce.packet.uri, ""))
                is Lce.Error -> {
                    Crashlytics.log(Log.ERROR, LOG_TAG, lce.packet.errorMessage)
                    emit(ContentUri(lce.packet.uri, lce.packet.errorMessage))
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
        ContentRepository.bitmapToByteArray(url).collect { lce ->
            when (lce) {
                is Lce.Content -> emit(ContentBitmap(lce.packet.image, lce.packet.errorMessage))
                is Lce.Error -> {
                    Crashlytics.log(Log.WARN, LOG_TAG,
                            "bitmapToByteArray error or null - ${lce.packet.errorMessage}")
                    emit(ContentBitmap(lce.packet.image, lce.packet.errorMessage))
                }
            }
        }
    }

    /**
     * Collects and combines errors from building the audio player.
     *
     * @param a Event<ContentUri> content mp3 file
     * @param b Event<ContentBitmap> content preview image
     * @return String combined error message
     */
    private fun getAudioPlayerErrors(a: ContentUri, b: ContentBitmap) =
            a.errorMessage.apply { if (this.isNotEmpty()) this }.apply {
                b.errorMessage.also {
                    if (it.isNotEmpty()) this.plus(" " + it)
                }
            }

}