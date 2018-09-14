package app.carpecoin.content.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import app.carpecoin.Enums.FeedType.ARCHIVED
import app.carpecoin.Enums.FeedType.SAVED
import app.carpecoin.Enums.UserAction.ARCHIVE
import app.carpecoin.Enums.UserAction.SAVE
import app.carpecoin.coin.R
import app.carpecoin.user.SignInDialogFragment
import app.carpecoin.utils.Constants
import app.carpecoin.utils.Utils
import com.google.firebase.auth.FirebaseAuth

private val LOG_TAG = ItemTouchHelper::class.java.simpleName

private const val RIGHT_SWIPE = 8
private const val LEFT_SWIPE = 4

class ItemTouchHelper {

    fun build(context: Context, feedType: String, adapter: ContentAdapter, fragmentManager: FragmentManager): ItemTouchHelper {
        return ItemTouchHelper(object : ItemTouchHelper.Callback() {

            override fun getMovementFlags(recyclerView: RecyclerView,
                                          viewHolder: RecyclerView.ViewHolder): Int =
                    makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    if (direction == RIGHT_SWIPE && feedType != SAVED.name) { // Save
                        adapter.updateItem(feedType, SAVE, viewHolder.adapterPosition, user)
                    } else if (direction == LEFT_SWIPE && feedType != ARCHIVED.name) { // Archive
                        adapter.updateItem(feedType, ARCHIVE, viewHolder.adapterPosition, user)
                    }
                } else {
                    signInDialog(viewHolder as ViewHolder)
                }
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    var icon = ContextCompat.getDrawable(context, R.drawable.ic_error_outline_black_48dp)
                    var iconLeft = 0
                    var iconRight = 0

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

