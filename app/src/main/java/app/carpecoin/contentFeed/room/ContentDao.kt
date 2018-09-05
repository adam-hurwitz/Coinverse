package app.carpecoin.contentFeed.room

import androidx.paging.DataSource
import androidx.room.*
import app.carpecoin.contentFeed.models.Content
import java.util.*

@Dao
interface ContentDao {

    @Query("SELECT * FROM content WHERE timestamp >= :timeframe ORDER BY qualityScore DESC")
    fun getAllPaged(timeframe: Date): DataSource.Factory<Int, Content>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(users: ArrayList<Content?>)

    @Delete
    fun delete(content: Content)
}
