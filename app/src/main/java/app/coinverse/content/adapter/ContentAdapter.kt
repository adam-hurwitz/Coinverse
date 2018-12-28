package app.coinverse.content.adapter

import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.databinding.library.baseAdapters.BR
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.coinverse.Enums.FeedType.MAIN
import app.coinverse.Enums.UserActionType
import app.coinverse.R
import app.coinverse.content.ContentViewModel
import app.coinverse.content.models.Content
import app.coinverse.content.models.ContentSelected
import app.coinverse.databinding.CellContentBinding
import app.coinverse.utils.ADAPTER_POSITION_KEY
import app.coinverse.utils.CLICK_SPAM_PREVENTION_THRESHOLD
import com.google.firebase.auth.FirebaseUser
import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject
import kotlinx.android.synthetic.main.cell_content.view.*

private val LOG_TAG = ContentAdapter::class.java.simpleName

private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Content>() {
    override fun areItemsTheSame(oldContent: Content, newContent: Content): Boolean =
            oldContent.id == newContent.id

    override fun areContentsTheSame(oldContent: Content, newContent: Content): Boolean =
            oldContent == newContent
}

class ContentAdapter(var contentViewModel: ContentViewModel) : PagedListAdapter<Content, ContentAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val onContentSelectedSubscriber: ReplaySubject<ContentSelected> = ReplaySubject.create()
    private val onContentSharedSubscriber: ReplaySubject<Content> = ReplaySubject.create()
    private val onContentSourceOpenedSubscriber: ReplaySubject<String> = ReplaySubject.create()

    private var lastClickTime = 0L

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CellContentBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        binding.viewmodel = contentViewModel
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position).let { content -> if (content != null) holder.bind(createOnClickListener(content), content) }
    }

    private fun createOnClickListener(content: Content): OnClickListener {
        return OnClickListener { view ->
            when (view.id) {
                R.id.preview -> {
                    if (SystemClock.elapsedRealtime() - lastClickTime > CLICK_SPAM_PREVENTION_THRESHOLD)
                        onContentSelectedSubscriber.onNext(
                                ContentSelected(view.getTag(ADAPTER_POSITION_KEY) as Int, content))
                    lastClickTime = SystemClock.elapsedRealtime()
                }
                R.id.share -> onContentSharedSubscriber.onNext(content)
                R.id.openSource -> onContentSourceOpenedSubscriber.onNext(content.url)
            }
        }
    }

    fun organizeContent(feedType: String, actionType: UserActionType, itemPosition: Int, user: FirebaseUser) {
        var mainFeedEmptied = false
        if (feedType == MAIN.name) mainFeedEmptied = itemCount == 1
        contentViewModel.organizeContent(feedType, actionType, user, getItem(itemPosition), mainFeedEmptied)
    }

    class ViewHolder(private var binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(onClickListener: OnClickListener, data: Any) {
            binding.setVariable(BR.data, data)
            binding.setVariable(BR.clickListener, onClickListener)
            binding.root.preview.setTag(ADAPTER_POSITION_KEY, layoutPosition)
            binding.executePendingBindings()
        }
    }

    fun onContentSelected(): Observable<ContentSelected> {
        return onContentSelectedSubscriber
    }

    fun onContentShared(): Observable<Content> {
        return onContentSharedSubscriber
    }

    fun onContentSourceOpened(): Observable<String> {
        return onContentSourceOpenedSubscriber
    }

}
