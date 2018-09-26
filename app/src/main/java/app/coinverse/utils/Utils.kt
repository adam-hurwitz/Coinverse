package app.coinverse.utils

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics

object Utils {
    lateinit var resources: Resources

    fun convertDpToPx(dp: Int) = Math.round(dp * (resources.displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))

    fun getDisplayWidth(context: Context) = context.getResources().getDisplayMetrics().widthPixels

    fun getDisplayHeight(context: Context) = context.getResources().getDisplayMetrics().heightPixels

}