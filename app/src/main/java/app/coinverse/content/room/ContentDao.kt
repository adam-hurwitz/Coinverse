package app.coinverse.content.room

import androidx.paging.DataSource
import androidx.room.*
import app.coinverse.Enums.FeedType
import app.coinverse.content.models.Content
import com.google.firebase.Timestamp
import java.util.*

@Dao
interface ContentDao {

    @Query("SELECT * FROM content WHERE timestamp >= :timeframe AND feedType = :feedType ORDER BY qualityScore DESC")
    fun getMainContent(timeframe: Timestamp, feedType: FeedType): DataSource.Factory<Int, Content>

    @Query("SELECT * FROM content WHERE feedType = :feedType ORDER BY timestamp DESC")
    fun getCategorizedContent(feedType: FeedType): DataSource.Factory<Int, Content>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertContent(users: ArrayList<Content?>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateContent(content: Content)
}
