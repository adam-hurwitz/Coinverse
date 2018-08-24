package app.carpecoin.contentFeed

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import app.carpecoin.ViewHolder
import app.carpecoin.coin.databinding.CellContentFeedBinding
import app.carpecoin.coin.databinding.FragmentContentFeedBinding
import app.carpecoin.contentFeed.models.Content
import app.carpecoin.utils.Constants.PAGE_SIZE
import app.carpecoin.utils.Constants.PREFETCH_DISTANCE
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.LoadingState
import com.firebase.ui.firestore.paging.LoadingState.LOADING_INITIAL
import com.firebase.ui.firestore.paging.LoadingState.LOADED
import com.firebase.ui.firestore.paging.LoadingState.LOADING_MORE
import com.firebase.ui.firestore.paging.LoadingState.FINISHED
import com.firebase.ui.firestore.paging.LoadingState.ERROR
import android.content.Intent
import android.net.Uri
import kotlinx.android.synthetic.main.fragment_content_feed.*


private val LOG_TAG = ContentFeedFragment::class.java.simpleName

class ContentFeedFragment : Fragment() {

    private lateinit var binding: FragmentContentFeedBinding
    private lateinit var viewModel: ContentFeedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ContentFeedViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentContentFeedBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(this)
        binding.viewmodel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //TODO: Handle savedInstanceState adapter position
        initializeFirestoreAdapter()
        swipeToRefreshView.setOnRefreshListener {
            initializeFirestoreAdapter()
        }
        observeContentSelection()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ContentFeedFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = ContentFeedFragment()
    }

    fun initializeData() {
        //viewModel.initializeData(viewModel.isRealtimeDataEnabled)
    }

    private fun initializeFirestoreAdapter() {

        val config: PagedList.Config = PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPrefetchDistance(PREFETCH_DISTANCE)
                .setPageSize(PAGE_SIZE)
                .build()

        val options: FirestorePagingOptions<Content> = FirestorePagingOptions.Builder<Content>()
                .setLifecycleOwner(this)
                .setQuery(viewModel.contentFeedQuery, config, Content::class.java).build()

        val adapter = object : FirestorePagingAdapter<Content, ViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val binding =
                        CellContentFeedBinding.inflate(layoutInflater, parent, false)
                binding.viewmodel = viewModel
                val viewHolder = ViewHolder(binding)
                return viewHolder
            }

            override fun onBindViewHolder(viewHolder: ViewHolder, position: Int, content: Content) {
                viewHolder.bind(content)
            }

            override fun onLoadingStateChanged(state: LoadingState) {
                when (state) {
                    LOADING_INITIAL -> Log.v(LOG_TAG,
                            String.format("onLoadingStateChanged(): %s", LOADING_INITIAL))
                    LOADED -> swipeToRefreshView.isRefreshing = false
                    LOADING_MORE -> Log.v(LOG_TAG,
                            String.format("onLoadingStateChanged(): %s", LOADING_MORE))
                    FINISHED -> swipeToRefreshView.isRefreshing = false
                    ERROR -> Log.v(LOG_TAG,
                            String.format("onLoadingStateChanged(): %s", ERROR))
                }
            }
        }
        contentFeedRecyclerView.layoutManager = LinearLayoutManager(context)
        contentFeedRecyclerView.adapter = adapter
    }

    private fun observeContentSelection() {
        viewModel.contentSelected.observe(this, Observer { content ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://${content.id}")))
        })
    }

    //TODO: Keep in case Adapter is refactored to PagedListAdapter.
    /*private fun observeContentFeed() {
        viewModel.contentFeedLiveData.observe(
                this, Observer { contentFeedList ->
            for (content in contentFeedList!!) {
                println("CONTENT_ADDED:" + content)
            }
        })
    }*/

}


