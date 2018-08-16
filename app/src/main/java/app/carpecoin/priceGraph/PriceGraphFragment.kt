package app.carpecoin.priceGraph

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import app.carpecoin.coin.R
import app.carpecoin.priceGraph.models.PriceGraphData
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import app.carpecoin.priceGraph.models.PriceGraphLiveData
import app.carpecoin.Enums.Exchange.BINANCE
import app.carpecoin.Enums.Exchange.GEMINI
import app.carpecoin.Enums.Exchange.KUCOIN
import app.carpecoin.Enums.Exchange.KRAKEN
import app.carpecoin.Enums.Exchange.GDAX
import app.carpecoin.Enums.OrderType
import app.carpecoin.Enums.OrderType.ASK
import app.carpecoin.Enums.OrderType.BID
import app.carpecoin.Enums.Timeframe
import app.carpecoin.Enums.Timeframe.DAY
import app.carpecoin.priceGraph.models.PriceGraphXAndYConstraints
import com.jjoe64.graphview.GridLabelRenderer
import app.carpecoin.Enums.Exchange
import app.carpecoin.priceGraph.models.PercentDifference
import app.carpecoin.utils.ExchangeColors
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import app.carpecoin.Enums
import app.carpecoin.HomeFragment
import app.carpecoin.coin.databinding.FragmentPriceGraphBinding
import kotlinx.android.synthetic.main.fragment_price_graph.view.*

private val dataPointRadiusValue = TypedValue()

class PriceGraphFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var binding: FragmentPriceGraphBinding
    private lateinit var viewModel: PriceDataViewModel
    private lateinit var homeFragment: HomeFragment

    private var enabledExchangesList: ArrayList<Enums.Exchange?>? = ArrayList()
    private var graphSeriesMap = hashMapOf(
            Pair(Enums.Exchange.GDAX, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(Enums.Exchange.BINANCE, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(Enums.Exchange.GEMINI, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(Enums.Exchange.KUCOIN, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(Enums.Exchange.KRAKEN, PriceGraphData(LineGraphSeries(), LineGraphSeries())))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentPriceGraphBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(this)
        viewModel = ViewModelProviders.of(this).get(PriceDataViewModel::class.java)
        binding.viewmodel = viewModel
        homeFragment = (parentFragment?.fragmentManager?.findFragmentById(R.id.navHostFragment)
                ?.childFragmentManager?.findFragmentById(R.id.navHostFragment) as HomeFragment)

        //FIXME: Debugging graphLiveData being observed multiple times when back is pressed.
        println(String.format("DOUBLE_GRAPH: onCreateView()"))

        setPriceGraphStyle()
        setRealtimeDataConfiguration()
        observeExchangesEnabled()
        observeGraphData()
        observeGraphConstraints()
        observePriceDifferenceDetails()

        if (savedInstanceState == null) {
            setGraphVisibility(View.GONE)
            initializeData()
        }
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() = PriceGraphFragment()
    }

    fun initializeData() {
        viewModel.initializeData(viewModel.isRealtimeDataEnabled)
    }

    private fun setPriceGraphStyle() {
        binding.priceGraph.viewport.isXAxisBoundsManual = true
        val graphLabels = binding.priceGraph.gridLabelRenderer
        graphLabels.gridStyle = GridLabelRenderer.GridStyle.NONE
        graphLabels.isHorizontalLabelsVisible = false
        graphLabels.isVerticalLabelsVisible = false
        resources.getValue(R.dimen.data_point_radius, dataPointRadiusValue, true)
        viewModel.timeframe.observe(this, Observer { timeframe: Timeframe? ->
            when (timeframe) {
                DAY -> binding.timeframe.text = resources.getString(R.string.timeframe_last_day)
                else -> binding.timeframe.text = resources.getString(R.string.timeframe_last_day)
            }
        })
    }

    private fun setRealtimeDataConfiguration() {
        viewModel.isRealtimeDataEnabled.let {
            homeFragment.setRefreshStatus(it)
            if (it) {
                binding.priceGraph.viewport.isScrollable = true
            } else {
                binding.priceGraph.viewport.isScalable = true
            }
        }
    }

    private fun observeExchangesEnabled() {
        viewModel.enabledExchanges.observe(this, Observer { enabledExchangeList: ArrayList<Exchange?>? ->

            enabledExchangesList = enabledExchangeList
            binding.priceGraph.removeAllSeries()

            setPriceGraphToggleColor(enabledExchangeList?.contains(GDAX) ?: false,
                    binding.coinbaseToggle)
            setPriceGraphToggleColor(enabledExchangeList?.contains(BINANCE) ?: false,
                    binding.binanceToggle)
            setPriceGraphToggleColor(enabledExchangeList?.contains(GEMINI) ?: false,
                    binding.geminiToggle)
            setPriceGraphToggleColor(enabledExchangeList?.contains(KUCOIN) ?: false,
                    binding.kucoinToggle)
            setPriceGraphToggleColor(enabledExchangeList?.contains(KRAKEN) ?: false,
                    binding.krakenToggle)

            for (exchange in enabledExchangeList!!) {
                val asks = graphSeriesMap[exchange]?.asks
                val bids = graphSeriesMap[exchange]?.bids
                if (enabledExchangeList.contains(exchange)) {
                    binding.priceGraph.addSeries(asks)
                    binding.priceGraph.addSeries(bids)
                } else {
                    binding.priceGraph.removeSeries(asks)
                    binding.priceGraph.removeSeries(bids)
                }
            }
            binding.priceGraph.refreshDrawableState()
        })
    }

    private fun setPriceGraphToggleColor(isToggled: Boolean?,
                                         toggleView: TextView) {
        if (isToggled == true) {
            toggleView.setTextColor(ContextCompat.getColor(context!!, R.color.colorAccent))
        } else {
            toggleView.setTextColor(ContextCompat.getColor(context!!, R.color.colorPrimaryDark))
        }
    }

    private fun observeGraphData() {
        viewModel.graphLiveData.observe(
                this, Observer { priceGraphDataMap: HashMap<Exchange, PriceGraphLiveData>? ->

            //FIXME: Debugging graphLiveData being observed multiple times when back is pressed.
            println(String.format("DOUBLE_GRAPH: observeGraphData"))

            for (priceGraphData in priceGraphDataMap!!.entries) {
                val exchange = priceGraphData.key
                setExchangeGraphDataAndStyle(exchange, ASK, graphSeriesMap[exchange]?.asks,
                        priceGraphDataMap)
                setExchangeGraphDataAndStyle(exchange, BID, graphSeriesMap[exchange]?.bids,
                        priceGraphDataMap)
            }
        })
    }

    private fun observeGraphConstraints() {
        viewModel.priceGraphXAndYConstraintsLiveData.observe(
                this,
                Observer { priceGraphXAndYConstraints: PriceGraphXAndYConstraints? ->
                    binding.priceGraph.viewport.setMinY(priceGraphXAndYConstraints?.minY
                            ?: 0.0)
                    binding.priceGraph.viewport.setMaxY(priceGraphXAndYConstraints?.maxY
                            ?: 0.0)
                    binding.priceGraph.viewport.setMinX(priceGraphXAndYConstraints?.minX
                            ?: 0.0)
                    binding.priceGraph.viewport.setMaxX(priceGraphXAndYConstraints?.maxX
                            ?: 0.0)
                    binding.priceGraph.onDataChanged(true, false)
                })
    }

    private fun observePriceDifferenceDetails() {
        viewModel.priceDifferenceDetailsLiveData.observe(this, Observer {
            percentDifference: PercentDifference? ->
            updatePriceDifferenceIndicators(ConstraintSet(), percentDifference?.askExchange,
                    binding.baseToQuoteAsk.id)
            updatePriceDifferenceIndicators(ConstraintSet(), percentDifference?.bidExchange,
                    binding.baseToQuoteBid.id)
        })
    }

    private fun updatePriceDifferenceIndicators(constraintSet: ConstraintSet,
                                                exchange: Exchange?,
                                                orderLayoutId: Int) {
        constraintSet.clone(binding.activityMainConstraint.card_price_constraint)
        var textViewId: Int
        val margin = resources.getInteger(R.integer.price_graph_base_to_quote_margin)
        when (exchange) {
            GDAX -> textViewId = binding.coinbaseToggle.id
            BINANCE -> textViewId = binding.binanceToggle.id
            GEMINI -> textViewId = binding.geminiToggle.id
            KUCOIN -> textViewId = binding.kucoinToggle.id
            KRAKEN -> textViewId = binding.krakenToggle.id
            else -> textViewId = 0
        }
        constraintSet.connect(orderLayoutId, ConstraintSet.TOP, textViewId,
                ConstraintSet.BOTTOM, margin)
        constraintSet.connect(orderLayoutId, ConstraintSet.LEFT, textViewId,
                ConstraintSet.LEFT, margin)
        constraintSet.connect(orderLayoutId, ConstraintSet.RIGHT, textViewId,
                ConstraintSet.RIGHT, margin)
        constraintSet.applyTo(binding.activityMainConstraint.card_price_constraint)
    }

    private fun setExchangeGraphDataAndStyle(exchange: Exchange, orderType: OrderType,
                                             orders: LineGraphSeries<DataPoint>?,
                                             priceGraphDataMap: HashMap<Exchange, PriceGraphLiveData>?) {
        binding.priceGraph.removeSeries(orders)
        if (priceGraphDataMap != null) {
            val orders: LineGraphSeries<DataPoint>?
            val color: Int
            val thickness: Int
            if (orderType == ASK) {
                graphSeriesMap[exchange]?.asks = priceGraphDataMap[exchange]?.asksLiveData?.value
                orders = graphSeriesMap[exchange]?.asks
                color = ExchangeColors.get(exchange, ASK)
                thickness = resources.getInteger(R.integer.price_graph_asks_thickness)
            } else {
                graphSeriesMap[exchange]?.bids = priceGraphDataMap[exchange]?.bidsLiveData?.value
                orders = graphSeriesMap[exchange]?.bids
                color = ExchangeColors.get(exchange, BID)
                thickness = resources.getInteger(R.integer.price_graph_bids_thickness)
            }

            //TODO: Make swipe-to-refresh smoother.
            setGraphVisibility(View.VISIBLE)
            homeFragment.disableSwipeToRefresh()

            setOrderPriceGraphStyle(orders, color, thickness)
            if (enabledExchangesList?.contains(exchange) == true) {
                binding.priceGraph.addSeries(orders)
            }
        }
    }

    private fun setOrderPriceGraphStyle(orders: LineGraphSeries<DataPoint>?, color: Int,
                                        thickness: Int) {
        orders?.color = ContextCompat.getColor(context!!, color)
        orders?.thickness = thickness
        orders?.isDrawDataPoints = false
        orders?.dataPointsRadius = dataPointRadiusValue.float
    }

    private fun setGraphVisibility(visibility: Int) {
        if (visibility == View.VISIBLE) {
            binding.priceGraph.visibility = View.VISIBLE
            binding.progressBar.visibility = ProgressBar.GONE
            binding.percentDifference.visibility = View.VISIBLE
            binding.baseToQuoteAsk.visibility = View.VISIBLE
            binding.baseToQuoteBid.visibility = View.VISIBLE
        } else {
            binding.priceGraph.visibility = View.INVISIBLE
            binding.progressBar.progress = ProgressBar.VISIBLE
            binding.percentDifference.visibility = View.INVISIBLE
            binding.baseToQuoteAsk.visibility = View.GONE
            binding.baseToQuoteBid.visibility = View.GONE
        }
    }

}
