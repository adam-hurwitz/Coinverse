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
import app.carpecoin.HomeViewModel
import app.carpecoin.coin.databinding.FragmentPriceGraphBinding
import kotlinx.android.synthetic.main.fragment_price_graph.*

private val dataPointRadiusValue = TypedValue()

class PriceGraphFragment : Fragment() {
    private lateinit var binding: FragmentPriceGraphBinding
    private lateinit var priceViewModel: PriceDataViewModel
    private lateinit var homeViewModel: HomeViewModel

    private var enabledExchangesList: ArrayList<Enums.Exchange?>? = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        priceViewModel = ViewModelProviders.of(this).get(PriceDataViewModel::class.java)
        homeViewModel = ViewModelProviders.of(activity!!).get(HomeViewModel::class.java)
        if (savedInstanceState == null) {
            initializeData()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentPriceGraphBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(this)
        binding.viewmodel = priceViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPriceGraphStyle()
        setRealtimeDataConfiguration()
        setGraphVisibility(View.GONE)

        observeExchangesEnabled()
        observeGraphData()
        observeGraphConstraints()
        observePriceDifferenceDetails()
    }

    companion object {
        @JvmStatic
        fun newInstance() = PriceGraphFragment()
    }

    fun initializeData() {
        priceViewModel.initializeData(priceViewModel.isRealtimeDataEnabled)
    }

    private fun setPriceGraphStyle() {
        priceGraph.viewport.isXAxisBoundsManual = true
        val graphLabels = priceGraph.gridLabelRenderer
        graphLabels.gridStyle = GridLabelRenderer.GridStyle.NONE
        graphLabels.isHorizontalLabelsVisible = false
        graphLabels.isVerticalLabelsVisible = false
        resources.getValue(R.dimen.data_point_radius, dataPointRadiusValue, true)
        priceViewModel.timeframe.observe(viewLifecycleOwner, Observer { timeframeToQuery: Timeframe? ->
            when (timeframeToQuery) {
                DAY -> timeframe.text = resources.getString(R.string.timeframe_last_day)
                else -> timeframe.text = resources.getString(R.string.timeframe_last_day)
            }
        })
    }

    private fun setRealtimeDataConfiguration() {
        priceViewModel.isRealtimeDataEnabled.let {
            homeViewModel.setRefreshStatus(it)
            if (it) {
                priceGraph.viewport.isScrollable = true
            } else {
                priceGraph.viewport.isScalable = true
            }
        }
    }

    private fun observeExchangesEnabled() {
        priceViewModel.enabledExchanges.observe(viewLifecycleOwner, Observer { enabledExchangeList: ArrayList<Exchange?>? ->

            enabledExchangesList = enabledExchangeList
            priceGraph.removeAllSeries()

            setPriceGraphToggleColor(enabledExchangeList?.contains(GDAX) ?: false,
                    coinbaseToggle)
            setPriceGraphToggleColor(enabledExchangeList?.contains(BINANCE) ?: false,
                    binanceToggle)
            setPriceGraphToggleColor(enabledExchangeList?.contains(GEMINI) ?: false,
                    geminiToggle)
            setPriceGraphToggleColor(enabledExchangeList?.contains(KUCOIN) ?: false,
                    kucoinToggle)
            setPriceGraphToggleColor(enabledExchangeList?.contains(KRAKEN) ?: false,
                    krakenToggle)

            for (exchange in enabledExchangeList!!) {
                val graphSeriesMap = priceViewModel.graphSeriesMap[exchange]
                val asks = graphSeriesMap?.asks
                val bids = graphSeriesMap?.bids
                if (enabledExchangeList.contains(exchange)) {
                    priceGraph.addSeries(asks)
                    priceGraph.addSeries(bids)
                } else {
                    priceGraph.removeSeries(asks)
                    priceGraph.removeSeries(bids)
                }
            }
            priceGraph.refreshDrawableState()
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
        priceViewModel.graphLiveData.observe(
                viewLifecycleOwner, Observer { priceGraphDataMap: HashMap<Exchange, PriceGraphData>? ->
            for (priceGraphData in priceGraphDataMap!!.entries) {
                val exchange = priceGraphData.key
                val graphSeriesMap = priceViewModel.graphSeriesMap[exchange]
                setExchangeGraphDataAndStyle(exchange, ASK, graphSeriesMap?.asks, priceGraphDataMap)
                setExchangeGraphDataAndStyle(exchange, BID, graphSeriesMap?.bids, priceGraphDataMap)
            }
        })
    }

    private fun observeGraphConstraints() {
        priceViewModel.priceGraphXAndYConstraintsLiveData.observe(
                viewLifecycleOwner,
                Observer { priceGraphXAndYConstraints: PriceGraphXAndYConstraints? ->
                    priceGraph.viewport.setMinY(priceGraphXAndYConstraints?.minY
                            ?: 0.0)
                    priceGraph.viewport.setMaxY(priceGraphXAndYConstraints?.maxY
                            ?: 0.0)
                    priceGraph.viewport.setMinX(priceGraphXAndYConstraints?.minX
                            ?: 0.0)
                    priceGraph.viewport.setMaxX(priceGraphXAndYConstraints?.maxX
                            ?: 0.0)
                    priceGraph.onDataChanged(true, false)
                })
    }

    private fun observePriceDifferenceDetails() {
        priceViewModel.priceDifferenceDetailsLiveData.observe(viewLifecycleOwner, Observer { percentDifference: PercentDifference? ->
            updatePriceDifferenceIndicators(ConstraintSet(), percentDifference?.askExchange,
                    baseToQuoteAsk.id)
            updatePriceDifferenceIndicators(ConstraintSet(), percentDifference?.bidExchange,
                    baseToQuoteBid.id)
        })
    }

    private fun updatePriceDifferenceIndicators(constraintSet: ConstraintSet,
                                                exchange: Exchange?,
                                                orderLayoutId: Int) {
        constraintSet.clone(card_price_constraint)
        var textViewId: Int
        val margin = resources.getInteger(R.integer.price_graph_base_to_quote_margin)
        when (exchange) {
            GDAX -> textViewId = coinbaseToggle.id
            BINANCE -> textViewId = binanceToggle.id
            GEMINI -> textViewId = geminiToggle.id
            KUCOIN -> textViewId = kucoinToggle.id
            KRAKEN -> textViewId = krakenToggle.id
            else -> textViewId = 0
        }
        constraintSet.connect(orderLayoutId, ConstraintSet.TOP, textViewId,
                ConstraintSet.BOTTOM, margin)
        constraintSet.connect(orderLayoutId, ConstraintSet.LEFT, textViewId,
                ConstraintSet.LEFT, margin)
        constraintSet.connect(orderLayoutId, ConstraintSet.RIGHT, textViewId,
                ConstraintSet.RIGHT, margin)
        constraintSet.applyTo(card_price_constraint)
    }

    private fun setExchangeGraphDataAndStyle(exchange: Exchange, orderType: OrderType,
                                             orders: LineGraphSeries<DataPoint>?,
                                             priceGraphDataMap: HashMap<Exchange, PriceGraphData>?) {
        //TODO:Examine logic here for double graph data bug
        if (priceGraphDataMap != null) {
            priceGraph.removeSeries(orders)
            val orders: LineGraphSeries<DataPoint>?
            val color: Int
            val thickness: Int
            val graphSeriesMap = priceViewModel.graphSeriesMap[exchange]
            if (orderType == ASK) {
                graphSeriesMap?.asks = priceGraphDataMap[exchange]?.asks
                orders = graphSeriesMap?.asks
                color = ExchangeColors.get(exchange, ASK)
                thickness = resources.getInteger(R.integer.price_graph_asks_thickness)
            } else {
                graphSeriesMap?.bids = priceGraphDataMap[exchange]?.bids
                orders = graphSeriesMap?.bids
                color = ExchangeColors.get(exchange, BID)
                thickness = resources.getInteger(R.integer.price_graph_bids_thickness)
            }

            //TODO: Make swipe-to-refresh smoother.
            setGraphVisibility(View.VISIBLE)
            homeViewModel.disableSwipeToRefresh()

            setOrderPriceGraphStyle(orders, color, thickness)
            if (enabledExchangesList?.contains(exchange) == true) {
                priceGraph.addSeries(orders)
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
            priceGraph.visibility = View.VISIBLE
            progressBar.visibility = ProgressBar.GONE
            percentDifference.visibility = View.VISIBLE
            baseToQuoteAsk.visibility = View.VISIBLE
            baseToQuoteBid.visibility = View.VISIBLE
        } else {
            priceGraph.visibility = View.INVISIBLE
            progressBar.progress = ProgressBar.VISIBLE
            percentDifference.visibility = View.INVISIBLE
            baseToQuoteAsk.visibility = View.GONE
            baseToQuoteBid.visibility = View.GONE
        }
    }

}
