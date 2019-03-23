package app.coinverse.content.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.coinverse.content.models.Content
import app.coinverse.utils.DATABASE_NAME

@Database(entities = arrayOf(Content::class), version = 2)
@TypeConverters(Converters::class)
abstract class CoinverseDatabase : RoomDatabase() {

    abstract fun contentDao(): ContentDao

    companion object {

        private var INSTANCE: CoinverseDatabase? = null

        fun getAppDatabase(context: Context): CoinverseDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context.applicationContext,
                        CoinverseDatabase::class.java, DATABASE_NAME)
                        .addMigrations(MIGRATION_1_2)
                        .build()
            }
            return INSTANCE as CoinverseDatabase
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE content ADD COLUMN loadingStatus INTEGER DEFAULT 0 NOT NULL")
            }
        }
    }

}
