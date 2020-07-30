package app.coinverse.feed.state

import android.os.Parcel
import android.os.Parcelable
import androidx.paging.PagedList
import app.coinverse.feed.Content
import app.coinverse.utils.ToolbarState
import app.coinverse.utils.UserActionType

sealed class FeedViewState {
    class Feed(
            val toolbarState: ToolbarState = ToolbarState(),
            val feed: PagedList<Content>,
            val error: String? = null
    ) : FeedViewState()

    data class SwipeToRefresh(
            val isEnabled: Boolean = false,
            val error: String? = null
    ) : FeedViewState()

    data class OpenContent(
            val isLoading: Boolean = false,
            val position: Int,
            val contentId: String = "",
            val content: Content = Content(),
            val filePath: String? = "",
            val error: String? = null
    ) : FeedViewState(), Parcelable {
        constructor(parcel: Parcel) : this(
                parcel.readByte() != 0.toByte(),
                parcel.readInt(),
                parcel.readString()!!,
                parcel.readParcelable(Content::class.java.classLoader)!!,
                parcel.readString(),
                parcel.readString())
        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeByte(if (isLoading) 1 else 0)
            parcel.writeInt(position)
            parcel.writeString(contentId)
            parcel.writeParcelable(content, flags)
            parcel.writeString(filePath)
            parcel.writeString(error)
        }
        override fun describeContents() = 0
        companion object CREATOR : Parcelable.Creator<OpenContent> {
            override fun createFromParcel(parcel: Parcel) = OpenContent(parcel)
            override fun newArray(size: Int) = arrayOfNulls<OpenContent?>(size)
        }
    }

    data class SignIn(val position: Int) : FeedViewState()

    data class SwipeContent(val actionType: UserActionType, val position: Int) : FeedViewState()

    class UpdateAds : FeedViewState()

    data class ClearAdjacentAds(val position: Int, val error: String? = null) : FeedViewState()

    data class ShareContent(val content: Content?) : FeedViewState()

    data class OpenContentSource(val url: String) : FeedViewState()
}