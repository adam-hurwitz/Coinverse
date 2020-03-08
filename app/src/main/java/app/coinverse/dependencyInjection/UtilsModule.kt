package app.coinverse.dependencyInjection

import android.app.Application
import android.content.Context
import androidx.room.Room
import app.coinverse.feed.room.CoinverseDatabase
import app.coinverse.utils.DATABASE_NAME
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
    fun providesAppContext() = app as Context

    @Provides
    @Singleton
    fun providesFirebaseAnalytics() = FirebaseAnalytics.getInstance(app)

    @Provides
    @Singleton
    fun providesFeedDatabase() = Room.databaseBuilder(
            app,
            CoinverseDatabase::class.java, DATABASE_NAME)
            .addMigrations(ROOM_MIGRATION_1_2, ROOM_MIGRATION_2_3)
            .build()
}