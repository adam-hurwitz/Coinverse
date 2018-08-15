package app.carpecoin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import app.carpecoin.coin.R
import app.carpecoin.coin.databinding.FragmentHomeBinding
import app.carpecoin.contentFeed.ContentFeedFragment
import app.carpecoin.priceGraph.PriceGraphFragment

/**
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class HomeFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null

    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(this)
        if (savedInstanceState == null) {
            fragmentManager
                    ?.beginTransaction()
                    ?.add(binding.priceDataContainer.id, PriceGraphFragment.newInstance())
                    ?.commit()
            fragmentManager
                    ?.beginTransaction()
                    ?.add(binding.contentFeedContainer.id, ContentFeedFragment.newInstance())
                    ?.commit()
        }
        return binding.root
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) = HomeFragment()
    }

    //TODO: Fix swipe to refresh and CollapasingToolBar overlap.
    fun setRefreshStatus(isRealTimeDataEnabled: Boolean) {
        if (isRealTimeDataEnabled) {
            binding.swipeToRefresh.isRefreshing = false
            binding.swipeToRefresh.isEnabled = false
        } else {
            binding.swipeToRefresh.setOnRefreshListener {
                (fragmentManager?.findFragmentById(R.id.priceDataContainer) as PriceGraphFragment)
                        .initializeData()
                //TODO: Decide 1 or 2 SwipeToRefresh for screen.
                /*(fragmentManager?.findFragmentById(R.id.contentFeedContainer) as ContentFeedFragment)
                        .initializeData()*/
            }
        }
    }

    fun disableSwipeToRefresh(){
        binding.swipeToRefresh.isRefreshing = false
    }

}
