package app.coinverse.user.models

import app.coinverse.Enums.PaymentStatus
import app.coinverse.Enums.AccountType
import app.coinverse.Enums.PaymentStatus.FREE
import app.coinverse.Enums.AccountType.READ
import com.google.firebase.Timestamp

data class User(var id: String, var username: String?, var email: String?,
                var phoneNumber: String?, var profileImage: String, var creationTimestamp: Timestamp,
                var lastSignInTimestamp: Timestamp, var providerId: String, var paymentStatus: PaymentStatus,
                var accountType: AccountType, var viewCount: Double, var startCount: Double,
                var consumeCount: Double, var finishCount: Double, var organizeCount: Double,
                var shareCount: Double, var clearFeedCount: Double, var dismissCount: Double) {
    constructor() : this("", "", "", "", "",
            Timestamp.now(), Timestamp.now(), "", FREE, READ,0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
}