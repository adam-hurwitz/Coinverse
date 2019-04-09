package app.coinverse.content.models

import app.coinverse.Enums.FeedType
import app.coinverse.Enums.Status

data class ContentSwipedStatus(var feedType: FeedType, var status: Status)