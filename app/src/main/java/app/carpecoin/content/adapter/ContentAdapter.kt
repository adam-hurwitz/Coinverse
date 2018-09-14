package app.carpecoin.content.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import app.carpecoin.Enums.FeedType.ARCHIVED
import app.carpecoin.Enums.FeedType.SAVED
import app.carpecoin.Enums.UserAction
import app.carpecoin.Enums.UserAction.ARCHIVE
import app.carpecoin.Enums.UserAction.SAVE
import app.carpecoin.coin.databinding.CellContentBinding
import app.carpecoin.content.ContentRepository.deleteContent
import app.carpecoin.content.ContentRepository.setContent
import app.carpecoin.content.ContentViewModel
import app.carpecoin.content.models.Content
import app.carpecoin.firebase.FirestoreCollections.ARCHIVED_COLLECTION
import app.carpecoin.firebase.FirestoreCollections.SAVED_COLLECTION
import app.carpecoin.firebase.FirestoreCollections.usersCollection
import com.google.firebase.auth.FirebaseUser

private val LOG_TAG = ContentAdapter::class.java.simpleName

private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Content>() {
    override fun areItemsTheSame(oldContent: Content, newContent: Content): Boolean =
            oldContent.id == newContent.id

    override fun areContentsTheSame(oldContent: Content, newContent: Content): Boolean =
            oldContent == newContent
}

class ContentAdapter(var contentViewmodel: ContentViewModel) : PagedListAdapter<Content, ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CellContentBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        binding.viewmodel = contentViewmodel
        val viewHolder = ViewHolder(binding)
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position)!!)
    }

    fun updateItem(feedType: String, action: UserAction, itemPosition: Int, user: FirebaseUser) {
        val userReference = usersCollection.document(user.uid)
        val content = getItem(itemPosition)
        if (action == SAVE) {
            if (feedType == ARCHIVED.name) {
                deleteContent(userReference, ARCHIVED_COLLECTION, content)
            }
            content?.feedType = SAVED
            setContent(contentViewmodel, userReference, SAVED_COLLECTION, content)
        } else if (action == ARCHIVE) {
            if (feedType == SAVED.name) {
                deleteContent(userReference, SAVED_COLLECTION, content)
            }
            content?.feedType = ARCHIVED
            setContent(contentViewmodel, userReference, ARCHIVED_COLLECTION, content)
        }
    }

}
