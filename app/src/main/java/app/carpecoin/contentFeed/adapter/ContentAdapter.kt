package app.carpecoin.contentFeed.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import app.carpecoin.ViewHolder
import app.carpecoin.coin.databinding.CellContentFeedBinding
import app.carpecoin.contentFeed.ContentFeedViewModel
import app.carpecoin.contentFeed.models.Content
import app.carpecoin.firebase.FirestoreCollections.ARCHIVED_COLLECTION
import app.carpecoin.firebase.FirestoreCollections.usersCollection
import com.google.firebase.auth.FirebaseUser

private val LOG_TAG = ContentAdapter::class.java.simpleName

private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Content>() {
    override fun areItemsTheSame(oldContent: Content, newContent: Content): Boolean =
            oldContent.id == newContent.id

    override fun areContentsTheSame(oldContent: Content, newContent: Content): Boolean =
            oldContent == newContent
}

class ContentAdapter(var viewModel: ContentFeedViewModel)
    : PagedListAdapter<Content, ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CellContentFeedBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        binding.viewmodel = viewModel
        val viewHolder = ViewHolder(binding)
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        println("QUALITY_SCORE: "
                + getItem(position)?.timestamp + " " + getItem(position)?.qualityScore +
                " " + getItem(position)?.contentTitle)
        holder.bind(getItem(position)!!)
    }

    fun removeItem(itemPosition: Int, user: FirebaseUser) {
        val content = getItem(itemPosition)
        usersCollection.document(user.uid)
                .collection(ARCHIVED_COLLECTION)
                .document(content!!.contentTitle)
                .set(content)
                .addOnSuccessListener {
                    Log.v(LOG_TAG, String.format("Content added to archivedCollection:%s", it))
                }.addOnFailureListener {
                    Log.v(LOG_TAG, String.format("Content failed to be added to archivedCollection:%s", it))
                }
    }

}
