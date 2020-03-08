package app.coinverse.feed.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.coinverse.feed.models.Content

@Database(entities = arrayOf(Content::class), version = 3)
@TypeConverters(Converters::class)
abstract class CoinverseDatabase : RoomDatabase() {

    abstract fun feedDao(): FeedDao
}
