package app.coinverse.user.models

import java.util.*

data class User(var id: String, var username: String?, var email: String?,
                var phoneNumber: String?, var profileImage: String, var creationTimestamp: Date,
                var lastSignInTimestamp: Date, var providerId: String, var viewCount: Double,
                var startCount: Double, var consumeContent: Double, var finishCount: Double,
                var organizeCount: Double, var shareCount: Double, var clearFeedCount: Double,
                var dismissCount: Double)