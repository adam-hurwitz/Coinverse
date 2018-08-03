package app.carpecoin

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.util.Log
import com.jjoe64.graphview.series.DataPoint
import app.carpecoin.Enums.Exchange
import app.carpecoin.Enums.Exchange.*
import kotlin.collections.HashMap
import app.carpecoin.models.price.*
import com.jjoe64.graphview.series.LineGraphSeries
import app.carpecoin.Enums.Timeframe
import app.carpecoin.Enums.Timeframe.DAY
import app.carpecoin.utils.auth.Auth
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*


object PriceRepository {
    private const val MAX_PRICE_DIFFERENCE_COLLECTION = "maximumPercentDifference"
    private const val TIMESTAMP: String = "timestamp"

    private val LOG_TAG = PriceRepository::class.java.simpleName
    //TODO: Use dependency injection.
    private val priceDifferenceCollection = FirebaseFirestore
            .getInstance(FirebaseApp.getInstance(Auth.PRICE_SERVICE))
            .collection(MAX_PRICE_DIFFERENCE_COLLECTION)

    private var xIndex = 0.0

    var priceGraphLiveData = MutableLiveData<HashMap<Exchange, PriceGraphLiveData>>()
    var percentDifference = MutableLiveData<PercentDifference>()
    var priceGraphXAndYConstraintsLiveData = MutableLiveData<PriceGraphXAndYConstraints>()
    var isPriceGraphDataLoadedLiveData = MutableLiveData<Boolean>()

    lateinit var priceDataListenerRegistration: ListenerRegistration

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

    fun startFirestoreEventListeners(isLiveDataEnabled: Boolean, timeframe: Timeframe?) {

        if (!isLiveDataEnabled) {
            exchangeOrdersDataPointsMap.clear()
            exchangeOrdersLiveDataMap.clear()
            index = 0
        }

        priceDataListenerRegistration = priceDifferenceCollection
                .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
                .whereGreaterThan(TIMESTAMP, getTimeframe(timeframe))
                .addSnapshotListener(EventListener { value, error ->
                    if (error != null) {
                        Log.e(LOG_TAG, "Price Data EventListener Failed.", error)
                        return@EventListener
                    }

                    if (!isLiveDataEnabled) {
                        priceDataListenerRegistration.remove()
                    }

                    if (value!!.documents.isNotEmpty()) {
                        isPriceGraphDataLoadedLiveData.value = true
                    }

                    for (priceDataDocument in value.getDocumentChanges()) {
                        xIndex = index++.toDouble()
                        val priceData = priceDataDocument.document.toObject(MaximumPercentPriceDifference::class.java)
                        percentDifference.value = priceData.percentDifference
                        //TODO: Refactor axis to use dates
                        //TODO: Refactor to Observable.zip()
                        generateGraphData(GDAX, priceData.gdaxExchangeOrderData)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { priceGraphLiveData ->
                                    this.priceGraphLiveData.postValue(priceGraphLiveData)
                                }
                        generateGraphData(BINANCE, priceData.binanceExchangeOrderData)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { priceGraphLiveData ->
                                    this.priceGraphLiveData.postValue(priceGraphLiveData)
                                }
                        generateGraphData(GEMINI, priceData.geminiExchangeOrderData)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { priceGraphLiveData ->
                                    this.priceGraphLiveData.postValue(priceGraphLiveData)
                                }
                        generateGraphData(KUCOIN, priceData.kucoinExchangeOrderData)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { priceGraphLiveData ->
                                    this.priceGraphLiveData.postValue(priceGraphLiveData)
                                }
                        generateGraphData(KRAKEN, priceData.krakenExchangeOrderData)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { priceGraphLiveData ->
                                    this.priceGraphLiveData.postValue(priceGraphLiveData)
                                }
                    }

                })
    }

    private fun generateGraphData(exchange: Exchange, exchangeOrderData: ExchangeOrderData): Observable<HashMap<Exchange, PriceGraphLiveData>> {
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
            bidLiveData.postValue(LineGraphSeries(bidDataPoints.toTypedArray()))
            val askLiveData = MutableLiveData<LineGraphSeries<DataPoint>>()
            askLiveData.postValue(LineGraphSeries(askDataPoints.toTypedArray()))
            exchangeOrdersLiveDataMap[exchange] = PriceGraphLiveData(bidLiveData, askLiveData)
        } else {
            val bidDataPoints = exchangeOrdersDataPointsMap[exchange]?.bidsLiveData
            bidDataPoints?.add(bidDataPoint)
            val askDataPoints = exchangeOrdersDataPointsMap[exchange]?.asksLiveData
            askDataPoints?.add(askDataPoint)

            exchangeOrdersLiveDataMap[exchange]?.bidsLiveData?.postValue(
                    LineGraphSeries(bidDataPoints?.toTypedArray()))
            exchangeOrdersLiveDataMap[exchange]?.asksLiveData?.postValue(
                    LineGraphSeries(askDataPoints?.toTypedArray()))
        }
        findMinAndMaxGraphConstraints(baseToQuoteBid, baseToQuoteAsk)
        return Observable.just(exchangeOrdersLiveDataMap)
    }

    private fun findMinAndMaxGraphConstraints(baseToQuoteBid: Double, baseToQuoteAsk: Double) {
        findMinAndMaxY(Math.max(baseToQuoteBid, baseToQuoteAsk))
        findMinAndMaxX(xIndex)
        priceGraphXAndYConstraintsLiveData.postValue(PriceGraphXAndYConstraints(minX, maxX, minY, maxY))
    }

    private fun getTimeframe(timeframe: Timeframe?): Date {
        var timeframeToQuery: Int
        when (timeframe) {
            DAY -> timeframeToQuery = -1
            else -> timeframeToQuery = -1
        }
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, timeframeToQuery)
        return Date(calendar.timeInMillis)
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
