package app.coinverse.dependencyInjection

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.room.Room
import app.coinverse.feed.data.CoinverseDatabase
import app.coinverse.utils.DATABASE_NAME
import app.coinverse.utils.PLAYER_OPEN_STATUS_KEY
import app.coinverse.utils.ROOM_MIGRATION_1_2
import app.coinverse.utils.ROOM_MIGRATION_2_3
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class UtilsModule(private val app: Application) {

    @Provides
    @Singleton
    fun providesSharedPreferences() = app.getSharedPreferences(PLAYER_OPEN_STATUS_KEY, MODE_PRIVATE)

    @Provides
    @Singleton
    fun providesAppContext() = app as Context

    @Provides
    @Singleton
    fun providesFirebaseAnalytics() = FirebaseAnalytics.getInstance(app)

    @Provides
    @Singleton
    fun providesFeedDao() = Room.databaseBuilder(
            app,
            CoinverseDatabase::class.java, DATABASE_NAME)
            .addMigrations(ROOM_MIGRATION_1_2, ROOM_MIGRATION_2_3)
            .build()
            .feedDao()
}