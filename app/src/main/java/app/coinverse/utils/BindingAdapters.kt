package app.coinverse.utils

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
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
