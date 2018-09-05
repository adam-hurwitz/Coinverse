package app.carpecoin.user.models

import java.util.*

data class UserInfo(var id: String, var username: String?, var email: String?,
                    var phoneNumber: String?, var profileImage: String, var creationTimestamp: Date,
                    var lastSignInTimestamp: Date, var providerId: String)