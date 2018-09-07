package app.carpecoin.priceGraph

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import app.carpecoin.Enums
import app.carpecoin.Enums.Currency.BTC
import app.carpecoin.Enums.Currency.ETH
import app.carpecoin.Enums.Exchange
import app.carpecoin.Enums.Exchange.GDAX
import app.carpecoin.Enums.Timeframe
import app.carpecoin.Enums.Timeframe.DAY
import app.carpecoin.priceGraph.models.PercentDifference
import app.carpecoin.priceGraph.models.PriceGraphData
import app.carpecoin.priceGraph.models.PricePair
import com.jjoe64.graphview.series.LineGraphSeries

class PriceViewModel : ViewModel() {

    //TODO: Set timeframe from UI.
    var timeframe = MutableLiveData<Timeframe>()
    var enabledExchanges = MutableLiveData<ArrayList<Exchange?>>()
    var priceDifferenceDetailsLiveData: LiveData<PercentDifference>

    //TODO: Query Firebase by pricePair.
    var pricePair = PricePair(ETH, BTC)

    var graphSeriesMap = hashMapOf(
            Pair(Enums.Exchange.GDAX, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(Enums.Exchange.BINANCE, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(Enums.Exchange.GEMINI, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(Enums.Exchange.KUCOIN, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(Enums.Exchange.KRAKEN, PriceGraphData(LineGraphSeries(), LineGraphSeries())))

    private var toInitializePriceGraphData = MutableLiveData<Boolean>()

    init {
        val percentPriceDifferenceLiveData = PriceRepository.priceDifferenceDetailsLiveData
        timeframe.value = DAY
        enabledExchanges.value = arrayListOf(GDAX)
        this.priceDifferenceDetailsLiveData =
                Transformations.map(percentPriceDifferenceLiveData) { result -> result }
    }

    fun getPrices(isRealtime: Boolean) {
        this.toInitializePriceGraphData.value = true
        PriceRepository.getPrices(isRealtime, timeframe.value!!)
    }

    val graphLiveData = Transformations.switchMap(toInitializePriceGraphData) {
        PriceRepository.graphLiveData
    }

    val priceGraphXAndYConstraintsLiveData = Transformations.switchMap(toInitializePriceGraphData) {
        PriceRepository.graphConstraintsLiveData
    }

    fun addOrRemoveEnabledExchanges(exchange: Exchange) {
        val currentEnabledExchanges = enabledExchanges.value
        var updatedEnabledExchanges = ArrayList<Exchange?>()
        if (currentEnabledExchanges?.contains(exchange) != true && currentEnabledExchanges != null
                && currentEnabledExchanges.isNotEmpty()) {
            updatedEnabledExchanges = arrayListOf(exchange, currentEnabledExchanges.first())
        } else if (currentEnabledExchanges?.contains(exchange) == false || currentEnabledExchanges == null) {
            updatedEnabledExchanges.add(exchange)
        } else {
            currentEnabledExchanges.remove(exchange)
            updatedEnabledExchanges = currentEnabledExchanges
        }
        this.enabledExchanges.value = updatedEnabledExchanges
    }

}