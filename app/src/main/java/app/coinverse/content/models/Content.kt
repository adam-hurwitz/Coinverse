package app.coinverse.content.models

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import app.coinverse.Enums.ContentType
import app.coinverse.Enums.FeedType
import java.util.*

@Entity(tableName = "content")
data class Content(@PrimaryKey var id: String, var qualityScore: Double,
                   var contentType: ContentType, var timestamp: Date, var creator: String,
                   var title: String, var previewImage: String, var description: String,
                   var sourceUrl: String, var contentUrl: String, var text: String,
                   var feedType: FeedType, var savedPosition: Int, var viewCount: Double,
                   var startCount: Double, var consumeCount: Double, var finishCount: Double,
                   var organizeCount: Double, var shareCount: Double, var clearFeedCount: Double,
                   var dismissCount: Double) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readDouble(),
            ContentType.values()[parcel.readInt()],
            Date(parcel.readLong()),
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            FeedType.values()[parcel.readInt()],
            parcel.readInt(),
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readDouble())

    constructor() : this("", 0.0, ContentType.NONE, Date(), "", "",
            "", "", "", "", "", FeedType.NONE, 0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeDouble(qualityScore)
        parcel.writeInt(contentType.ordinal)
        parcel.writeLong(timestamp.time)
        parcel.writeString(creator)
        parcel.writeString(title)
        parcel.writeString(previewImage)
        parcel.writeString(description)
        parcel.writeString(sourceUrl)
        parcel.writeString(contentUrl)
        parcel.writeString(text)
        parcel.writeInt(feedType.ordinal)
        parcel.writeInt(savedPosition)
        parcel.writeDouble(viewCount)
        parcel.writeDouble(startCount)
        parcel.writeDouble(consumeCount)
        parcel.writeDouble(finishCount)
        parcel.writeDouble(organizeCount)
        parcel.writeDouble(shareCount)
        parcel.writeDouble(clearFeedCount)
        parcel.writeDouble(dismissCount)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Content> {
        override fun createFromParcel(parcel: Parcel): Content {
            return Content(parcel)
        }

        override fun newArray(size: Int): Array<Content?> {
            return arrayOfNulls(size)
        }

    }
}