package app.carpecoin

import android.arch.lifecycle.*
import app.carpecoin.models.price.PercentDifference
import app.carpecoin.Enums.Exchange
import app.carpecoin.Enums.Exchange.GDAX
import app.carpecoin.models.price.PricePair
import app.carpecoin.Enums.Currency.ETH
import app.carpecoin.Enums.Currency.BTC
import app.carpecoin.Enums.Timeframe
import app.carpecoin.Enums.Timeframe.DAY

class MainViewModel : ViewModel() {
    //TODO: Inject PriceRepository

    //TODO: Query Firebase by pricePair.
    var pricePair = PricePair(ETH, BTC)

    //TODO: Paid feature
    var isRealtimeDataEnabled = true
    var enabledExchanges = MutableLiveData<ArrayList<Exchange?>>()
    var timeframe = MutableLiveData<Timeframe>()
    var percentDifferenceLiveData: LiveData<PercentDifference>

    private var toInitializePriceGraphData = MutableLiveData<Boolean>()

    init {
        val percentPriceDifferenceLiveData = PriceRepository.getPercentPriceDifferenceLiveData()
        enabledExchanges.value = arrayListOf(GDAX)
        this.percentDifferenceLiveData =
                Transformations.map(percentPriceDifferenceLiveData) { result -> result }
        //TODO: Select timeframe in UI.
        timeframe.value = DAY
    }

    val priceGraphLiveData = Transformations.switchMap(toInitializePriceGraphData) {
        PriceRepository.getPricingGraphLiveData()
    }

    val isPriceGraphDataLoadedLiveData = Transformations.switchMap(toInitializePriceGraphData) {
        PriceRepository.getIsPriceGraphDataLoadedLiveData()
    }

    val priceGraphXAndYConstraintsLiveData = Transformations.switchMap(toInitializePriceGraphData) {
        PriceRepository.getPricingGraphXAndYConstraintsLiveData()
    }

    fun initializeData(isRealtimeDataEnabled: Boolean) {
        this.toInitializePriceGraphData.value = true
        PriceRepository.startFirestoreEventListeners(isRealtimeDataEnabled, timeframe.value)
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