package app.carpecoin.contentFeed.models

import java.util.*
import app.carpecoin.Enums.ContentType
import app.carpecoin.Enums.ContentType.EMPTY

data class Content(var id: String, var qualityScore: Double, var contentType: ContentType,
                   var timestamp: Date, var creator: String, var contentTitle: String,
                   var previewImage: String, var description: String, var url: String) {
    constructor() : this("", 0.0, EMPTY, Date(), "", "", "",
            "", "")
}