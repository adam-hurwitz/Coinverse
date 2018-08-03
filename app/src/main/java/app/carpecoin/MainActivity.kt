package app.carpecoin

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.databinding.DataBindingUtil
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import app.carpecoin.coin.databinding.ActivityMainBinding
import app.carpecoin.models.price.PriceGraphLiveData
import app.carpecoin.Enums.Exchange.GDAX
import app.carpecoin.models.price.PriceGraphXAndYConstraints
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.jjoe64.graphview.GridLabelRenderer
import app.carpecoin.Enums.Exchange
import app.carpecoin.Enums.Exchange.BINANCE
import app.carpecoin.Enums.Exchange.GEMINI
import app.carpecoin.Enums.Exchange.KUCOIN
import app.carpecoin.Enums.Exchange.KRAKEN
import app.carpecoin.Enums.OrderType
import app.carpecoin.Enums.OrderType.ASK
import app.carpecoin.Enums.OrderType.BID
import app.carpecoin.Enums.Timeframe
import app.carpecoin.Enums.Timeframe.DAY
import android.util.TypedValue
import app.carpecoin.models.price.PercentDifference
import app.carpecoin.models.price.PriceGraphData
import app.carpecoin.utils.ExchangeColors
import android.support.constraint.ConstraintSet
import app.carpecoin.coin.R
import app.carpecoin.utils.FirebaseHelper
import kotlinx.android.synthetic.main.activity_main.view.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    private val dataPointRadiusValue = TypedValue()

    private var enabledExchangesList: ArrayList<Exchange?>? = ArrayList()
    private var exchangeGraphSeriesMap = hashMapOf(
            Pair(GDAX, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(BINANCE, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(GEMINI, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(KUCOIN, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(KRAKEN, PriceGraphData(LineGraphSeries(), LineGraphSeries())))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseHelper.initialize(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.setLifecycleOwner(this)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        binding.viewmodel = viewModel
        setRealtimeDataModeConfigurations()
        observePriceGraphData()
        if (savedInstanceState == null) {
            setPriceGraphVisibility(View.GONE)
            viewModel.initializeData(viewModel.isRealtimeDataEnabled)
        }
    }

    private fun setRealtimeDataModeConfigurations() {
        if (viewModel.isRealtimeDataEnabled) {
            binding.swipeToRefresh.isRefreshing = false
            binding.swipeToRefresh.isEnabled = false
            binding.priceGraph.viewport.isScrollable = true // Enables horizontal scrolling.
        } else {
            binding.priceGraph.viewport.isScalable = true // Enables horizontal zooming and scrolling.
            binding.swipeToRefresh.setOnRefreshListener {
                viewModel.initializeData(viewModel.isRealtimeDataEnabled)
            }
        }
    }

    private fun observePriceGraphData() {
        observeExchangesEnabled()
        setPriceGraphStyle()

        viewModel.priceGraphLiveData.observe(
                this, Observer { priceGraphDataMap: HashMap<Enums.Exchange, PriceGraphLiveData>? ->

            for (priceGraphData in priceGraphDataMap!!.entries) {
                println("VALUE: " + priceGraphData.key)
                val exchange = priceGraphData.key
                setExchangeGraphDataAndStyle(exchange, ASK, exchangeGraphSeriesMap[exchange]?.asks,
                        priceGraphDataMap)
                setExchangeGraphDataAndStyle(exchange, BID, exchangeGraphSeriesMap[exchange]?.bids,
                        priceGraphDataMap)
            }

        })

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

        viewModel.isPriceGraphDataLoadedLiveData.observe(
                this, Observer { isPriceGraphDataLoaded: Boolean? ->
            if (isPriceGraphDataLoaded ?: true) {
                //TODO: Make swipe-to-refresh smoother.
                setPriceGraphVisibility(View.VISIBLE)
                binding.swipeToRefresh.isRefreshing = false
            }
        })

        viewModel.percentDifferenceLiveData.observe(this, Observer { percentDifference: PercentDifference? ->
            println(String.format("ASK_EXCHANGE:%s BID_EXCHANGE:%s", percentDifference?.askExchange,
                    percentDifference?.bidExchange))

            val constraintSet = ConstraintSet()
            constraintSet.clone(binding.activityMainConstraint.card_price_constraint)
            var textViewId: Int
            val margin = resources.getInteger(R.integer.price_graph_base_to_quote_margin)
            when (percentDifference?.askExchange) {
                GDAX -> textViewId = binding.coinbaseToggle.id
                BINANCE -> textViewId = binding.binanceToggle.id
                GEMINI -> textViewId = binding.geminiToggle.id
                KUCOIN -> textViewId = binding.kucoinToggle.id
                KRAKEN -> textViewId = binding.krakenToggle.id
                else -> textViewId = 0
            }
            constraintSet.connect(binding.baseToQuoteAsk.id, ConstraintSet.TOP, textViewId,
                    ConstraintSet.BOTTOM, margin)
            constraintSet.connect(binding.baseToQuoteAsk.id, ConstraintSet.LEFT, textViewId,
                    ConstraintSet.LEFT, margin)
            constraintSet.connect(binding.baseToQuoteAsk.id, ConstraintSet.RIGHT, textViewId,
                    ConstraintSet.RIGHT, margin)
            when (percentDifference?.bidExchange) {
                GDAX -> textViewId = binding.coinbaseToggle.id
                BINANCE -> textViewId = binding.binanceToggle.id
                GEMINI -> textViewId = binding.geminiToggle.id
                KUCOIN -> textViewId = binding.kucoinToggle.id
                KRAKEN -> textViewId = binding.krakenToggle.id
                else -> textViewId = 0
            }

            constraintSet.connect(binding.baseToQuoteBid.id, ConstraintSet.TOP, textViewId,
                    ConstraintSet.BOTTOM, margin)
            constraintSet.connect(binding.baseToQuoteBid.id, ConstraintSet.LEFT, textViewId,
                    ConstraintSet.LEFT, margin)
            constraintSet.connect(binding.baseToQuoteBid.id, ConstraintSet.RIGHT, textViewId,
                    ConstraintSet.RIGHT, margin)
            constraintSet.applyTo(binding.activityMainConstraint.card_price_constraint)
        })
    }

    private fun observeExchangesEnabled() {
        viewModel.enabledExchanges.observe(this, Observer { enabledExchangeList: ArrayList<Exchange?>? ->

            this.enabledExchangesList = enabledExchangeList
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
                val asks = exchangeGraphSeriesMap[exchange]?.asks
                val bids = exchangeGraphSeriesMap[exchange]?.bids
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
            toggleView.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
        } else {
            toggleView.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
        }
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

    private fun setExchangeGraphDataAndStyle(exchange: Exchange, orderType: OrderType,
                                             orders: LineGraphSeries<DataPoint>?,
                                             priceGraphDataMap: HashMap<Exchange, PriceGraphLiveData>?) {
        binding.priceGraph.removeSeries(orders)
        if (priceGraphDataMap != null) {
            val orders: LineGraphSeries<DataPoint>?
            val color: Int
            val thickness: Int
            if (orderType == ASK) {
                exchangeGraphSeriesMap[exchange]?.asks = priceGraphDataMap[exchange]?.asksLiveData?.value
                orders = exchangeGraphSeriesMap[exchange]?.asks
                color = ExchangeColors.get(exchange, ASK)
                thickness = resources.getInteger(R.integer.price_graph_asks_thickness)
            } else {
                exchangeGraphSeriesMap[exchange]?.bids = priceGraphDataMap[exchange]?.bidsLiveData?.value
                orders = exchangeGraphSeriesMap[exchange]?.bids
                color = ExchangeColors.get(exchange, BID)
                thickness = resources.getInteger(R.integer.price_graph_bids_thickness)
            }
            setOrderPriceGraphStyle(orders, color, thickness)
            if (enabledExchangesList?.contains(exchange) == true) {
                binding.priceGraph.addSeries(orders)
            }
        }
    }

    private fun setOrderPriceGraphStyle(orders: LineGraphSeries<DataPoint>?, color: Int,
                                        thickness: Int) {
        orders?.color = ContextCompat.getColor(this, color)
        orders?.thickness = thickness
        orders?.isDrawDataPoints = false
        orders?.dataPointsRadius = dataPointRadiusValue.float
    }

    private fun setPriceGraphVisibility(visibility: Int) {
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