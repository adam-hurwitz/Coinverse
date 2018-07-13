package app.carpecoin.coin

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.firebase.client.Firebase
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.view.View
import android.widget.ProgressBar
import app.carpecoin.Enums
import app.carpecoin.MainViewModel
import app.carpecoin.coin.databinding.ActivityMainBinding
import app.carpecoin.models.price.PriceGraphLiveData
import app.carpecoin.Enums.Exchange.GDAX
import app.carpecoin.models.price.PriceGraphXAndYConstraints
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

//FIXME: Save OnSaveInstanceState for PriceGraph
class MainActivity : AppCompatActivity() {
    //TODO: Paid feature
    var IS_LIVE_DATA_ENABLED = false

    var bidsSeries: LineGraphSeries<DataPoint>? = LineGraphSeries()
    var asksSeries: LineGraphSeries<DataPoint>? = LineGraphSeries()

    lateinit var binding: ActivityMainBinding
    lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.setAndroidContext(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        binding.viewmodel = viewModel
        setRealtimeDataModeConfigurations()
        if (savedInstanceState == null) {
            initializeGraph()
        }
        observePriceGraphData()

    }

    private fun initializeGraph() {
        setGraphVisibility(View.GONE)
        viewModel.initializeData()
        viewModel.setFirebasePriceGraphListeners(IS_LIVE_DATA_ENABLED)
    }

    private fun setGraphVisibility(visibility: Int) {
        if (visibility == View.VISIBLE) {
            binding.pricingGraph.visibility = View.VISIBLE
            binding.pricingGraph.viewport.isXAxisBoundsManual = true
            binding.progressBar.visibility = ProgressBar.GONE
        } else {
            binding.pricingGraph.visibility = View.INVISIBLE
            binding.progressBar.progress = ProgressBar.VISIBLE
        }
    }

    private fun observePriceGraphData() {
        viewModel.priceGraphData.observe(
                this,
                Observer { priceGraphDataMap: HashMap<Enums.Exchange, PriceGraphLiveData>? ->
                    binding.pricingGraph.removeAllSeries()
                    if (priceGraphDataMap?.get(GDAX) != null) {
                        bidsSeries = priceGraphDataMap.get(GDAX)?.bidsLiveData?.value
                        asksSeries = priceGraphDataMap.get(GDAX)?.asksLiveData?.value
                        bidsSeries?.color = Color.BLUE
                        asksSeries?.color = Color.BLUE
                        binding.pricingGraph.addSeries(bidsSeries)
                        binding.pricingGraph.addSeries(asksSeries)
                    }
                    setGraphVisibility(View.VISIBLE)

                    /*if (priceGraphDataMap?.get(BINANCE) != null) {
                        var bidsSeries = priceGraphDataMap?.get(BINANCE)?.bidsLiveData?.value
                        var asksSeries = priceGraphDataMap?.get(BINANCE)?.asksLiveData?.value
                        bidsSeries?.color = Color.CYAN
                        asksSeries?.color = Color.CYAN
                        binding.pricingGraph.addSeries(bidsSeries)
                        binding.pricingGraph.addSeries(asksSeries)
                    }*/
                    /*if (priceGraphDataMap?.get(KRAKEN) != null) {
                        binding.pricingGraph.addSeries(priceGraphDataMap?.get(KRAKEN)?.bidsLiveData?.value)
                        binding.pricingGraph.addSeries(priceGraphDataMap?.get(KRAKEN)?.asksLiveData?.value)
                    }*/
                    /*if (priceGraphDataMap?.get(KUCOIN) != null) {
                        //binding.pricingGraph.addSeries(priceGraphDataMap?.get(KUCOIN)?.bidsLiveData?.value)
                        //binding.pricingGraph.addSeries(priceGraphDataMap?.get(KUCOIN)?.asksLiveData?.value)
                    }*/
                    /*if (priceGraphDataMap?.get(GEMINI) != null) {
                        //binding.pricingGraph.addSeries(priceGraphDataMap?.get(GEMINI)?.bidsLiveData?.value)
                        //binding.pricingGraph.addSeries(priceGraphDataMap?.get(GEMINI)?.asksLiveData?.value)
                    }*/
                })
        viewModel.priceGraphXAndYConstraints.observe(
                this,
                Observer { priceGraphXAndYConstraints: PriceGraphXAndYConstraints? ->
                    binding.pricingGraph.viewport.setMinY(priceGraphXAndYConstraints?.minY
                            ?: 0.0)
                    binding.pricingGraph.viewport.setMaxY(priceGraphXAndYConstraints?.maxY
                            ?: 0.0)
                    binding.pricingGraph.viewport.setMinX(priceGraphXAndYConstraints?.minX
                            ?: 0.0)
                    binding.pricingGraph.viewport.setMaxX(priceGraphXAndYConstraints?.maxX
                            ?: 0.0)
                    binding.pricingGraph.onDataChanged(true, false)
                })
        viewModel.isPriceGraphDataLoaded.observe(
                this,
                Observer { isPriceGraphDataLoaded: Boolean? ->
                    if (isPriceGraphDataLoaded ?: true) {
                        binding.swipeToRefresh.isRefreshing = false
                    }
                }
        )
    }

    private fun setRealtimeDataModeConfigurations() {
        if (IS_LIVE_DATA_ENABLED) {
            binding.swipeToRefresh.isRefreshing = false
            binding.swipeToRefresh.isEnabled = false
        } else {
            binding.swipeToRefresh.setOnRefreshListener({
                viewModel.initializeData()
                viewModel.setFirebasePriceGraphListeners(IS_LIVE_DATA_ENABLED)
            })
        }
    }

}