package app.coinverse.content.models

import java.util.*

data class UserAction(var timestamp: Date, var email: String) {
    constructor() : this(Date(), "")
}