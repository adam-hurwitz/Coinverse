package app.coinverse.feed.adapter

import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.coinverse.R.id.contentTypeLogo
import app.coinverse.R.id.openSource
import app.coinverse.R.id.preview
import app.coinverse.R.id.share
import app.coinverse.databinding.CellContentBinding
import app.coinverse.feed.Content
import app.coinverse.feed.state.FeedViewIntent
import app.coinverse.feed.state.FeedViewIntent.OpenContent
import app.coinverse.utils.Event
import app.coinverse.utils.setContentTypeIcon
import app.coinverse.utils.setImageUrlRounded
import app.coinverse.utils.setTimePostedAgo
import app.coinverse.utils.setYouTubeLogo
import kotlinx.coroutines.ExperimentalCoroutinesApi

private val LOG_TAG = FeedAdapter::class.java.simpleName

private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Content>() {
    override fun areItemsTheSame(oldContent: Content, newContent: Content): Boolean =
            oldContent.id == newContent.id

    override fun areContentsTheSame(oldContent: Content, newContent: Content): Boolean =
            oldContent == newContent
}

@ExperimentalCoroutinesApi
class FeedAdapter(
        private val intent: FeedViewIntent
) : PagedListAdapter<Content, FeedAdapter.ViewHolder>(DIFF_CALLBACK) {
    val loadingIds: HashSet<String> = hashSetOf()

    class ViewHolder(private var binding: CellContentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(content: Content, loadingIds: HashSet<String>, onClickListener: OnClickListener) {
            binding.cellContentFeed.setOnClickListener(onClickListener)
            binding.creator.text = content.creator
            binding.timeAgo.setTimePostedAgo(content.timestamp.toDate().time)
            binding.contentTypeLogo.setContentTypeIcon(content.contentType)
            binding.youTubeLogo.setYouTubeLogo(content.contentType)
            binding.contentTypeLogo.setOnClickListener(onClickListener)
            binding.preview.setImageUrlRounded(content.previewImage)
            binding.preview.setOnClickListener(onClickListener)
            binding.progressBar.visibility = if (loadingIds.contains(content.id)) VISIBLE else GONE
            binding.titleToolbar.text = content.title
            binding.openSource.setOnClickListener(onClickListener)
            binding.share.setOnClickListener(onClickListener)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CellContentBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { content ->
            holder.bind(content, loadingIds, createOnClickListener(content, position))
        }
    }

    private fun createOnClickListener(content: Content, position: Int) = OnClickListener { view ->
        when (view.id) {
            preview, contentTypeLogo -> intent.openContent.value =
                    Event(OpenContent(content, position))
            share -> intent.shareContent.value = Event(content)
            openSource -> intent.openContentSource.value = Event(content.url)
        }
    }

    fun getContent(position: Int) = getItem(position)
}