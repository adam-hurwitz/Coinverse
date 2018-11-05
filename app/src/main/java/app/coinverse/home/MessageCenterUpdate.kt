package app.coinverse.home

import java.util.*

data class MessageCenterUpdate(var id: String, var timestamp: Date, var versionName: String,
                               var message: String) {
    constructor() : this("", Date(), "", "")
}