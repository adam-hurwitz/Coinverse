package app.coinverse.content.adapter

import android.R.color.white
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import app.coinverse.Enums
import app.coinverse.Enums.FeedType.DISMISSED
import app.coinverse.Enums.FeedType.SAVED
import app.coinverse.Enums.PaymentStatus.FREE
import app.coinverse.Enums.PaymentStatus.PAID
import app.coinverse.Enums.SignInType.DIALOG
import app.coinverse.Enums.UserActionType.DISMISS
import app.coinverse.Enums.UserActionType.SAVE
import app.coinverse.R.color
import app.coinverse.R.dimen
import app.coinverse.R.drawable.*
import app.coinverse.R.string.dismiss
import app.coinverse.R.string.save
import app.coinverse.home.HomeViewModel
import app.coinverse.user.SignInDialogFragment
import app.coinverse.utils.*
import com.google.firebase.auth.FirebaseAuth
import com.mopub.nativeads.MoPubRecyclerAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

private val LOG_TAG = ItemTouchHelper::class.java.simpleName

class ItemTouchHelper(var homeViewModel: HomeViewModel) {

    fun build(context: Context, paymentStatus: Enums.PaymentStatus, feedType: String, adapter: ContentAdapter,
              moPubAdapter: MoPubRecyclerAdapter, fragmentManager: FragmentManager): ItemTouchHelper {
        return ItemTouchHelper(object : Callback() {

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
                    if (paymentStatus == FREE && !moPubAdapter.isAd(viewHolder.adapterPosition))
                        makeMovementFlags(0, LEFT or RIGHT)
                    else if (paymentStatus == PAID) makeMovementFlags(0, LEFT or RIGHT)
                    else makeMovementFlags(0, ACTION_STATE_IDLE)

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                FirebaseAuth.getInstance().currentUser.let { user ->
                    if (user != null) {
                        if (paymentStatus == FREE) {
                            val contentAdapterPosition = moPubAdapter.getOriginalPosition(viewHolder.adapterPosition)
                            if (direction == RIGHT_SWIPE && feedType != SAVED.name) // Save
                                adapter.organizeContent(feedType, SAVE, contentAdapterPosition, user)
                                        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                                        .subscribe { status -> Log.v(LOG_TAG, "Move to SAVED status: $status") }
                                        .dispose()
                            if (direction == LEFT_SWIPE && feedType != DISMISSED.name)
                                adapter.organizeContent(feedType, DISMISS, contentAdapterPosition, user)
                                        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                                        .subscribe { status -> Log.v(LOG_TAG, "Move to DISMISSED status: $status") }
                                        .dispose()
                        } else {
                            if (direction == RIGHT_SWIPE && feedType != SAVED.name)  // Save
                                adapter.organizeContent(feedType, SAVE, viewHolder.adapterPosition, user)
                            if (direction == LEFT_SWIPE && feedType != DISMISSED.name)  // Dismiss
                                adapter.organizeContent(feedType, DISMISS, viewHolder.adapterPosition, user)
                        }
                    } else signInDialog(viewHolder as ContentAdapter.ViewHolder)
                }
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView,
                                     viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float,
                                     actionState: Int, isCurrentlyActive: Boolean) {
                homeViewModel.enableSwipeToRefresh(false)
                if (actionState == ACTION_STATE_SWIPE) {
                    var icon = getDrawable(context, ic_error_black_48dp)
                    val iconLeft: Int
                    val iconRight: Int
                    val background: ColorDrawable
                    val itemView = viewHolder.itemView
                    val margin = convertDpToPx(CELL_CONTENT_MARGIN)
                    val iconWidth = icon!!.intrinsicWidth
                    val iconHeight = icon.intrinsicHeight
                    val cellHeight = itemView.bottom - itemView.top
                    val iconTop = itemView.top + (cellHeight - iconHeight) / 2
                    val iconBottom = iconTop + iconHeight
                    val paint = Paint()
                    paint.textSize = context.resources.getDimension(dimen.text_size_normal)
                    paint.color = context.getColor(white)
                    var action = ""
                    val xTextPosition: Float
                    val yTextPosition: Float

                    if (dX > 0 && feedType != SAVED.name) { // Save
                        // Draw icon.
                        icon = getDrawable(context, ic_save_planet_dark_48dp)
                        background = ColorDrawable(getColor(context, color.colorPrimary))
                        background.setBounds(0, itemView.top, (itemView.left + dX).toInt(), itemView.bottom)
                        iconLeft = margin
                        iconRight = margin + iconWidth
                        background.draw(c)
                        icon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon?.draw(c)
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        // Draw text.
                        action = context.getString(save)
                        val actionTextLength = paint.measureText(action)
                        xTextPosition = (margin + ((iconWidth - actionTextLength) / 2))
                        yTextPosition = (iconBottom + convertDpToPx(SWIPE_CONTENT_Y_MARGIN_DP)).toFloat()
                        c.drawText(action, xTextPosition, yTextPosition, paint)
                    } else if (dX < 0 && feedType != DISMISSED.name) { // Dismiss
                        // Draw icon.
                        icon = getDrawable(context, ic_dismiss_planet_light_48dp)
                        background = ColorDrawable(getColor(context, color.colorAccent))
                        background.setBounds((itemView.right - dX).toInt(), itemView.top, 0, itemView.bottom)
                        iconLeft = itemView.right - margin - iconWidth
                        iconRight = itemView.right - margin
                        background.draw(c)
                        icon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon?.draw(c)
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        // Draw text.
                        action = context.getString(dismiss)
                        val actionTextLength = paint.measureText(action)
                        xTextPosition = (itemView.right - margin - ((iconWidth + actionTextLength) / 2))
                        yTextPosition = (iconBottom + convertDpToPx(SWIPE_CONTENT_Y_MARGIN_DP)).toFloat()
                        c.drawText(action, xTextPosition, yTextPosition, paint)
                    }
                }
            }

            private fun signInDialog(viewHolder: ContentAdapter.ViewHolder) {
                SignInDialogFragment.newInstance(
                        Bundle().apply { putInt(SIGNIN_TYPE_KEY, DIALOG.code) }
                ).show(fragmentManager, SIGNIN_DIALOG_FRAGMENT_TAG)
                adapter.notifyItemChanged(viewHolder.adapterPosition)
            }

        })
    }
}