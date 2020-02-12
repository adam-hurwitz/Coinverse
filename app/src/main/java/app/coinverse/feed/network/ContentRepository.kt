package app.coinverse.feed.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import app.coinverse.feed.models.ContentBitmap
import app.coinverse.feed.models.ContentPlayer
import app.coinverse.firebase.contentEnCollection
import app.coinverse.firebase.firebaseApp
import app.coinverse.utils.AUDIO_URL
import app.coinverse.utils.AUDIO_URL_TOKEN_REGEX
import app.coinverse.utils.BITMAP_COMPRESSION_QUALITY
import app.coinverse.utils.models.Lce
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL

object ContentRepository {

    fun getContentUri(contentId: String, filePath: String) = flow {
        emit(Lce.Loading())
        try {
            val uri = FirebaseStorage.getInstance(firebaseApp(true))
                    .reference.child(filePath).downloadUrl.await()
            contentEnCollection.document(contentId) // Update content Audio Uri.
                    .update(AUDIO_URL, Regex(AUDIO_URL_TOKEN_REGEX).replace(uri.toString(), "")).await()
            emit(Lce.Content(ContentPlayer(
                    uri = uri,
                    image = ByteArray(0),
                    errorMessage = "")))
        } catch (error: StorageException) {
            emit(Lce.Error(ContentPlayer(
                    Uri.parse(""),
                    ByteArray(0), "getContentUri error - ${error.localizedMessage}")))
        }
    }

    fun bitmapToByteArray(url: String) = flow {
        emit(Lce.Loading())
        emit(Lce.Content(ContentBitmap(ByteArrayOutputStream().apply {
            try {
                BitmapFactory.decodeStream(URL(url).openConnection().apply {
                    doInput = true
                    connect()
                }.getInputStream())
            } catch (e: IOException) {
                emit(Lce.Error(ContentBitmap(ByteArray(0),
                        "bitmapToByteArray error or null - ${e.localizedMessage}")))
                null
            }?.compress(Bitmap.CompressFormat.JPEG, BITMAP_COMPRESSION_QUALITY, this)
        }.toByteArray(), "")))
    }.flowOn(Dispatchers.IO)
}