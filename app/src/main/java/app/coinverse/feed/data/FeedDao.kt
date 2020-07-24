package app.coinverse.feed.data

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.coinverse.feed.Content
import app.coinverse.utils.FeedType
import com.google.firebase.Timestamp

@Dao
interface FeedDao {

    @Query("SELECT * FROM content WHERE timestamp >= :timeframe AND feedType = :feedType ORDER BY timestamp DESC")
    fun getMainFeedRoom(timeframe: Timestamp, feedType: FeedType): DataSource.Factory<Int, Content>

    @Query("SELECT * FROM content WHERE feedType = :feedType ORDER BY timestamp DESC")
    fun getLabeledFeedRoom(feedType: FeedType): DataSource.Factory<Int, Content>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFeed(feed: List<Content>?)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateFeed(feed: List<Content>?)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateContent(content: Content)
}
