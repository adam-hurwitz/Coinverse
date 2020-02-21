package app.coinverse.feed.room

import androidx.paging.DataSource
import androidx.room.*
import app.coinverse.feed.models.Content
import app.coinverse.utils.FeedType
import com.google.firebase.Timestamp

@Dao
interface FeedDao {

    @Query("SELECT * FROM content WHERE timestamp >= :timeframe AND feedType = :feedType ORDER BY timestamp DESC")
    fun getMainFeedRoom(timeframe: Timestamp, feedType: FeedType): DataSource.Factory<Int, Content>

    @Query("SELECT * FROM content WHERE feedType = :feedType ORDER BY timestamp DESC")
    fun getLabeledFeedRoom(feedType: FeedType): DataSource.Factory<Int, Content>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(users: List<Content>?)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateContent(content: Content)
}
