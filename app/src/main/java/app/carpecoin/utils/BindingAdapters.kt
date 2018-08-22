package app.carpecoin.utils

import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import androidx.databinding.BindingAdapter

@BindingAdapter("imageUrl")
fun ImageView.setImageUrl(url: String?) {
    Glide.with(context)
            .load(url)
            .into(this)
}

@BindingAdapter("timePostedAgo")
fun TextView.setTimePostedAgo(time: Long) {
    this.text = DateAndTime.getTimeAgo(context, time)
}
