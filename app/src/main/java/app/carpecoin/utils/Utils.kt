package app.carpecoin.utils

import android.content.res.Resources
import android.util.DisplayMetrics

object Utils {
    lateinit var resources: Resources

    fun convertDpToPx(dp: Int): Int {
        return Math.round(dp * (resources.displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }
}