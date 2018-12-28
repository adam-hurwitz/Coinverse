package app.coinverse.utils

import android.os.Bundle
import app.coinverse.Enums.UserActionType.*
import app.coinverse.content.ContentViewModel
import app.coinverse.content.models.Content
import app.coinverse.content.room.ContentDao
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Param
import com.google.firebase.auth.FirebaseAuth

fun updateActionsAndAnalytics(content: Content, contentViewModel: ContentViewModel,
                              contentDao: ContentDao, analytics: FirebaseAnalytics, watchPercent: Double) {
    Thread(Runnable { run { contentDao.updateContent(content) } }).start()
    if (watchPercent >= FINISH_THRESHOLD) {
        val bundle = Bundle()
        val user = FirebaseAuth.getInstance().currentUser
        bundle.putString(Param.ITEM_NAME, content.title)
        if (user != null) {
            contentViewModel.updateActions(FINISH, content, user)
            bundle.putString(USER_ID_PARAM, user.uid)
        }
        bundle.putString(CREATOR_PARAM, content.creator)
        analytics.logEvent(FINISH_CONTENT_EVENT, bundle)
    } else if (watchPercent >= CONSUME_THRESHOLD) {
        val bundle = Bundle()
        val user = FirebaseAuth.getInstance().currentUser
        bundle.putString(Param.ITEM_NAME, content.title)
        if (user != null) {
            contentViewModel.updateActions(CONSUME, content, user)
            bundle.putString(USER_ID_PARAM, user.uid)
        }
        bundle.putString(CREATOR_PARAM, content.creator)
        analytics.logEvent(CONSUME_CONTENT_EVENT, bundle)
    }
}

fun updateStartActionsAndAnalytics(savedInstanceState: Bundle?, content: Content,
                                   contentViewModel: ContentViewModel, analytics: FirebaseAnalytics) {
    if (savedInstanceState == null)
        analytics.logEvent(START_CONTENT_EVENT,
                Bundle().apply {
                    this.putString(Param.ITEM_NAME, content.title)
                    this.putString(CREATOR_PARAM, content.creator)
                    FirebaseAuth.getInstance().currentUser.let { user ->
                        if (user != null) {
                            contentViewModel.updateActions(START, content, user)
                            this.putString(USER_ID_PARAM, user.uid)
                        }
                    }
                })
}

fun getWatchPercent(currentPosition: Double, seekToPositionMillis: Double, duration: Double) =
        (currentPosition - seekToPositionMillis) / duration