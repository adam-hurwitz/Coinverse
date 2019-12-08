package app.coinverse.analytics.models

data class UserActionCount(var viewCount: Double = 0.0,
                           var startCount: Double = 0.0,
                           var consumeCount: Double = 0.0,
                           var finishCount: Double = 0.0,
                           var organizeCount: Double = 0.0,
                           var shareCount: Double = 0.0,
                           var clearFeedCount: Double = 0.0,
                           var dismissCount: Double = 0.0)
