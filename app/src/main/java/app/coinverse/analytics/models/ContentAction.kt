package app.coinverse.analytics.models

import com.google.firebase.Timestamp

data class ContentAction(var timestamp: Timestamp, var contentId: String, var title: String,
                         var creator: String, var qualityScore: Double) {
    constructor() : this(Timestamp.now(), "", "", "", 0.0)
}