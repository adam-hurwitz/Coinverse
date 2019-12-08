package app.coinverse.analytics.models

import com.google.firebase.Timestamp

data class ContentAction(var timestamp: Timestamp = Timestamp.now(),
                         var contentId: String = "",
                         var title: String = "",
                         var creator: String = "",
                         var qualityScore: Double = 0.0)