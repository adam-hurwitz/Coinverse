package app.coinverse.utils

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import app.coinverse.Enums.ContentType
import app.coinverse.Enums.ContentType.ARTICLE
import app.coinverse.Enums.ContentType.YOUTUBE
import app.coinverse.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions.circleCropTransform


@BindingAdapter("imageUrl")
fun ImageView.setImageUrl(url: String?) {
    GlideApp.with(context)
            .load(url)
            .transform(RoundedCorners(CONTENT_IMAGE_CORNER_RADIUS))
            .placeholder(R.drawable.content_placeholder)
            .error(R.drawable.coinverse_logo_placeholder)
            .fallback(R.drawable.coinverse_logo_placeholder)
            .into(this)
}

@BindingAdapter("contentTypeIcon")
fun ImageView.setContentTypeIcon(contentType: ContentType) {
    when (contentType) {
        //TODO: Add ARTICLE, NONE icons.
        ARTICLE -> this.setImageResource(R.drawable.ic_audio_black)
        YOUTUBE -> this.setImageResource(R.drawable.ic_video_black)
    }
}

@BindingAdapter("profileImageUrl")
fun ImageView.setProfileImageUrl(url: String?) {
    Glide.with(context)
            .load(url)
            .apply(circleCropTransform())
            .into(this)
}

@BindingAdapter("timePostedAgo")
fun TextView.setTimePostedAgo(time: Long) {
    this.text = DateAndTime.getTimeAgo(context, time, false)
}
