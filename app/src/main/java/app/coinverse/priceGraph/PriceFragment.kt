package app.coinverse.priceGraph

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import app.coinverse.Enums
import app.coinverse.Enums.Exchange
import app.coinverse.Enums.Exchange.*
import app.coinverse.Enums.OrderType
import app.coinverse.Enums.OrderType.ASK
import app.coinverse.Enums.OrderType.BID
import app.coinverse.Enums.Timeframe
import app.coinverse.Enums.Timeframe.DAY
import app.coinverse.HomeViewModel
import app.coinverse.coin.R
import app.coinverse.coin.databinding.FragmentPriceBinding
import app.coinverse.priceGraph.models.PercentDifference
import app.coinverse.priceGraph.models.PriceGraphData
import app.coinverse.priceGraph.models.PriceGraphXAndYConstraints
import app.coinverse.utils.ExchangeColors
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

    private var enabledExchangesList: ArrayList<Enums.Exchange?>? = ArrayList()

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
        binding.setLifecycleOwner(this)
        binding.viewmodel = priceViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPriceGraphStyle()
        setGraphVisibility(View.GONE)

        observeExchangesEnabled()
        observeGraphData()
        observeGraphConstraints()
        observePriceDifferenceDetails()
    }

    companion object {
        @JvmStatic
        fun newInstance() = PriceFragment()
    }

    fun getPrices(isRealtime: Boolean, isOnCreateCall: Boolean) {
        priceViewModel.getPrices(isRealtime, isOnCreateCall)
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
            //TODO: Make prices adjacent or below / above and smaller if min/max exchange is the same.
            //TODO: Pass in 1) isSameExchange: Boolean 2) otherPriceViewId
            updatePriceDifferenceIndicators(ConstraintSet(), percentDifference?.askExchange,
                    baseToQuoteAsk.id)
            updatePriceDifferenceIndicators(ConstraintSet(), percentDifference?.bidExchange,
                    baseToQuoteBid.id)
        })
    }

    private fun updatePriceDifferenceIndicators(constraintSet: ConstraintSet, exchange: Exchange?,
                                                orderLayoutId: Int) {
        constraintSet.clone(card_price_constraint)
        val textViewId: Int
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