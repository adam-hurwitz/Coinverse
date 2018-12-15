package app.coinverse.user.models

import com.google.firebase.Timestamp

data class User(var id: String, var username: String?, var email: String?,
                var phoneNumber: String?, var profileImage: String, var creationTimestamp: Timestamp,
                var lastSignInTimestamp: Timestamp, var providerId: String, var messageCenterUnreadCount: Double,
                var viewCount: Double, var startCount: Double, var consumeCount: Double,
                var finishCount: Double, var organizeCount: Double, var shareCount: Double,
                var clearFeedCount: Double, var dismissCount: Double) {
    constructor() : this("", "", "", "", "", Timestamp.now(),
            Timestamp.now(), "", 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0,
            0.0, 0.0)
}