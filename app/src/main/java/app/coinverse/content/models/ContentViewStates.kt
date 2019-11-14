package app.coinverse.content.models

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import androidx.room.Entity
import androidx.room.PrimaryKey
import app.coinverse.utils.ContentType
import app.coinverse.utils.FeedType
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.Timeframe
import app.coinverse.utils.Timeframe.DAY
import app.coinverse.utils.livedata.Event
import app.coinverse.utils.models.ToolbarState
import com.google.firebase.Timestamp
import java.util.*

data class FeedViewState(val feedType: FeedType = MAIN,
                         val timeframe: Timeframe = DAY,
                         val toolbar: ToolbarState = ToolbarState(),
                         val contentList: LiveData<PagedList<Content>> = MutableLiveData(),
                         val contentToPlay: LiveData<Event<ContentToPlay?>> = MutableLiveData(),
                         val contentLabeled: LiveData<Event<ContentLabeled?>> = MutableLiveData(),
                         val notificationBitmap: LiveData<Event<Bitmap>> = MutableLiveData())

data class PlayerViewState(val contentPlayer: LiveData<Event<ContentPlayer>>)

@Entity(tableName = "content")
data class Content(@PrimaryKey var id: String = "",
                   var qualityScore: Double = 0.0,
                   var contentType: ContentType = ContentType.NONE,
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
            ContentType.values()[parcel.readInt()],
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

data class ContentToPlay(var position: Int,
                         var content: Content,
                         var filePath: String?,
                         val errorMessage: String) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readParcelable(Content::class.java.classLoader)!!,
            parcel.readString(),
            parcel.readString()!!)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(position)
        parcel.writeParcelable(content, flags)
        parcel.writeString(filePath)
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

data class ContentLabeled(val position: Int, val errorMessage: String)
data class PagedListResult(val pagedList: LiveData<PagedList<Content>>?,
                           val errorMessage: String)

data class ContentUri(val uri: Uri, val errorMessage: String)
data class ContentBitmap(val image: ByteArray = ByteArray(0),
                         val errorMessage: String = "")

data class ContentPlayer(val uri: Uri, val image: ByteArray, val errorMessage: String) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readParcelable(Uri::class.java.classLoader)!!,
            parcel.createByteArray()!!,
            parcel.readString()!!)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeByteArray(image)
        parcel.writeString(errorMessage)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<ContentPlayer> {
        override fun createFromParcel(parcel: Parcel) = ContentPlayer(parcel)

        override fun newArray(size: Int): Array<ContentPlayer?> {
            return arrayOfNulls(size)
        }
    }
}
