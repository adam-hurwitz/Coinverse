package app.coinverse.utils

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.ui.R.id.snackbar_text
import app.coinverse.R
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.snackbar.Snackbar

lateinit var resourcesUtil: Resources

fun convertDpToPx(dp: Int) = Math.round(dp * (resourcesUtil.displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))

fun getDisplayWidth(context: Context) = context.resources.displayMetrics.widthPixels

fun getDisplayHeight(context: Context) = context.resources.displayMetrics.heightPixels

fun ImageView.setImageUrl(context: Context, url: String) {
    GlideApp.with(context)
            .load(url)
            .transform(RoundedCorners(CONTENT_IMAGE_CORNER_RADIUS))
            .placeholder(R.drawable.ic_content_placeholder)
            .error(R.drawable.ic_coinverse_24dp)
            .fallback(R.drawable.ic_coinverse_24dp)
            .into(this)
}

fun snackbarWithText(res: String, rootView: View) {
    val snackbar = Snackbar.make(rootView, res, Snackbar.LENGTH_LONG)
    snackbar.view.fitsSystemWindows = true
    val textView = snackbar.view.findViewById(snackbar_text) as TextView
    textView.setTextColor(resourcesUtil.getColor(R.color.colorPrimary, null))
    snackbar.show()
}