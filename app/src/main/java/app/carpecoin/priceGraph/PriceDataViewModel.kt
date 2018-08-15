package app.carpecoin.priceGraph

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import app.carpecoin.priceGraph.models.PercentDifference
import app.carpecoin.Enums.Exchange
import app.carpecoin.Enums.Exchange.GDAX
import app.carpecoin.priceGraph.models.PricePair
import app.carpecoin.Enums.Currency.ETH
import app.carpecoin.Enums.Currency.BTC
import app.carpecoin.Enums.Timeframe
import app.carpecoin.Enums.Timeframe.DAY

class PriceDataViewModel : ViewModel() {

    //TODO: Inject PriceDataRepository

    //TODO: Query Firebase by pricePair.
    var pricePair = PricePair(ETH, BTC)

    //TODO: Paid feature
    var isRealtimeDataEnabled = true

    //TODO: Set timeframe from UI.
    var timeframe = MutableLiveData<Timeframe>()
    var enabledExchanges = MutableLiveData<ArrayList<Exchange?>>()
    var priceDifferenceDetailsLiveData: LiveData<PercentDifference>

    private var toInitializePriceGraphData = MutableLiveData<Boolean>()

    init {
        val percentPriceDifferenceLiveData = PriceDataRepository.priceDifferenceDetailsLiveData
        timeframe.value = DAY
        enabledExchanges.value = arrayListOf(GDAX)
        this.priceDifferenceDetailsLiveData =
                Transformations.map(percentPriceDifferenceLiveData) { result -> result }
    }

    fun initializeData(isRealtimeDataEnabled: Boolean) {
        this.toInitializePriceGraphData.value = true
        PriceDataRepository.startFirestoreEventListeners(isRealtimeDataEnabled, timeframe.value!!)
    }

    val graphLiveData = Transformations.switchMap(toInitializePriceGraphData) {
        PriceDataRepository.graphLiveData
    }

    val priceGraphXAndYConstraintsLiveData = Transformations.switchMap(toInitializePriceGraphData) {
        PriceDataRepository.graphConstraintsLiveData
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