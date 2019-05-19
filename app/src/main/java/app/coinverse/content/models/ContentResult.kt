package app.coinverse.content.models

import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.paging.PagedList

sealed class ContentResult {
    data class PagedListResult(val pagedList: LiveData<PagedList<Content>>?, val errorMessage: String) : ContentResult()
    data class ContentToPlay(var position: Int, var content: Content, var response: String?, val errorMessage: String) : Parcelable {
        constructor(parcel: Parcel) : this(
                parcel.readInt(),
                parcel.readParcelable(Content::class.java.classLoader),
                parcel.readString(),
                parcel.readString())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(position)
            parcel.writeParcelable(content, flags)
            parcel.writeString(response)
            parcel.writeString(errorMessage)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<ContentToPlay> {
            override fun createFromParcel(parcel: Parcel): ContentToPlay {
                return ContentToPlay(parcel)
            }

            override fun newArray(size: Int): Array<ContentToPlay?> {
                return arrayOfNulls(size)
            }
        }
    }
    data class ContentLabeled(val position: Int, val errorMessage: String) : ContentResult()
}

