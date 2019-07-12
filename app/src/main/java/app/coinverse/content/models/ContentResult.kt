package app.coinverse.content.models

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.paging.PagedList

sealed class ContentResult {
    data class PagedListResult(val pagedList: LiveData<PagedList<Content>>?,
                               val errorMessage: String) : ContentResult()

    data class ContentToPlay(var position: Int, var content: Content, var filePath: String?,
                             val errorMessage: String) : Parcelable {
        constructor(parcel: Parcel) : this(
                parcel.readInt(),
                parcel.readParcelable(Content::class.java.classLoader),
                parcel.readString(),
                parcel.readString())
        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(position)
            parcel.writeParcelable(content, flags)
            parcel.writeString(filePath)
            parcel.writeString(errorMessage)
        }

        override fun describeContents() = 0
        companion object CREATOR : Parcelable.Creator<ContentToPlay> {
            override fun createFromParcel(parcel: Parcel): ContentToPlay {
                return ContentToPlay(parcel)
            }
            override fun newArray(size: Int): Array<ContentToPlay?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class ContentUri(val uri: Uri, val errorMessage: String) : ContentResult()
    data class ContentBitmap(val image: ByteArray, val errorMessage: String) : ContentResult()
    data class ContentPlayer(val uri: Uri, val image: ByteArray, val errorMessage: String)
        : ContentResult(), Parcelable {
        constructor(parcel: Parcel) : this(
                parcel.readParcelable(Uri::class.java.classLoader),
                parcel.createByteArray(),
                parcel.readString())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeParcelable(uri, flags)
            parcel.writeByteArray(image)
            parcel.writeString(errorMessage)
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<ContentPlayer> {
            override fun createFromParcel(parcel: Parcel): ContentPlayer {
                return ContentPlayer(parcel)
            }

            override fun newArray(size: Int): Array<ContentPlayer?> {
                return arrayOfNulls(size)
            }
        }
    }
    data class ContentLabeled(val position: Int, val errorMessage: String) : ContentResult()
}