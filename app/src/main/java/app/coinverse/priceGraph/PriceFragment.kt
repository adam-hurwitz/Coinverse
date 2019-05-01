package app.coinverse.priceGraph

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import app.coinverse.R.color.colorAccent
import app.coinverse.R.color.colorPrimaryDark
import app.coinverse.R.dimen.data_point_radius
import app.coinverse.R.integer
import app.coinverse.R.string.*
import app.coinverse.databinding.FragmentPriceBinding
import app.coinverse.home.HomeViewModel
import app.coinverse.priceGraph.models.PriceGraphData
import app.coinverse.priceGraph.models.PriceGraphXAndYConstraints
import app.coinverse.utils.Enums.Exchange
import app.coinverse.utils.Enums.Exchange.*
import app.coinverse.utils.Enums.OrderType
import app.coinverse.utils.Enums.OrderType.ASK
import app.coinverse.utils.Enums.OrderType.BID
import app.coinverse.utils.Enums.Timeframe
import app.coinverse.utils.Enums.Timeframe.DAY
import app.coinverse.utils.getExchangeColor
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.fragment_price.*


private val dataPointRadiusValue = TypedValue()

class PriceFragment : Fragment() {
    private var LOG_TAG = PriceFragment::class.java.simpleName
    private lateinit var binding: FragmentPriceBinding
    private lateinit var priceViewModel: PriceViewModel
    private lateinit var homeViewModel: HomeViewModel

    private var enabledOrderTypeList: ArrayList<OrderType?>? = ArrayList()
    private var enabledExchangeList: ArrayList<Exchange?>? = ArrayList()

    companion object {
        @JvmStatic
        fun newInstance() = PriceFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        priceViewModel = ViewModelProviders.of(this).get(PriceViewModel::class.java)
        homeViewModel = ViewModelProviders.of(activity!!).get(HomeViewModel::class.java)
        if (savedInstanceState == null) {
            homeViewModel.isRealtime.observe(this, Observer { isRealtime: Boolean ->
                getPrices(isRealtime, true)
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentPriceBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewmodel = priceViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPriceGraphStyle()
        setGraphVisibility(GONE)
        observeOrderTypesEnabled()
        observeExchangesEnabled()
        observePriceSelected()
        observeGraphData()
        observeGraphConstraints()
        observePriceDifferenceDetails()
    }

    fun getPrices(isRealtime: Boolean, isOnCreateCall: Boolean) {
        priceViewModel.getPrices(isRealtime, isOnCreateCall)
    }

    private fun setPriceGraphStyle() {
        graph.viewport.isXAxisBoundsManual = true
        val graphLabels = graph.gridLabelRenderer
        graphLabels.gridStyle = GridLabelRenderer.GridStyle.NONE
        graphLabels.isHorizontalLabelsVisible = false
        graphLabels.isVerticalLabelsVisible = false
        resources.getValue(data_point_radius, dataPointRadiusValue, true)
        priceViewModel.timeframe.observe(viewLifecycleOwner, Observer { timeframeToQuery: Timeframe? ->
            when (timeframeToQuery) {
                DAY -> timeframe.text = resources.getString(timeframe_last_day)
                else -> timeframe.text = resources.getString(timeframe_last_day)
            }
        })
    }

    private fun observeOrderTypesEnabled() {
        priceViewModel.enabledOrderTypes.observe(viewLifecycleOwner, Observer { enabledOrderTypeList ->
            if (enabledExchangeList!!.isNotEmpty()) {
                graph.removeAllSeries()
                for (exchange in enabledExchangeList!!) {
                    val graphSeriesMap = priceViewModel.graphSeriesMap[exchange]
                    if (enabledOrderTypeList.contains(BID)) graph.addSeries(graphSeriesMap?.bids)
                    else graph.removeSeries(graphSeriesMap?.bids)
                    if (enabledOrderTypeList.contains(ASK)) graph.addSeries(graphSeriesMap?.asks)
                    else graph.removeSeries(graphSeriesMap?.asks)
                }
            }
            setExchangeToggleColor(enabledOrderTypeList.contains(BID), bidsToggle)
            setExchangeToggleColor(enabledOrderTypeList.contains(ASK), asksToggle)
            this.enabledOrderTypeList = enabledOrderTypeList
        })
    }

    private fun observeExchangesEnabled() {
        priceViewModel.enabledExchanges.observe(viewLifecycleOwner, Observer { enabledExchangeList ->
            this.enabledExchangeList = enabledExchangeList
            graph.removeAllSeries()
            setExchangeToggleColor(COINBASE, enabledExchangeList, coinbaseToggle)
            setExchangeToggleColor(BINANCE, enabledExchangeList, binanceToggle)
            setExchangeToggleColor(GEMINI, enabledExchangeList, geminiToggle)
            setExchangeToggleColor(KRAKEN, enabledExchangeList, krakenToggle)

            for (exchange in enabledExchangeList!!) {
                val graphSeriesMap = priceViewModel.graphSeriesMap[exchange]
                val asks = graphSeriesMap?.asks
                val bids = graphSeriesMap?.bids
                if (enabledExchangeList.contains(exchange)) {
                    if (enabledOrderTypeList!!.contains(ASK)) graph.addSeries(asks)
                    if (enabledOrderTypeList!!.contains(BID)) graph.addSeries(bids)
                }
            }
        })
    }

    private fun observePriceSelected() {
        priceViewModel.priceSelected.observe(viewLifecycleOwner, Observer { selected ->
            priceSelected.setTextColor(context!!.getColor(getExchangeColor(selected.first)))
            priceSelected.text = selected.second
        })
    }

    private fun setExchangeToggleColor(exchange: Exchange, enabledExchangeList: ArrayList<Exchange?>?,
                                       toggleView: TextView) {
        if (enabledExchangeList!!.contains(exchange))
            toggleView.setTextColor(ContextCompat.getColor(context!!, getExchangeColor(exchange)))
        else toggleView.setTextColor(ContextCompat.getColor(context!!, colorPrimaryDark))
    }

    private fun setExchangeToggleColor(isToggled: Boolean?, toggleView: TextView) {
        if (isToggled == true) toggleView.setTextColor(ContextCompat.getColor(context!!, colorAccent))
        else toggleView.setTextColor(ContextCompat.getColor(context!!, colorPrimaryDark))
    }

    private fun observeGraphData() {
        priceViewModel.graphLiveData.observe(viewLifecycleOwner, Observer { priceGraphDataMap ->
            for (priceGraphData in priceGraphDataMap!!.entries) {
                val exchange = priceGraphData.key
                val graphSeriesMap = priceViewModel.graphSeriesMap[exchange]
                setGraph(exchange, ASK, graphSeriesMap?.asks, priceGraphDataMap)
                setGraph(exchange, BID, graphSeriesMap?.bids, priceGraphDataMap)
            }
        })
    }

    private fun observeGraphConstraints() {
        priceViewModel.priceGraphXAndYConstraintsLiveData.observe(
                viewLifecycleOwner,
                Observer { priceGraphXAndYConstraints: PriceGraphXAndYConstraints? ->
                    graph.viewport.setMinY(priceGraphXAndYConstraints?.minY ?: 0.0)
                    graph.viewport.setMaxY(priceGraphXAndYConstraints?.maxY ?: 0.0)
                    graph.viewport.setMinX(priceGraphXAndYConstraints?.minX ?: 0.0)
                    graph.viewport.setMaxX(priceGraphXAndYConstraints?.maxX ?: 0.0)
                    graph.onDataChanged(true, false)
                })
    }

    private fun observePriceDifferenceDetails() {
        priceViewModel.priceDifferenceLiveData.observe(viewLifecycleOwner, Observer { minAndMaxPriceData ->
            if (SDK_INT >= O) {
                maxBid.tooltipText = String.format(getString(max_min_format),
                        minAndMaxPriceData?.bidExchange, minAndMaxPriceData?.baseToQuoteBid?.toFloat())
                minAsk.tooltipText = String.format(getString(max_min_format),
                        minAndMaxPriceData?.askExchange, minAndMaxPriceData?.baseToQuoteAsk?.toFloat())
            } else {
                maxBid.setOnClickListener {
                    makeText(context, String.format(getString(max_min_format),
                            minAndMaxPriceData?.bidExchange, minAndMaxPriceData?.baseToQuoteBid?.toFloat()),
                            LENGTH_SHORT).show()
                }
                minAsk.setOnClickListener {
                    makeText(context, String.format(getString(max_min_format),
                            minAndMaxPriceData?.askExchange, minAndMaxPriceData?.baseToQuoteAsk?.toFloat()),
                            LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setGraph(exchange: Exchange, orderType: OrderType, orders: LineGraphSeries<DataPoint>?,
                         priceGraphDataMap: HashMap<Exchange, PriceGraphData>?) {
        if (priceGraphDataMap != null) {
            graph.removeSeries(orders)
            val orders: LineGraphSeries<DataPoint>?
            val color: Int
            val thickness: Int
            val graphSeriesMap = priceViewModel.graphSeriesMap[exchange]
            val priceLabel: String
            if (orderType == BID) {
                graphSeriesMap?.bids = priceGraphDataMap[exchange]?.bids
                orders = graphSeriesMap?.bids
                color = getExchangeColor(exchange, BID)
                thickness = resources.getInteger(integer.price_graph_bids_thickness)
                priceLabel = getString(bid)
            } else {
                graphSeriesMap?.asks = priceGraphDataMap[exchange]?.asks
                orders = graphSeriesMap?.asks
                color = getExchangeColor(exchange, ASK)
                thickness = resources.getInteger(integer.price_graph_asks_thickness)
                priceLabel = getString(ask)
            }
            setOrderPriceGraphStyle(orders, color, thickness)
            if (enabledExchangeList?.contains(exchange) == true &&
                    enabledOrderTypeList!!.contains(orderType)) graph.addSeries(orders)
            //TODO: Check HomeViewModel for progress status.
            setGraphVisibility(VISIBLE)
            homeViewModel.setSwipeToRefreshState(false)
            orders?.setOnDataPointTapListener { series, dataPoint ->
                priceViewModel.setPriceSelected(
                        Pair(exchange, String.format("%.5f %s", dataPoint.y.toFloat(), priceLabel)))
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
        if (visibility == VISIBLE) {
            graph.visibility = VISIBLE
            progressBar.visibility = ProgressBar.GONE
        } else {
            graph.visibility = INVISIBLE
            progressBar.progress = ProgressBar.VISIBLE
        }
    }
}