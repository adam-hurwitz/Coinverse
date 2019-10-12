package app.coinverse.utils

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.paging.LimitOffsetDataSource
import app.coinverse.content.ContentViewModel
import app.coinverse.contentviewmodel.LabelContentTest
import app.coinverse.utils.livedata.Event
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

fun LabelContentTest.mockUser() =
        if (this.isUserSignedIn) FirebaseAuth.getInstance().currentUser else null

fun ContentViewModel.feedViewState() = this.feedViewState.getOrAwaitValue()
fun ContentViewModel.playerViewState() = this.playerViewState.getOrAwaitValue()
fun ContentViewModel.viewEffects() = this.viewEffect.getOrAwaitValue()
fun <T> LiveData<Event<T>>.observe() = this.getOrAwaitValue().peekEvent()

/**
 * Gets the value of a [LiveData] or waits for it to have one, with a timeout.
 *
 * Use this extension from host-side (JVM) tests. It's recommended to use it alongside
 * `InstantTaskExecutorRule` or a similar mechanism to execute tasks synchronously.
 */
fun <T> LiveData<T>.getOrAwaitValue(
        time: Long = 5,
        timeUnit: TimeUnit = TimeUnit.SECONDS,
        afterObserve: () -> Unit = {}
): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(o: T?) {
            data = o
            latch.countDown()
            this@getOrAwaitValue.removeObserver(this)
        }
    }
    this.observeForever(observer)
    afterObserve.invoke()
    // Don't wait indefinitely if the LiveData is not set.
    if (!latch.await(time, timeUnit)) {
        this.removeObserver(observer)
        throw TimeoutException("LiveData value was never set.")
    }
    @Suppress("UNCHECKED_CAST")
    return data as T
}

/**
 * Observes a [LiveData] until the `block` is done executing.
 */
fun <T> LiveData<T>.observeForTesting(block: () -> Unit) {
    val observer = Observer<T> { }
    try {
        observeForever(observer)
        block()
    } finally {
        removeObserver(observer)
    }
}

// Mock PagedList.
fun <T> List<T>.asPagedList(config: PagedList.Config? = null) =
        LivePagedListBuilder<Int, T>(createMockDataSourceFactory(this),
                config ?: PagedList.Config.Builder()
                        .setEnablePlaceholders(false)
                        .setPageSize(1)
                        .setPageSize(if (this.isEmpty()) 1 else size)
                        .setMaxSize((if (this.isEmpty()) 1 else size) + 2)
                        .setPrefetchDistance(1)
                        .build())
                .build().getOrAwaitValue()

private fun <T> createMockDataSourceFactory(itemList: List<T>): DataSource.Factory<Int, T> =
        object : DataSource.Factory<Int, T>() {
            override fun create(): DataSource<Int, T> = MockLimitDataSource(itemList)
        }

private val mockQuery = mockk<RoomSQLiteQuery> { every { sql } returns "" }

private val mockDb = mockk<RoomDatabase> {
    every { invalidationTracker } returns mockk(relaxUnitFun = true)
}

class MockLimitDataSource<T>(private val itemList: List<T>)
    : LimitOffsetDataSource<T>(mockDb, mockQuery, false, null) {
    override fun convertRows(cursor: Cursor?): MutableList<T> = itemList.toMutableList()
    override fun countItems(): Int = itemList.count()
    override fun isInvalid(): Boolean = false
    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) { /* Not implemented */
    }

    override fun loadRange(startPosition: Int, loadCount: Int) =
            itemList.subList(startPosition, startPosition + loadCount).toMutableList()

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<T>) {
        callback.onResult(itemList, 0)
    }
}