package app.coinverse.content.room

import androidx.paging.DataSource
import androidx.room.*
import app.coinverse.content.models.Content
import app.coinverse.utils.FeedType
import com.google.firebase.Timestamp

@Dao
interface ContentDao {

    @Query("SELECT * FROM content WHERE timestamp >= :timeframe AND feedType = :feedType ORDER BY timestamp DESC")
    fun queryMainContentList(timeframe: Timestamp, feedType: FeedType): DataSource.Factory<Int, Content>

    @Query("SELECT * FROM content WHERE feedType = :feedType ORDER BY timestamp DESC")
    fun queryLabeledContentList(feedType: FeedType): DataSource.Factory<Int, Content>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentList(users: List<Content>?)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateContent(content: Content)
}
