package app.coinverse.content.room

import androidx.room.TypeConverter
import app.coinverse.Enums.ContentType
import app.coinverse.Enums.ContentType.ARTICLE
import app.coinverse.Enums.ContentType.YOUTUBE
import app.coinverse.Enums.FeedType
import app.coinverse.Enums.FeedType.*
import com.google.firebase.Timestamp
import java.util.*

class Converters {

    @TypeConverter
    fun toTimestamp(value: Long): Timestamp = Timestamp(Date(value))

    @TypeConverter
    fun toTimeLong(timestamp: Timestamp) = timestamp.toDate().time

    @TypeConverter
    fun toContentType(contentType: Int) =
            when (contentType) {
                ARTICLE.code -> ARTICLE
                YOUTUBE.code -> YOUTUBE
                NONE.code -> ContentType.NONE
                else -> throw IllegalArgumentException("Could not recognize contentType")
            }

    @TypeConverter
    fun toInteger(contentType: ContentType) = contentType.code

    @TypeConverter
    fun toFeedType(feedType: Int) =
            when (feedType) {
                MAIN.code -> MAIN
                SAVED.code -> SAVED
                DISMISSED.code -> DISMISSED
                NONE.code -> NONE
                else -> throw IllegalArgumentException("Could not recognize feedType")
            }

    @TypeConverter
    fun toInteger(feedType: FeedType) = feedType.code
}