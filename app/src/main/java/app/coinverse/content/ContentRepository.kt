package app.coinverse.content

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import app.coinverse.feed.models.ContentPlayer
import app.coinverse.firebase.contentEnCollection
import app.coinverse.firebase.firebaseApp
import app.coinverse.utils.AUDIO_URL
import app.coinverse.utils.AUDIO_URL_TOKEN_REGEX
import app.coinverse.utils.BITMAP_COMPRESSION_QUALITY
import app.coinverse.utils.Resource.Companion.error
import app.coinverse.utils.Resource.Companion.loading
import app.coinverse.utils.Resource.Companion.success
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepository @Inject constructor() {
    fun getContentUri(contentId: String, filePath: String) = flow {
        emit(loading(null))
        try {
            val uri = FirebaseStorage.getInstance(firebaseApp(true))
                    .reference.child(filePath).downloadUrl.await()
            contentEnCollection.document(contentId) // Update content Audio Uri.
                    .update(AUDIO_URL, Regex(AUDIO_URL_TOKEN_REGEX).replace(uri.toString(), "")).await()
            emit(success(ContentPlayer(uri = uri, image = ByteArray(0))))
        } catch (error: StorageException) {
            emit(error("getContentUri error - ${error.localizedMessage}",
                    ContentPlayer(
                            Uri.parse(""),
                            ByteArray(0))))
        }
    }

    fun bitmapToByteArray(url: String) = flow {
        emit(loading(null))
        emit(success(ByteArrayOutputStream().apply {
            try {
                BitmapFactory.decodeStream(URL(url).openConnection().apply {
                    doInput = true
                    connect()
                }.getInputStream())
            } catch (e: IOException) {
                emit(error("bitmapToByteArray error or null - ${e.localizedMessage}", null))
                null
            }?.compress(Bitmap.CompressFormat.JPEG, BITMAP_COMPRESSION_QUALITY, this)
        }.toByteArray()))
    }.flowOn(Dispatchers.IO)
}