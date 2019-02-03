package app.coinverse.content.models

import android.os.Parcel
import android.os.Parcelable

data class ContentSelected(var position: Int, var content: Content, var response: String?) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readParcelable(Content::class.java.classLoader),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(position)
        parcel.writeParcelable(content, flags)
        parcel.writeString(response)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ContentSelected> {
        override fun createFromParcel(parcel: Parcel): ContentSelected {
            return ContentSelected(parcel)
        }

        override fun newArray(size: Int): Array<ContentSelected?> {
            return arrayOfNulls(size)
        }
    }
}