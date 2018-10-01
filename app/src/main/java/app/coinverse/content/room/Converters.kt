package app.coinverse.content.room

import androidx.room.TypeConverter
import app.coinverse.Enums.ContentType
import app.coinverse.Enums.ContentType.YOUTUBE
import app.coinverse.Enums.FeedType
import app.coinverse.Enums.FeedType.*
import java.util.*

class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long): Date {
        return Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun toContentType(contentType: Int): ContentType {
        when (contentType) {
            YOUTUBE.code -> return YOUTUBE
            NONE.code -> return ContentType.NONE
            else -> throw IllegalArgumentException("Could not recognize contentType")
        }
    }

    @TypeConverter
    fun toInteger(contentType: ContentType): Int? {
        return contentType.code
    }

    @TypeConverter
    fun toFeedType(feedType: Int): FeedType {
        when (feedType) {
            MAIN.code -> return MAIN
            SAVED.code -> return SAVED
            ARCHIVED.code -> return ARCHIVED
            NONE.code -> return NONE
            else -> throw IllegalArgumentException("Could not recognize feedType")
        }
    }

    @TypeConverter
    fun toInteger(feedType: FeedType): Int? {
        return feedType.code
    }

}