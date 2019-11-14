package app.coinverse.utils

import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.ui.R.id.snackbar_text
import androidx.paging.Config
import app.coinverse.R.color
import app.coinverse.R.drawable.*
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

lateinit var resourcesUtil: Resources

suspend fun Query.awaitRealtime() = suspendCancellableCoroutine<QuerySnapshot?> { continuation ->
    addSnapshotListener({ value, error ->
        if (error == null && continuation.isActive && !value!!.isEmpty) {
            continuation.resume(value)
        }
    })
}

val pagedListConfig = Config(
        enablePlaceholders = true,
        prefetchDistance = PREFETCH_DISTANCE,
        pageSize = PAGE_SIZE)

fun convertDpToPx(dp: Int) = Math.round(dp * (resourcesUtil.displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))

fun getDisplayWidth(context: Context) = context.resources.displayMetrics.widthPixels

fun getDisplayHeight(context: Context) = context.resources.displayMetrics.heightPixels

fun ImageView.setImageUrl(context: Context, url: String) {
    GlideApp.with(context)
            .load(url)
            .transform(RoundedCorners(CONTENT_IMAGE_CORNER_RADIUS))
            .placeholder(ic_content_placeholder)
            .error(ic_coinverse_24dp)
            .fallback(ic_coinverse_24dp)
            .into(this)
}

fun getDialogDisplayWidth(context: Context) =
        if (context.resources.configuration.orientation == ORIENTATION_PORTRAIT)
            getDisplayWidth(context)
        else (getDisplayWidth(context) / CONTENT_DIALOG_LANDSCAPE_WIDTH_DIVISOR).toInt()

fun getDialogDisplayHeight(context: Context) =
        if (context.resources.configuration.orientation == ORIENTATION_PORTRAIT)
            getDisplayHeight(context) / CONTENT_DIALOG_PORTRAIT_HEIGHT_DIVISOR
        else (getDisplayHeight(context) / CONTENT_DIALOG_LANDSCAPE_HEIGHT_DIVISOR).toInt()

fun snackbarWithText(res: String, rootView: View) {
    Snackbar.make(rootView, res, Snackbar.LENGTH_LONG).apply {
        this.view.fitsSystemWindows = true
        (this.view.findViewById(snackbar_text) as TextView)
                .setTextColor(resourcesUtil.getColor(color.colorPrimary, null))
    }.show()
}

fun ByteArray.byteArrayToBitmap(context: Context) =
        run {
            BitmapFactory.decodeByteArray(this, BITMAP_OFFSET, size).run {
                if (this != null) this
                else AppCompatResources.getDrawable(context, ic_coinverse_48dp)?.toBitmap()
            }
        }