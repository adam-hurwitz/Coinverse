package app.coinverse.feed.models

import android.net.Uri
import androidx.lifecycle.LiveData
import app.coinverse.utils.livedata.Event

data class PlayerViewState(val contentPlayer: LiveData<Event<ContentPlayer>>)
data class ContentUri(val uri: Uri, val errorMessage: String)
data class ContentBitmap(val image: ByteArray = ByteArray(0), val errorMessage: String = "")