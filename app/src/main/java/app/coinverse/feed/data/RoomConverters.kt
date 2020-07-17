package app.coinverse.feed.data

import androidx.room.TypeConverter
import app.coinverse.utils.ContentType
import app.coinverse.utils.ContentType.ARTICLE
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.FeedType
import app.coinverse.utils.FeedType.DISMISSED
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.FeedType.SAVED
import com.google.firebase.Timestamp
import java.util.*

class RoomConverters {
    @TypeConverter
    fun toTimestamp(value: Long) = Timestamp(Date(value))

    @TypeConverter
    fun toTimeLong(timestamp: Timestamp) = timestamp.toDate().time

    @TypeConverter
    fun toContentType(contentType: Int) = when (contentType) {
        ARTICLE.code -> ARTICLE
        YOUTUBE.code -> YOUTUBE
        else -> throw IllegalArgumentException("Could not recognize contentType")
    }

    @TypeConverter
    fun toInteger(contentType: ContentType) = contentType.code

    @TypeConverter
    fun toFeedType(feedType: Int) = when (feedType) {
        MAIN.code -> MAIN
        SAVED.code -> SAVED
        DISMISSED.code -> DISMISSED
        else -> throw IllegalArgumentException("Could not recognize feedType")
    }

    @TypeConverter
    fun toInteger(feedType: FeedType) = feedType.code
}