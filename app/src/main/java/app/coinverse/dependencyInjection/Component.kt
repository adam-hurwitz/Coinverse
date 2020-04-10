package app.coinverse.dependencyInjection

import app.coinverse.content.views.AudioFragment
import app.coinverse.content.views.YouTubeFragment
import app.coinverse.feed.AudioService
import app.coinverse.feed.FeedFragment
import app.coinverse.firebase.FirebaseHelper
import app.coinverse.priceGraph.PriceFragment
import app.coinverse.user.UserFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [UtilsModule::class])
interface Component {
    fun inject(userFragment: UserFragment)
    fun inject(priceFragment: PriceFragment)
    fun inject(feedFragment: FeedFragment)
    fun inject(audioFragment: AudioFragment)
    fun inject(audioService: AudioService)
    fun inject(youTubeFragment: YouTubeFragment)

    fun firebaseHelper(): FirebaseHelper
}