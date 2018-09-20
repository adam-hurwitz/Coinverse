package app.carpecoin.user.models

import java.util.*

data class ContentAction(var timestamp: Date, var contentId: String, var contentTitle: String,
                         var creator: String, var qualityScore: Double) {
    constructor() : this(Date(), "", "", "", 0.0)
}