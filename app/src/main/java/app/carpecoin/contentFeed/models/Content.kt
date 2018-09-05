package app.carpecoin.contentFeed.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.carpecoin.Enums.ContentType
import app.carpecoin.Enums.ContentType.EMPTY
import java.util.*

@Entity (tableName = "content")
data class Content(@PrimaryKey var id: String, var qualityScore: Double,
                   var contentType: ContentType, var timestamp: Date, var creator: String,
                   var contentTitle: String, var previewImage: String, var description: String,
                   var url: String) {

    constructor() : this("", 0.0, EMPTY, Date(), "", "",
            "", "", "")
}