package app.coinverse.analytics.models

import com.google.firebase.Timestamp

data class UserAction(var timestamp: Timestamp = Timestamp.now(), var email: String = "")