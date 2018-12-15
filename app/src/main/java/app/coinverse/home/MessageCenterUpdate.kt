package app.coinverse.home

import com.google.firebase.Timestamp

data class MessageCenterUpdate(var id: String, var timestamp: Timestamp, var versionName: String,
                               var message: String) {
    constructor() : this("", Timestamp.now(), "", "")
}