package app.coinverse.content.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import app.coinverse.Enums.FeedType.ARCHIVED
import app.coinverse.Enums.FeedType.SAVED
import app.coinverse.Enums.UserActionType.ARCHIVE
import app.coinverse.Enums.UserActionType.SAVE
import app.coinverse.HomeViewModel
import app.coinverse.coin.R
import app.coinverse.user.SignInDialogFragment
import app.coinverse.utils.Constants
import app.coinverse.utils.Utils
import com.google.firebase.auth.FirebaseAuth

private val LOG_TAG = ItemTouchHelper::class.java.simpleName

private const val RIGHT_SWIPE = 8
private const val LEFT_SWIPE = 4

class ItemTouchHelper(var homeViewModel: HomeViewModel) {

    fun build(context: Context, feedType: String, adapter: ContentAdapter,
              fragmentManager: FragmentManager): ItemTouchHelper {
        return ItemTouchHelper(object : Callback() {

            override fun getMovementFlags(recyclerView: RecyclerView,
                                          viewHolder: RecyclerView.ViewHolder): Int =
                    makeMovementFlags(0, LEFT or RIGHT)

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    if (direction == RIGHT_SWIPE && feedType != SAVED.name) { // Save
                        adapter.organizeContent(feedType, SAVE, viewHolder.adapterPosition, user)
                    } else if (direction == LEFT_SWIPE && feedType != ARCHIVED.name) { // Archive
                        adapter.organizeContent(feedType, ARCHIVE, viewHolder.adapterPosition, user)
                    }
                } else {
                    signInDialog(viewHolder as ViewHolder)
                }
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView,
                                     viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float,
                                     actionState: Int, isCurrentlyActive: Boolean) {
                homeViewModel.enableSwipeToRefresh(false)
                if (actionState == ACTION_STATE_SWIPE) {
                    var icon = ContextCompat.getDrawable(context, R.drawable.ic_error_black_48dp)
                    val iconLeft: Int
                    val iconRight: Int
                    val background: ColorDrawable
                    val itemView = viewHolder.itemView
                    val margin = Utils.convertDpToPx(Constants.CELL_CONTENT_MARGIN)
                    val iconWidth = icon!!.intrinsicWidth
                    val iconHeight = icon.intrinsicHeight
                    val cellHeight = itemView.bottom - itemView.top
                    val iconTop = itemView.top + (cellHeight - iconHeight) / 2
                    val iconBottom = iconTop + iconHeight

                    if (dX > 0 && feedType != SAVED.name) { // Save
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_save_white_48dp)
                        background = ColorDrawable(ContextCompat.getColor(context, R.color.colorPrimary))
                        background.setBounds(0, itemView.top, (itemView.left + dX).toInt(), itemView.bottom)
                        iconLeft = margin
                        iconRight = margin + iconWidth
                        background.draw(c)
                        icon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon?.draw(c)
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    } else if (dX < 0 && feedType != ARCHIVED.name) { // Archive
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_check_white_48dp)
                        background = ColorDrawable(ContextCompat.getColor(context, R.color.colorAccent))
                        background.setBounds((itemView.right - dX).toInt(), itemView.top, 0, itemView.bottom)
                        iconLeft = itemView.right - margin - iconWidth
                        iconRight = itemView.right - margin
                        background.draw(c)
                        icon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon?.draw(c)
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    } else {
                        homeViewModel.enableSwipeToRefresh(true)
                    }
                }
            }

            private fun signInDialog(viewHolder: ViewHolder) {
                SignInDialogFragment.newInstance().show(fragmentManager, Constants.SIGNIN_DIALOG_FRAGMENT_TAG)
                adapter.notifyItemChanged(viewHolder.adapterPosition)
            }

        })
    }
}

