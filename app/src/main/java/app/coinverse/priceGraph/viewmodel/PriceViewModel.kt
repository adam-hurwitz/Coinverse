package app.coinverse.priceGraph.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.Transformations.switchMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.coinverse.priceGraph.PriceRepository
import app.coinverse.priceGraph.models.PercentDifference
import app.coinverse.priceGraph.models.PriceGraphData
import app.coinverse.priceGraph.models.PricePair
import app.coinverse.utils.Currency
import app.coinverse.utils.Exchange
import app.coinverse.utils.Exchange.BINANCE
import app.coinverse.utils.Exchange.COINBASE
import app.coinverse.utils.Exchange.EMPTY
import app.coinverse.utils.Exchange.GEMINI
import app.coinverse.utils.Exchange.KRAKEN
import app.coinverse.utils.OrderType
import app.coinverse.utils.OrderType.BID
import app.coinverse.utils.Timeframe
import app.coinverse.utils.buildTypeTimescale
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.launch

/**
 * Todo: Remove price graphs and replace with content search bar.
 */
class PriceViewModel(val repository: PriceRepository) : ViewModel() {
    val pricePair = PricePair(Currency.ETH, Currency.BTC)
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
        _timeframe.value = buildTypeTimescale
        _enabledOrderTypes.value = arrayListOf(BID)
        _enabledExchanges.value = arrayListOf(COINBASE)
        val _priceDifferenceLiveData = repository.priceDifferenceDetailsLiveData
        this.priceDifferenceLiveData = map(_priceDifferenceLiveData) { result -> result }
    }

    val graphLiveData = switchMap(toInitializeGraphData) {
        repository.graphLiveData
    }

    val priceGraphXAndYConstraintsLiveData = switchMap(toInitializeGraphData) {
        repository.graphConstraintsLiveData
    }

    fun getPrices(isRealtime: Boolean, isOnCreateCall: Boolean) {
        this.toInitializeGraphData.value = true
        viewModelScope.launch {
            repository.getPrices(isRealtime, isOnCreateCall, timeframe.value!!)
        }
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