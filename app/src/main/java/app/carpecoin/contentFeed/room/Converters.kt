package app.carpecoin.contentFeed.room

import androidx.room.TypeConverter
import app.carpecoin.Enums.ContentType
import app.carpecoin.Enums.ContentType.EMPTY
import app.carpecoin.Enums.ContentType.YOUTUBE
import java.util.Date

class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long): Date {
        return Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date): Long {
        return date.getTime()
    }

    @TypeConverter
    fun toStatus(contentType: Int): ContentType {
        return if (contentType == YOUTUBE.code) {
            YOUTUBE
        } else if (contentType == EMPTY.code) {
            EMPTY
        } else {
            throw IllegalArgumentException("Could not recognize contentType")
        }
    }

    @TypeConverter
    fun toInteger(contentType: ContentType): Int? {
        return contentType.code
    }

}