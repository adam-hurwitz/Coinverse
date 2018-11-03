package app.coinverse.utils

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics

lateinit var resourcesUtil: Resources

fun convertDpToPx(dp: Int) = Math.round(dp * (resourcesUtil.displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))

fun getDisplayWidth(context: Context) = context.getResources().getDisplayMetrics().widthPixels

fun getDisplayHeight(context: Context) = context.getResources().getDisplayMetrics().heightPixels