package app.coinverse.feed.models

import android.net.Uri

data class ContentUri(val uri: Uri, val errorMessage: String)
data class ContentBitmap(val image: ByteArray = ByteArray(0), val errorMessage: String = "")