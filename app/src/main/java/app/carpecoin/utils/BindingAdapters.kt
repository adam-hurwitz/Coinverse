package app.carpecoin.utils

import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import androidx.databinding.BindingAdapter
import com.bumptech.glide.request.RequestOptions



@BindingAdapter("imageUrl")
fun ImageView.setImageUrl(url: String?) {
    Glide.with(context)
            .load(url)
            .into(this)
}

@BindingAdapter("profileImageUrl")
fun ImageView.setProfileImageUrl(url: String?) {
    Glide.with(context)
            .load(url)
            .apply(RequestOptions.circleCropTransform())
            .into(this)
}

@BindingAdapter("timePostedAgo")
fun TextView.setTimePostedAgo(time: Long) {
    this.text = DateAndTime.getTimeAgo(context, time)
}
