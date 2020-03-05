package app.coinverse.dependencyInjection

import android.content.Context
import app.coinverse.analytics.Analytics
import app.coinverse.content.ContentRepository
import app.coinverse.content.views.AudioFragment
import app.coinverse.content.views.YouTubeFragment
import app.coinverse.feed.AudioService
import app.coinverse.feed.FeedFragment
import app.coinverse.feed.FeedRepository
import app.coinverse.feed.room.FeedDatabase
import app.coinverse.firebase.FirebaseHelper
import app.coinverse.priceGraph.PriceFragment
import app.coinverse.priceGraph.PriceRepository
import app.coinverse.user.UserFragment
import app.coinverse.user.UserRepository
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [UtilsModule::class])
interface AppComponent {
    fun inject(userFragment: UserFragment)
    fun inject(priceFragment: PriceFragment)
    fun inject(feedFragment: FeedFragment)
    fun inject(audioFragment: AudioFragment)
    fun inject(audioService: AudioService)
    fun inject(youTubeFragment: YouTubeFragment)

    fun context(): Context
    fun firebaseHelper(): FirebaseHelper
    fun firebaseAnalytics(): FirebaseAnalytics
    fun feedDatabase(): FeedDatabase
    fun analytics(): Analytics
    fun userRepository(): UserRepository
    fun priceRepository(): PriceRepository
    fun feedRepository(): FeedRepository
    fun contentRepository(): ContentRepository
}