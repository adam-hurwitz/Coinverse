package app.coinverse.content.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.coinverse.content.models.Content

@Database(entities = arrayOf(Content::class), version = 1)
@TypeConverters(Converters::class)
abstract class ContentDatabase : RoomDatabase() {

    abstract fun contentDao(): ContentDao

    companion object {

        private var INSTANCE: ContentDatabase? = null

        fun getAppDatabase(context: Context): ContentDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context.applicationContext,
                        ContentDatabase::class.java, "content-database").build()
            }
            return INSTANCE as ContentDatabase
        }

    }
}
