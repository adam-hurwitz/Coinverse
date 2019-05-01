package app.coinverse.analytics.models

import com.google.firebase.Timestamp

data class UserAction(var timestamp: Timestamp, var email: String) {
    constructor() : this(Timestamp.now(), "")
}