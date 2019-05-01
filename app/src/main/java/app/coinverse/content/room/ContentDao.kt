package app.coinverse.content.room

import androidx.paging.DataSource
import androidx.room.*
import app.coinverse.content.models.Content
import app.coinverse.utils.Enums.FeedType
import com.google.firebase.Timestamp
import java.util.*

@Dao
interface ContentDao {

    @Query("SELECT * FROM content WHERE timestamp >= :timeframe AND feedType = :feedType ORDER BY timestamp DESC")
    fun getMainContentList(timeframe: Timestamp, feedType: FeedType): DataSource.Factory<Int, Content>

    @Query("SELECT * FROM content WHERE feedType = :feedType ORDER BY timestamp DESC")
    fun getCategorizedContentList(feedType: FeedType): DataSource.Factory<Int, Content>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertContentList(users: ArrayList<Content?>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateContentItem(content: Content)
}
