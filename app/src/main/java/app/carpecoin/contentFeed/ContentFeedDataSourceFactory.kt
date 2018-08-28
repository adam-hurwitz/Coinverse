package app.carpecoin.contentFeed

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import app.carpecoin.Enums.Timeframe
import app.carpecoin.contentFeed.models.Content
import java.util.*

class ContentFeedDataSourceFactory(var timeframe: MutableLiveData<Timeframe>)
    : DataSource.Factory<Date, Content>() {
    var sourceLiveData = MutableLiveData<ContentFeedDataSource>()
    override fun create(): DataSource<Date, Content> {
        val source = ContentFeedDataSource(timeframe)
        sourceLiveData = MutableLiveData()
        sourceLiveData.postValue(source)
        return source
    }
}
