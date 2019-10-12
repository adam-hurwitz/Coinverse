package app.coinverse.content.adapter

import android.R.color.white
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getDrawable
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import app.coinverse.R.color
import app.coinverse.R.dimen
import app.coinverse.R.drawable.*
import app.coinverse.R.string.dismiss
import app.coinverse.R.string.save
import app.coinverse.content.models.ContentViewEvent
import app.coinverse.content.models.ContentViewEvent.ContentSwipeDrawed
import app.coinverse.content.models.ContentViewEvent.ContentSwiped
import app.coinverse.utils.*
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.PaymentStatus.FREE
import app.coinverse.utils.PaymentStatus.PAID
import app.coinverse.utils.UserActionType.DISMISS
import app.coinverse.utils.UserActionType.SAVE
import app.coinverse.utils.livedata.Event
import com.mopub.nativeads.MoPubRecyclerAdapter

private val LOG_TAG = ItemTouchHelper::class.java.simpleName

class ItemTouchHelper(val _contentViewEvent: MutableLiveData<Event<ContentViewEvent>>) {

    fun build(context: Context, paymentStatus: PaymentStatus, feedType: FeedType,
              moPubAdapter: MoPubRecyclerAdapter?) = ItemTouchHelper(object : Callback() {

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) =
                if ((paymentStatus == FREE && !moPubAdapter!!.isAd(viewHolder.adapterPosition))
                        || paymentStatus == PAID)
                    when (feedType) {
                        MAIN -> makeMovementFlags(LEFT or RIGHT, LEFT or RIGHT)
                        SAVED -> makeMovementFlags(LEFT, LEFT)
                        DISMISSED -> makeMovementFlags(RIGHT, RIGHT)
                    }
                else makeMovementFlags(ACTION_STATE_IDLE, ACTION_STATE_IDLE)

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder) = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            _contentViewEvent.value = Event(ContentSwiped(
                    feedType,
                    if (direction == RIGHT_SWIPE && feedType != SAVED) SAVE else DISMISS,
                    viewHolder.adapterPosition))
        }

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView,
                                 viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float,
                                 actionState: Int, isCurrentlyActive: Boolean) {
            _contentViewEvent.value = Event(ContentSwipeDrawed(true))
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

                if (dX > 0 && feedType != SAVED) { // Save
                    // Draw icon.
                    icon = getDrawable(context, ic_coinverse_48dp)
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
                } else if (dX < 0 && feedType != DISMISSED) { // Dismiss
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
    })
}