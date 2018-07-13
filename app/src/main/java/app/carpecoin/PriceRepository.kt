package app.carpecoin

import android.arch.lifecycle.MutableLiveData
import com.jjoe64.graphview.series.DataPoint
import java.util.Calendar
import app.carpecoin.Enums.Exchange
import kotlin.collections.HashMap
import app.carpecoin.Enums.Exchange.GDAX
import app.carpecoin.models.price.*
import com.firebase.client.*
import com.jjoe64.graphview.series.LineGraphSeries
import com.firebase.client.DataSnapshot


object PriceRepository {
    val CARPECOIN_FIREBASE_URL = "https://carpecoin-14767.firebaseio.com/"
    val MAX_PRICE_PERCENT_DIFF_PATH = "maximumPercentDifference"
    val FIREBASE_REFERENCE = Firebase(CARPECOIN_FIREBASE_URL).child(MAX_PRICE_PERCENT_DIFF_PATH)
    val TIMESTAMP: String = "timestamp"
    val QUERY_LAST_THIRTY_DAYS = -30
    val GDAX_ORDER_DATA = "gdaxOrderAverages"
    val BINANCE_ORDER_DATA = "binanceOrderAverages"
    val KRAKEN_ORDER_DATA = "krakenOrderAverages"
    val KUCOIN_ORDER_DATA = "kucoinOrderAverages"
    val GEMINI_ORDER_DATA = "geminiOrderAverages"

    var priceGraphLiveData = MutableLiveData<HashMap<Exchange, PriceGraphLiveData>>()
    var priceGraphXAndYConstraintsLiveData = MutableLiveData<PriceGraphXAndYConstraints>()
    var isPriceGraphDataLoadedLiveData = MutableLiveData<Boolean>()
    var xIndex = 0.0

    lateinit var priceGraphChildEventListener: ChildEventListener

    private var index = 0
    private var minY: Double = 1000000000000.0
    private var maxY: Double = -1000000000000.0
    private var minX: Double = 1000000000000.0
    private var maxX: Double = -1000000000000.0
    private var exchangeOrdersDataPointsMap = HashMap<Exchange, ExchangeOrdersDataPoints>()
    private var exchangeOrdersLiveDataMap = HashMap<Exchange, PriceGraphLiveData>()

    fun getPricingGraphLiveData(): MutableLiveData<HashMap<Exchange, PriceGraphLiveData>> {
        return priceGraphLiveData
    }

    fun getPricingGraphXAndYConstraintsLiveData(): MutableLiveData<PriceGraphXAndYConstraints> {
        return priceGraphXAndYConstraintsLiveData
    }

    fun getIsPriceGraphDataLoadedLiveData(): MutableLiveData<Boolean> {
        return isPriceGraphDataLoadedLiveData
    }

    //TODO: GET DATA BY EACH EXCHANGE
    fun startFirebaseEventListeners(isLiveDataEnabled: Boolean) {
        if (!isLiveDataEnabled) {
            exchangeOrdersDataPointsMap.clear()
            exchangeOrdersLiveDataMap.clear()
            index = 0
        }
        priceGraphChildEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot?, previousChildKey: String?) {
                xIndex = index++.toDouble()
                for (attribute in dataSnapshot?.children ?: listOf()) {
                    when (attribute.key) {
                    //TODO: Toggle which to call based on HashMap passed to Repository
                    //TODO: Pass LiveData object of HashMap back to UI
                        GDAX_ORDER_DATA -> generateGraphData(GDAX, attribute)
                    //BINANCE_ORDER_DATA -> generateGraphData(BINANCE, attribute)
                    //KRAKEN_ORDER_DATA -> generateGraphData(KRAKEN, attribute)
                    //KUCOIN_ORDER_DATA -> generateGraphData(KUCOIN, attribute)
                    //GEMINI_ORDER_DATA -> generateGraphData(GEMINI, attribute)
                    }
                }
            }

            override fun onCancelled(firebaseError: FirebaseError?) {
                // Not implemented.
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot?, p1: String?) {
                // Not implemented.
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot?, p1: String?) {
                // Not implemented.
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot?) {
                // Not implemented.
            }
        }

        FIREBASE_REFERENCE
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot?) {
                        isPriceGraphDataLoadedLiveData.value = true
                        if (!isLiveDataEnabled) {
                            FIREBASE_REFERENCE.removeEventListener(this)
                            FIREBASE_REFERENCE.removeEventListener(priceGraphChildEventListener)
                        }
                    }

                    override fun onCancelled(firebaseError: FirebaseError?) {}
                })
        FIREBASE_REFERENCE
                .orderByChild(TIMESTAMP)
                .startAt(getDateFromThirtyDaysAgo().toDouble())
                .addChildEventListener(priceGraphChildEventListener)
    }

    //TODO: Refactor
    private fun generateGraphData(exchange: Exchange, attribute: DataSnapshot) {
        var exchangeOrderAverages = attribute.getValue(OrderAverages::class.java)
        var baseToQuoteBid = exchangeOrderAverages.baseToQuoteBid.price
        var baseToQuoteAsk = exchangeOrderAverages.baseToQuoteAsk.price
        var bidDataPoint = DataPoint(xIndex, baseToQuoteBid)
        var askDataPoint = DataPoint(xIndex, baseToQuoteAsk)
        if (!exchangeOrdersDataPointsMap.containsKey(exchange) &&
                !exchangeOrdersLiveDataMap.containsKey(exchange)) {
            var bidDataPoints = arrayListOf(bidDataPoint)
            var askDataPoints = arrayListOf(askDataPoint)
            exchangeOrdersDataPointsMap.put(
                    exchange, ExchangeOrdersDataPoints(bidDataPoints, askDataPoints))
            var bidLiveData = MutableLiveData<LineGraphSeries<DataPoint>>()
            bidLiveData.value = LineGraphSeries(bidDataPoints.toTypedArray())
            var askLiveData = MutableLiveData<LineGraphSeries<DataPoint>>()
            askLiveData.value = LineGraphSeries(askDataPoints.toTypedArray())
            exchangeOrdersLiveDataMap.put(exchange, PriceGraphLiveData(bidLiveData, askLiveData))
        } else {
            var bidDataPoints = exchangeOrdersDataPointsMap.get(exchange)?.bidsLiveData
            bidDataPoints?.add(bidDataPoint)
            var askDataPoints = exchangeOrdersDataPointsMap.get(exchange)?.asksLiveData
            askDataPoints?.add(askDataPoint)

            exchangeOrdersLiveDataMap.get(exchange)?.bidsLiveData?.value =
                    LineGraphSeries(bidDataPoints?.toTypedArray())
            exchangeOrdersLiveDataMap.get(exchange)?.asksLiveData?.value =
                    LineGraphSeries(askDataPoints?.toTypedArray())
        }
        findMinAndMaxGraphConstraints(baseToQuoteBid, baseToQuoteAsk)
        priceGraphLiveData.value = exchangeOrdersLiveDataMap
        println("BID:" + exchange + " " + exchangeOrderAverages.baseToQuoteBid.price)
        println("ASK:" + exchange + " " + exchangeOrderAverages.baseToQuoteAsk.price)
    }

    private fun findMinAndMaxGraphConstraints(baseToQuoteBid: Double, baseToQuoteAsk: Double) {
        findMinAndMaxY(Math.max(baseToQuoteBid, baseToQuoteAsk))
        findMinAndMaxX(xIndex)
        priceGraphXAndYConstraintsLiveData.value = PriceGraphXAndYConstraints(minX, maxX, minY, maxY)
    }

    private fun getDateFromThirtyDaysAgo(): Long {
        val calendar = Calendar.getInstance()
        calendar.setTime(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, QUERY_LAST_THIRTY_DAYS)
        return calendar.timeInMillis
    }

    private fun findMinAndMaxX(xIndex: Double) {
        if (xIndex < minX) {
            minX = xIndex
        } else if (xIndex > maxX) {
            maxX = xIndex
        }
    }

    private fun findMinAndMaxY(averageWeightedPrice: Double?) {
        if (averageWeightedPrice!! < minY) {
            minY = averageWeightedPrice
        } else if (averageWeightedPrice > maxY) {
            maxY = averageWeightedPrice
        }
    }

}
