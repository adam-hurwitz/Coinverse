package app.coinverse.feed

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import app.coinverse.utils.ContentType
import app.coinverse.utils.ContentType.NONE
import app.coinverse.utils.ContentType.values
import app.coinverse.utils.FeedType
import app.coinverse.utils.FeedType.MAIN
import com.google.firebase.Timestamp
import java.util.*

@Entity(tableName = "content")
data class Content(
        @PrimaryKey var id: String = "",
        var qualityScore: Double = 0.0,
        var contentType: ContentType = NONE,
        var timestamp: Timestamp = Timestamp.now(),
        var creator: String = "",
        var title: String = "",
        var previewImage: String = "",
        var description: String = "",
        var url: String = "",
        var textUrl: String = "",
        var audioUrl: String = "",
        var feedType: FeedType = MAIN,
        var savedPosition: Int = 0,
        var viewCount: Double = 0.0,
        var startCount: Double = 0.0,
        var consumeCount: Double = 0.0,
        var finishCount: Double = 0.0,
        var organizeCount: Double = 0.0,
        var shareCount: Double = 0.0,
        var clearFeedCount: Double = 0.0,
        var dismissCount: Double = 0.0) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readDouble(),
            values()[parcel.readInt()],
            Timestamp((Date(parcel.readLong()))),
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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeDouble(qualityScore)
        parcel.writeInt(contentType.ordinal)
        parcel.writeLong(timestamp.toDate().time)
        parcel.writeString(creator)
        parcel.writeString(title)
        parcel.writeString(previewImage)
        parcel.writeString(description)
        parcel.writeString(url)
        parcel.writeString(textUrl)
        parcel.writeString(audioUrl)
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