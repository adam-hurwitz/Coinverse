package app.carpecoin.contentFeed

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import app.carpecoin.coin.databinding.FragmentContentFeedBinding
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
        //TODO: Handle savedInstanceState adapter position.
        initializeAdapter()
        swipeToRefreshView.setOnRefreshListener {
            viewModel.contentFeedDataSourceFactory.sourceLiveData.value?.invalidate()
        }
        observeContentSelected()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ContentFeedFragment()
    }

    private fun initializeAdapter() {
        val adapter = ContentAdapter(viewModel)
        contentFeedRecyclerView.layoutManager = LinearLayoutManager(context)
        viewModel.contentList.observe(this, Observer { contentList ->
            adapter.submitList(contentList)
            swipeToRefreshView.isRefreshing = false
        })
        contentFeedRecyclerView.adapter = adapter
    }

    private fun observeContentSelected() {
        viewModel.contentSelected.observe(this, Observer { content ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://${content.id}")))
        })
    }

}


