package app.coinverse.priceGraph

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.Transformations.switchMap
import androidx.lifecycle.ViewModel
import app.coinverse.Enums.Currency.BTC
import app.coinverse.Enums.Currency.ETH
import app.coinverse.Enums.Exchange
import app.coinverse.Enums.Exchange.*
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

    //TODO: Query Firebase by pricePair.
    val pricePair = PricePair(ETH, BTC)
    val timeframe: LiveData<Timeframe> get() = _timeframe
    val enabledExchanges: LiveData<ArrayList<Exchange?>> get() = _enabledExchanges
    val enabledOrderTypes: LiveData<ArrayList<OrderType?>> get() = _enabledOrderTypes
    val graphSeriesMap = hashMapOf(
            Pair(COINBASE, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(BINANCE, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(GEMINI, PriceGraphData(LineGraphSeries(), LineGraphSeries())),
            Pair(KRAKEN, PriceGraphData(LineGraphSeries(), LineGraphSeries())))
    val priceSelected: LiveData<Pair<Exchange, String>> get() = _priceSelected
    val priceDifferenceLiveData: LiveData<PercentDifference>

    private val toInitializeGraphData = MutableLiveData<Boolean>()
    private val _timeframe = MutableLiveData<Timeframe>()
    private val _enabledExchanges = MutableLiveData<ArrayList<Exchange?>>()
    private val _enabledOrderTypes = MutableLiveData<ArrayList<OrderType?>>()
    private val _priceSelected = MutableLiveData<Pair<Exchange, String>>()

    init {
        _timeframe.value = DAY
        _enabledOrderTypes.value = arrayListOf(BID)
        _enabledExchanges.value = arrayListOf(COINBASE)
        val _priceDifferenceLiveData = PriceRepository.priceDifferenceDetailsLiveData
        this.priceDifferenceLiveData = map(_priceDifferenceLiveData) { result -> result }
    }

    val graphLiveData = switchMap(toInitializeGraphData) {
        PriceRepository.graphLiveData
    }

    val priceGraphXAndYConstraintsLiveData = switchMap(toInitializeGraphData) {
        PriceRepository.graphConstraintsLiveData
    }

    fun getPrices(isRealtime: Boolean, isOnCreateCall: Boolean) {
        this.toInitializeGraphData.value = true
        PriceRepository.getPrices(isRealtime, isOnCreateCall, timeframe.value!!)
    }

    fun setPriceSelected(priceSelected: Pair<Exchange, String>) {
        _priceSelected.value = priceSelected
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
            _priceSelected.value = Pair(EMPTY, "")
        _enabledExchanges.value = updatedEnabledExchanges
    }

    fun orderToggle(orderType: OrderType) {
        val enabledOrderTypes = enabledOrderTypes.value
        if (!enabledOrderTypes!!.contains(orderType)) enabledOrderTypes.add(orderType)
        else {
            enabledOrderTypes.remove(orderType)
            _priceSelected.value = Pair(EMPTY, "")
        }
        _enabledOrderTypes.value = enabledOrderTypes
    }
}