package app.coinverse.utils

import android.content.Context
import app.coinverse.BuildConfig
import app.coinverse.R
import app.coinverse.utils.BuildType.*
import app.coinverse.utils.Timeframe.*
import com.google.firebase.Timestamp
import java.util.*

val buildTypeTimescale = when (BuildConfig.BUILD_TYPE) {
    debug.name, release.name -> DAY
    open.name -> ALL
    else -> DAY
}

private val SECOND_MILLIS = 1000
private val MINUTE_MILLIS = 60 * SECOND_MILLIS
private val HOUR_MILLIS = 60 * MINUTE_MILLIS
private val DAY_MILLIS = 24 * HOUR_MILLIS

fun getTimeframe(timeframe: Timeframe?): Timestamp {
    val timeframeToQuery: Int
    when (timeframe) {
        DAY -> timeframeToQuery = -1
        WEEK -> timeframeToQuery = -7
        ALL -> timeframeToQuery = -3650
        else -> timeframeToQuery = -1
    }
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, timeframeToQuery)
    return Timestamp(Date(calendar.timeInMillis))
}


fun getTimeAgo(context: Context, time: Long, abbreviate: Boolean): String? {
    var time = time
    if (time < 1000000000000L) {
        // If timestamp given in seconds, convert to millis
        time *= 1000
    }

    val now = System.currentTimeMillis()
    if (time > now || time <= 0) {
        return null
    }

    val diff = now - time
    return if (diff < MINUTE_MILLIS) {
        context.getString(R.string.now)
    } else if (diff < 2 * MINUTE_MILLIS) {
        if (abbreviate) context.getString(R.string.a_minute_ago_abbreviate)
        context.getString(R.string.a_minute_ago)
    } else if (diff < 50 * MINUTE_MILLIS) {
        if (abbreviate) String.format("%s %s", (diff / MINUTE_MILLIS).toString(), context.getString(R.string.minutes_ago_abbreviate))
        else String.format("%s %s", (diff / MINUTE_MILLIS).toString(), context.getString(R.string.minutes_ago))
    } else if (diff < 90 * MINUTE_MILLIS) {
        if (abbreviate) context.getString(R.string.an_hour_ago_abbreviate)
        else context.getString(R.string.an_hour_ago)
    } else if (diff < 24 * HOUR_MILLIS) {
        if (abbreviate) String.format("%s %s", (diff / HOUR_MILLIS).toString(), context.getString(R.string.hours_ago_abbreviate))
        else String.format("%s %s", (diff / HOUR_MILLIS).toString(), context.getString(R.string.hours_ago))
    } else if (diff < 48 * HOUR_MILLIS) {
        context.getString(R.string.yesterday)
    } else {
        if (abbreviate) String.format("%s %s", (diff / DAY_MILLIS).toString(), context.getString(R.string.days_ago_abbreviate))
        else String.format("%s %s", (diff / DAY_MILLIS).toString(), context.getString(R.string.days_ago))
    }
}