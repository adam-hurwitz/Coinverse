package app.coinverse.user.models

import app.coinverse.utils.AccountType
import app.coinverse.utils.AccountType.READ
import app.coinverse.utils.PaymentStatus
import app.coinverse.utils.PaymentStatus.FREE
import com.google.firebase.Timestamp

data class User(var id: String = "", var username: String? = "", var email: String? = "",
                var phoneNumber: String? = "", var profileImage: String = "",
                var creationTimestamp: Timestamp = Timestamp.now(),
                var lastSignInTimestamp: Timestamp = Timestamp.now(), var providerId: String = "",
                var paymentStatus: PaymentStatus = FREE, var accountType: AccountType = READ)