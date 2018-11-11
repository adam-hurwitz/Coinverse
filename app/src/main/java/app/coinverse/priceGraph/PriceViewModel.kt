package app.coinverse.priceGraph

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import app.coinverse.Enums
import app.coinverse.Enums.Currency.BTC
import app.coinverse.Enums.Currency.ETH
import app.coinverse.Enums.Exchange
import app.coinverse.Enums.Exchange.COINBASE
import app.coinverse.Enums.Exchange.EMPTY
import app.coinverse.Enums.OrderType
import app.coinverse.Enums.OrderType.BID
import app.coinverse.Enums.Timeframe
import app.coinverse.Enums.Timeframe.DAY
import app.coinverse.priceGraph.models.PercentDifference
import app.coinverse.priceGraph.models.PriceGraphData
import app.coinverse.priceGraph.models.PricePair
import com.jjoe64.graphview.series.LineGraphSeries

class PriceViewModel : ViewModel() {

    //TODO: Set timeframe from UI.
    var timeframe = MutableLiveData<Timeframe>()
    var priceSelected = MutableLiveData<Pair<Exchange, String>>()
    var enabledOrderTypes = MutableLiveData<ArrayList<OrderType?>>()
    var enabledExchanges = MutableLiveData<ArrayList<Exchange?>>()
    var priceDifferenceLiveData: LiveData<PercentDifference>

    //TODO: Query Firebase by pricePair.
    var pricePair = PricePair(ETH, BTC)

    var graphSeriesMap = hashMapOf(
            Pair(Enums.Exchange.COINBASE, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(Enums.Exchange.BINANCE, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(Enums.Exchange.GEMINI, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(Enums.Exchange.KUCOIN, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(Enums.Exchange.KRAKEN, PriceGraphData(LineGraphSeries(), LineGraphSeries())))

    private var toInitializePriceGraphData = MutableLiveData<Boolean>()

    init {
        val priceDifferenceLiveData = PriceRepository.priceDifferenceDetailsLiveData
        timeframe.value = DAY
        enabledOrderTypes.value = arrayListOf(BID)
        enabledExchanges.value = arrayListOf(COINBASE)
        this.priceDifferenceLiveData = Transformations.map(priceDifferenceLiveData) { result -> result }
    }

    fun getPrices(isRealtime: Boolean, isOnCreateCall: Boolean) {
        this.toInitializePriceGraphData.value = true
        PriceRepository.getPrices(isRealtime, isOnCreateCall, timeframe.value!!)
    }

    val graphLiveData = Transformations.switchMap(toInitializePriceGraphData) {
        PriceRepository.graphLiveData
    }

    val priceGraphXAndYConstraintsLiveData = Transformations.switchMap(toInitializePriceGraphData) {
        PriceRepository.graphConstraintsLiveData
    }

    fun exchangeToggle(exchange: Exchange) {
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
        if (!updatedEnabledExchanges.contains(priceSelected.value?.first))
            priceSelected.value = Pair(EMPTY, "")
        this.enabledExchanges.value = updatedEnabledExchanges
    }

    fun orderToggle(orderType: OrderType) {
        val enabledOrderTypes = enabledOrderTypes.value
        if (!enabledOrderTypes!!.contains(orderType)) enabledOrderTypes.add(orderType)
        else {
            enabledOrderTypes.remove(orderType)
            priceSelected.value = Pair(EMPTY, "")
        }
        this.enabledOrderTypes.value = enabledOrderTypes
    }
}