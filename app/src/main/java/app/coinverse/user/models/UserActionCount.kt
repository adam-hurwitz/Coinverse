package app.coinverse.user.models

data class UserActionCount(var viewCount: Double, var startCount: Double,
                           var consumeCount: Double, var finishCount: Double, var organizeCount: Double,
                           var shareCount: Double, var clearFeedCount: Double, var dismissCount: Double) {
    constructor() : this(0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0)
}
