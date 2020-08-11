package app.coinverse.feed.adapter

import android.R.color.white
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getDrawable
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE
import androidx.recyclerview.widget.ItemTouchHelper.Callback
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import androidx.recyclerview.widget.RecyclerView
import app.coinverse.R.color
import app.coinverse.R.dimen
import app.coinverse.R.drawable.ic_coinverse_48dp
import app.coinverse.R.drawable.ic_dismiss_planet_light_48dp
import app.coinverse.R.drawable.ic_error_black_48dp
import app.coinverse.R.string.dismiss
import app.coinverse.R.string.save
import app.coinverse.feed.state.FeedViewIntent.SwipeContent
import app.coinverse.utils.CELL_CONTENT_MARGIN
import app.coinverse.utils.Event
import app.coinverse.utils.FeedType
import app.coinverse.utils.FeedType.DISMISSED
import app.coinverse.utils.FeedType.MAIN
import app.coinverse.utils.FeedType.SAVED
import app.coinverse.utils.PaymentStatus
import app.coinverse.utils.PaymentStatus.FREE
import app.coinverse.utils.PaymentStatus.PAID
import app.coinverse.utils.RIGHT_SWIPE
import app.coinverse.utils.SWIPE_CONTENT_Y_MARGIN_DP
import app.coinverse.utils.UserActionType.DISMISS
import app.coinverse.utils.UserActionType.SAVE
import app.coinverse.utils.convertDpToPx
import com.mopub.nativeads.MoPubRecyclerAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

private val LOG_TAG = ItemTouchHelper::class.java.simpleName

@ExperimentalCoroutinesApi
fun initItemTouchHelper(
        context: Context,
        resources: Resources,
        paymentStatus: PaymentStatus,
        feedType: FeedType,
        moPubAdapter: MoPubRecyclerAdapter?,
        swipeContent: MutableStateFlow<Event<SwipeContent?>>
) = ItemTouchHelper(object : Callback() {

    /**
     * Enable RecyclerView content item swiping, disable ad item swiping.
     *
     * @param recyclerView RecyclerView
     * @param viewHolder ViewHolder
     * @return Int
     */
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
        swipeContent.value = Event(SwipeContent(
                feedType = feedType,
                actionType = if (direction == RIGHT_SWIPE && feedType != SAVED) SAVE else DISMISS,
                position = viewHolder.adapterPosition,
                isSwiped = true
        ))
    }

    override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean) {
        swipeContent.value = Event(SwipeContent(
                feedType = feedType,
                actionType = if (dX > 0 && feedType != SAVED) SAVE else DISMISS,
                position = viewHolder.adapterPosition,
                isSwiped = false
        ))

        if (actionState == ACTION_STATE_SWIPE) {
            var icon = getDrawable(context, ic_error_black_48dp)
            val iconLeft: Int
            val iconRight: Int
            val background: ColorDrawable
            val itemView = viewHolder.itemView
            val margin = convertDpToPx(resources, CELL_CONTENT_MARGIN)
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

            if (dX > 0 && feedType != SAVED) { // Saved
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
                yTextPosition = (iconBottom + convertDpToPx(resources, SWIPE_CONTENT_Y_MARGIN_DP)).toFloat()
                c.drawText(action, xTextPosition, yTextPosition, paint)
            } else if (dX < 0 && feedType != DISMISSED) { // Dismissed
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
                yTextPosition = (iconBottom + convertDpToPx(resources, SWIPE_CONTENT_Y_MARGIN_DP)).toFloat()
                c.drawText(action, xTextPosition, yTextPosition, paint)
            }
        }
    }
})
