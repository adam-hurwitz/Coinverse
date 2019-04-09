package app.coinverse.content.models

import app.coinverse.Enums.UserActionType

data class ContentSwiped(var actionType: UserActionType, var position: Int)