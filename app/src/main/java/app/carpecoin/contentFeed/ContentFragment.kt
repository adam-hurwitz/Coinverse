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
import app.carpecoin.HomeViewModel
import app.carpecoin.coin.databinding.FragmentContentBinding
import app.carpecoin.contentFeed.adapter.ContentAdapter
import app.carpecoin.contentFeed.adapter.ItemTouchHelper
import app.carpecoin.contentFeed.room.ContentDatabase
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.fragment_content.*

private val LOG_TAG = ContentFragment::class.java.simpleName

class ContentFragment : Fragment() {
    private lateinit var binding: FragmentContentBinding
    private lateinit var contentViewModel: ContentViewModel
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentViewModel = ViewModelProviders.of(this).get(ContentViewModel::class.java)
        homeViewModel = ViewModelProviders.of(activity!!).get(HomeViewModel::class.java)
        contentViewModel.contentDatabase = ContentDatabase.getAppDatabase(context!!)
        if (savedInstanceState == null) {
            homeViewModel.isRealtime.observe(this, Observer {
                getContent(it)
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentContentBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(this)
        binding.viewmodel = contentViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //TODO: Handle savedInstanceState adapter position.
        initializeAdapter()
        observeContentSelected()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        observeSignIn()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ContentFragment()
    }

    fun getContent(isRealtime: Boolean) {
        contentViewModel.getContent(isRealtime)
    }

    private fun initializeAdapter() {
        val adapter = ContentAdapter(contentViewModel)
        contentFeedRecyclerView.layoutManager = LinearLayoutManager(context)
        contentViewModel.getContentList().observe(viewLifecycleOwner, Observer { contentList ->
            adapter.submitList(contentList)
            homeViewModel.disableSwipeToRefresh()
        })
        contentFeedRecyclerView.adapter = adapter
        ItemTouchHelper().build(context!!, adapter, fragmentManager!!).attachToRecyclerView(contentFeedRecyclerView)
    }


    private fun observeContentSelected() {
        contentViewModel.contentSelected.observe(this, Observer { content ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://${content.id}")))
        })
    }

    private fun observeSignIn() {
        homeViewModel.user.observe(this, Observer { user: FirebaseUser? ->
            //TODO: Set based on user subscription and preference.
            getContent(true)
        })
    }
}