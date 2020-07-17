package app.coinverse.content

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class ContentPlayer(val uri: Uri, val image: ByteArray) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readParcelable(Uri::class.java.classLoader)!!,
            parcel.createByteArray()!!)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeByteArray(image)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<ContentPlayer> {
        override fun createFromParcel(parcel: Parcel) = ContentPlayer(parcel)
        override fun newArray(size: Int) = arrayOfNulls<ContentPlayer>(size)
    }
}