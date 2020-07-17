package app.coinverse.dependencyInjection

import app.coinverse.content.AudioFragment
import app.coinverse.content.YouTubeFragment
import app.coinverse.feed.AudioService
import app.coinverse.feed.FeedFragment
import app.coinverse.feed.FeedViewModel
import app.coinverse.firebase.FirebaseHelper
import app.coinverse.priceGraph.PriceFragment
import app.coinverse.user.UserFragment
import dagger.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Singleton

@Singleton
@Component(modules = [AssistedInjectModule::class, UtilsModule::class])
interface Component {
    fun inject(userFragment: UserFragment)
    fun inject(priceFragment: PriceFragment)
    fun inject(feedFragment: FeedFragment)
    fun inject(audioFragment: AudioFragment)
    fun inject(audioService: AudioService)
    fun inject(youTubeFragment: YouTubeFragment)

    fun firebaseHelper(): FirebaseHelper

    @ExperimentalCoroutinesApi
    fun feedViewModelFactory(): FeedViewModel.Factory
}