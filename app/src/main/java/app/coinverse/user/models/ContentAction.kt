package app.coinverse.user.models

import java.util.*

data class ContentAction(var timestamp: Date, var contentId: String, var title: String,
                         var creator: String, var qualityScore: Double) {
    constructor() : this(Date(), "", "", "", 0.0)
}