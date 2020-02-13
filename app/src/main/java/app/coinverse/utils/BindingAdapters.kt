package app.coinverse.utils

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import app.coinverse.R
import app.coinverse.utils.ContentType.ARTICLE
import app.coinverse.utils.ContentType.YOUTUBE
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

@BindingAdapter("imageUrl")
fun ImageView.setImageUrlRounded(url: String?) {
    GlideApp.with(context)
            .load(url)
            .transform(RoundedCorners(CONTENT_IMAGE_CORNER_RADIUS))
            .placeholder(R.drawable.ic_content_placeholder)
            .error(R.drawable.ic_coinverse_24dp)
            .fallback(R.drawable.ic_coinverse_24dp)
            .into(this)
}

@BindingAdapter("contentTypeIcon")
fun ImageView.setContentTypeIcon(contentType: ContentType) {
    when (contentType) {
        ARTICLE -> this.setImageResource(R.drawable.ic_audio_black)
        YOUTUBE -> this.setImageResource(R.drawable.ic_video_black)
    }
}

@BindingAdapter("timePostedAgo")
fun TextView.setTimePostedAgo(time: Long) {
    this.text = getTimeAgo(context, time, false)
}