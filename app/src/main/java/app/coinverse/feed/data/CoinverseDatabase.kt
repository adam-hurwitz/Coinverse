package app.coinverse.feed.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.coinverse.feed.Content

@Database(entities = arrayOf(Content::class), version = 3)
@TypeConverters(RoomConverters::class)
abstract class CoinverseDatabase : RoomDatabase() {

    abstract fun feedDao(): FeedDao
}
