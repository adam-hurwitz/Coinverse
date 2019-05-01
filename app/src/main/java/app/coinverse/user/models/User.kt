package app.coinverse.user.models

import app.coinverse.utils.Enums.AccountType
import app.coinverse.utils.Enums.AccountType.READ
import app.coinverse.utils.Enums.PaymentStatus
import app.coinverse.utils.Enums.PaymentStatus.FREE
import com.google.firebase.Timestamp

data class User(var id: String, var username: String?, var email: String?,
                var phoneNumber: String?, var profileImage: String, var creationTimestamp: Timestamp,
                var lastSignInTimestamp: Timestamp, var providerId: String, var paymentStatus: PaymentStatus,
                var accountType: AccountType) {
    constructor() : this("", "", "", "", "",
            Timestamp.now(), Timestamp.now(), "", FREE, READ)
}