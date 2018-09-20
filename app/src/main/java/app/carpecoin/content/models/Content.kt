package app.carpecoin.content.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.carpecoin.Enums.ContentType
import app.carpecoin.Enums.FeedType
import java.util.*

@Entity(tableName = "content")
data class Content(@PrimaryKey var id: String, var qualityScore: Double,
                   var contentType: ContentType, var timestamp: Date, var creator: String,
                   var contentTitle: String, var previewImage: String, var description: String,
                   var url: String, var feedType: FeedType, var viewCount: Double,
                   var startCount: Double, var finishCount: Double, var organizeCount: Double,
                   var shareCount: Double, var clearFeedCount: Double, var archiveCount: Double) {

    constructor() : this("", 0.0, ContentType.NONE, Date(), "", "",
            "", "", "", FeedType.NONE, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0)
}