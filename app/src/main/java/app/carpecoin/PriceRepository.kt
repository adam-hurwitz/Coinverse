package app.carpecoin

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.jjoe64.graphview.series.DataPoint
import java.util.Calendar
import app.carpecoin.Enums.Exchange
import app.carpecoin.Enums.Exchange.*
import kotlin.collections.HashMap
import app.carpecoin.models.price.*
import com.firebase.client.*
import com.jjoe64.graphview.series.LineGraphSeries
import com.firebase.client.DataSnapshot
import app.carpecoin.Enums.Timeframe
import app.carpecoin.Enums.Timeframe.DAY


object PriceRepository {
    private const val CARPECOIN_FIREBASE_URL = "https://carpecoin-14767.firebaseio.com/"
    private const val MAX_PRICE_PERCENT_DIFF_PATH = "maximumPercentDifference"
    private const val ORDER_BY_TIMESTAMP: String = "timestamp"

    private val FIREBASE_REFERENCE = Firebase(CARPECOIN_FIREBASE_URL).child(MAX_PRICE_PERCENT_DIFF_PATH)

    private var xIndex = 0.0

    var priceGraphLiveData = MutableLiveData<HashMap<Exchange, PriceGraphLiveData>>()
    var percentDifference = MutableLiveData<PercentDifference>()
    var priceGraphXAndYConstraintsLiveData = MutableLiveData<PriceGraphXAndYConstraints>()
    var isPriceGraphDataLoadedLiveData = MutableLiveData<Boolean>()

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

    fun getPercentPriceDifferenceLiveData(): LiveData<PercentDifference> {
        return percentDifference
    }

    fun getPricingGraphXAndYConstraintsLiveData(): MutableLiveData<PriceGraphXAndYConstraints> {
        return priceGraphXAndYConstraintsLiveData
    }

    fun getIsPriceGraphDataLoadedLiveData(): MutableLiveData<Boolean> {
        return isPriceGraphDataLoadedLiveData
    }

    fun startFirebaseEventListeners(isLiveDataEnabled: Boolean, timeframe: Timeframe?) {
        if (!isLiveDataEnabled) {
            exchangeOrdersDataPointsMap.clear()
            exchangeOrdersLiveDataMap.clear()
            index = 0
        }
        priceGraphChildEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildKey: String?) {
                xIndex = index++.toDouble()
                val maxPercentPriceDifferenceData = dataSnapshot.getValue(MaximumPercentPriceDifference::class.java)
                percentDifference.value = maxPercentPriceDifferenceData.percentDifference
                //TODO: Potentially clean exchangeOrdersDataPointsMap to improve performance.
                //TODO: Fix axis to use dates
                generateGraphData(GDAX, maxPercentPriceDifferenceData.gdaxExchangeOrderData)
                generateGraphData(BINANCE, maxPercentPriceDifferenceData.binanceExchangeOrderData)
                generateGraphData(GEMINI, maxPercentPriceDifferenceData.geminiExchangeOrderData)
                generateGraphData(KUCOIN, maxPercentPriceDifferenceData.kucoinExchangeOrderData)
                generateGraphData(KRAKEN, maxPercentPriceDifferenceData.krakenExchangeOrderData)
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
                        println(String.format("Data loaded:%s", dataSnapshot?.childrenCount))
                        if (!isLiveDataEnabled) {
                            FIREBASE_REFERENCE.removeEventListener(this)
                            FIREBASE_REFERENCE.removeEventListener(priceGraphChildEventListener)
                        }
                    }

                    override fun onCancelled(firebaseError: FirebaseError?) {}
                })
        FIREBASE_REFERENCE
                .orderByChild(ORDER_BY_TIMESTAMP)
                .startAt(getTimeframe(timeframe).toDouble())
                .addChildEventListener(priceGraphChildEventListener)
    }

    //TODO: Refactor.
    private fun generateGraphData(exchange: Exchange, exchangeOrderData: ExchangeOrderData) {
        val baseToQuoteBid = exchangeOrderData.baseToQuoteBid.price
        val baseToQuoteAsk = exchangeOrderData.baseToQuoteAsk.price
        val bidDataPoint = DataPoint(xIndex, baseToQuoteBid)
        val askDataPoint = DataPoint(xIndex, baseToQuoteAsk)
        if (!exchangeOrdersDataPointsMap.containsKey(exchange) &&
                !exchangeOrdersLiveDataMap.containsKey(exchange)) {
            val bidDataPoints = arrayListOf(bidDataPoint)
            val askDataPoints = arrayListOf(askDataPoint)
            exchangeOrdersDataPointsMap[exchange] =
                    ExchangeOrdersDataPoints(bidDataPoints, askDataPoints)
            val bidLiveData = MutableLiveData<LineGraphSeries<DataPoint>>()
            bidLiveData.value = LineGraphSeries(bidDataPoints.toTypedArray())
            val askLiveData = MutableLiveData<LineGraphSeries<DataPoint>>()
            askLiveData.value = LineGraphSeries(askDataPoints.toTypedArray())
            exchangeOrdersLiveDataMap[exchange] = PriceGraphLiveData(bidLiveData, askLiveData)
        } else {
            val bidDataPoints = exchangeOrdersDataPointsMap[exchange]?.bidsLiveData
            bidDataPoints?.add(bidDataPoint)
            val askDataPoints = exchangeOrdersDataPointsMap[exchange]?.asksLiveData
            askDataPoints?.add(askDataPoint)

            exchangeOrdersLiveDataMap[exchange]?.bidsLiveData?.value =
                    LineGraphSeries(bidDataPoints?.toTypedArray())
            exchangeOrdersLiveDataMap[exchange]?.asksLiveData?.value =
                    LineGraphSeries(askDataPoints?.toTypedArray())
        }
        findMinAndMaxGraphConstraints(baseToQuoteBid, baseToQuoteAsk)
        priceGraphLiveData.value = exchangeOrdersLiveDataMap
        println("BID:" + exchange + " " + exchangeOrderData.baseToQuoteBid.price)
        println("ASK:" + exchange + " " + exchangeOrderData.baseToQuoteAsk.price)
    }

    private fun findMinAndMaxGraphConstraints(baseToQuoteBid: Double, baseToQuoteAsk: Double) {
        findMinAndMaxY(Math.max(baseToQuoteBid, baseToQuoteAsk))
        findMinAndMaxX(xIndex)
        priceGraphXAndYConstraintsLiveData.value = PriceGraphXAndYConstraints(minX, maxX, minY, maxY)
    }

    private fun getTimeframe(timeframe: Timeframe?): Long {
        var timeframeToQuery = 0
        when (timeframe) {
            DAY -> timeframeToQuery = -1
            else -> timeframeToQuery = -1
        }
        val calendar = Calendar.getInstance()
        calendar.time = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, timeframeToQuery)
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
