package app.coinverse.content.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.coinverse.content.models.Content
import app.coinverse.utils.DATABASE_NAME

@Database(entities = arrayOf(Content::class), version = 1)
@TypeConverters(Converters::class)
abstract class CoinverseDatabase : RoomDatabase() {

    abstract fun contentDao(): ContentDao

    companion object {

        private var INSTANCE: CoinverseDatabase? = null

        fun getAppDatabase(context: Context): CoinverseDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context.applicationContext,
                        CoinverseDatabase::class.java, DATABASE_NAME).build()
            }
            return INSTANCE as CoinverseDatabase
        }

    }
}
