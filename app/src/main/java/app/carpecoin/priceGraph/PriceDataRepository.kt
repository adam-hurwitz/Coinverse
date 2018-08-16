package app.carpecoin.priceGraph

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.jjoe64.graphview.series.DataPoint
import app.carpecoin.Enums.Exchange
import app.carpecoin.Enums.Status
import app.carpecoin.Enums.Status.SUCCESS
import app.carpecoin.Enums.Exchange.*
import kotlin.collections.HashMap
import com.jjoe64.graphview.series.LineGraphSeries
import app.carpecoin.Enums.Timeframe
import app.carpecoin.priceGraph.models.*
import app.carpecoin.utils.Constants.TIMESTAMP
import app.carpecoin.utils.DateAndTime.getTimeframe
import app.carpecoin.utils.auth.Auth
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.functions.Function5

private const val MAX_PRICE_DIFFERENCE_COLLECTION = "maximumPercentDifference"
private val LOG_TAG = PriceDataRepository::class.java.simpleName

object PriceDataRepository {

    var graphLiveData = MutableLiveData<HashMap<Exchange, PriceGraphLiveData>>()
    var priceDifferenceDetailsLiveData = MutableLiveData<PercentDifference>()
    var graphConstraintsLiveData = MutableLiveData<PriceGraphXAndYConstraints>()

    //TODO: Use dependency injection.
    private val priceDifferenceCollection = FirebaseFirestore
            .getInstance(FirebaseApp.getInstance(Auth.PRICE_FIRESTORE_NAME))
            .collection(MAX_PRICE_DIFFERENCE_COLLECTION)

    private var xIndex = 0.0
    private var index = 0
    private var minY: Double = 1000000000000.0
    private var maxY: Double = -1000000000000.0
    private var minX: Double = 1000000000000.0
    private var maxX: Double = -1000000000000.0

    private var exchangeOrdersPointsMap = HashMap<Exchange, ExchangeOrdersDataPoints>()
    private var exchangeOrdersLiveDataMap = HashMap<Exchange, PriceGraphLiveData>()

    private lateinit var listenerRegistration: ListenerRegistration

    fun startFirestoreEventListeners(isLiveDataEnabled: Boolean, timeframe: Timeframe) {

        if (!isLiveDataEnabled) {
            exchangeOrdersPointsMap.clear()
            exchangeOrdersLiveDataMap.clear()
            index = 0
        }

        val compositeDisposable = CompositeDisposable()
        listenerRegistration = priceDifferenceCollection
                .orderBy(TIMESTAMP, Query.Direction.ASCENDING)
                .whereGreaterThan(TIMESTAMP, getTimeframe(timeframe))
                .addSnapshotListener(EventListener { value, error ->
                    error?.run {
                        Log.e(LOG_TAG, "Price Data EventListener Failed.", error)
                        return@EventListener
                    }

                    if (!isLiveDataEnabled) listenerRegistration.remove()

                    //FIXME: Debugging graphLiveData being observed multiple times when back is pressed.
                    println(String.format("DOUBLE_GRAPH: listenerRegistration"))

                    for (priceDataDocument in value!!.getDocumentChanges()) {
                        xIndex = index++.toDouble()
                        val priceData = priceDataDocument.document
                                .toObject(MaximumPercentPriceDifference::class.java)
                        priceDifferenceDetailsLiveData.value = priceData.percentDifference
                        //TODO: Refactor axis to use dates

                        compositeDisposable.add(Observable.zip(
                                generateGraphData(GDAX, priceData.gdaxExchangeOrderData).subscribeOn(Schedulers.io()),
                                generateGraphData(BINANCE, priceData.binanceExchangeOrderData).subscribeOn(Schedulers.io()),
                                generateGraphData(GEMINI, priceData.geminiExchangeOrderData).subscribeOn(Schedulers.io()),
                                generateGraphData(KUCOIN, priceData.kucoinExchangeOrderData).subscribeOn(Schedulers.io()),
                                generateGraphData(KRAKEN, priceData.krakenExchangeOrderData).subscribeOn(Schedulers.io()),
                                getFunction5())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeWith(object : DisposableObserver<Status>() {
                                    override fun onNext(status: Status) {
                                        Log.v(LOG_TAG, String.format("Graph data status: %s", status))
                                    }

                                    override fun onError(e: Throwable) {
                                        e.printStackTrace()
                                    }

                                    override fun onComplete() {
                                        Log.v(LOG_TAG, String.format("Graph data status: complete"))
                                    }
                                }))
                    }
                    compositeDisposable.dispose()
                })
    }

    private fun generateGraphData(exchange: Exchange, exchangeOrderData: ExchangeOrderData): Observable<HashMap<Exchange, PriceGraphLiveData>> {
        val baseToQuoteBid = exchangeOrderData.baseToQuoteBid.price
        val baseToQuoteAsk = exchangeOrderData.baseToQuoteAsk.price
        val bidDataPoint = DataPoint(xIndex, baseToQuoteBid)
        val askDataPoint = DataPoint(xIndex, baseToQuoteAsk)
        if (!exchangeOrdersPointsMap.containsKey(exchange) &&
                !exchangeOrdersLiveDataMap.containsKey(exchange)) {
            val bidDataPoints = arrayListOf(bidDataPoint)
            val askDataPoints = arrayListOf(askDataPoint)
            exchangeOrdersPointsMap[exchange] =
                    ExchangeOrdersDataPoints(bidDataPoints, askDataPoints)
            val bidLiveData = MutableLiveData<LineGraphSeries<DataPoint>>()
            bidLiveData.postValue(LineGraphSeries(bidDataPoints.toTypedArray()))
            val askLiveData = MutableLiveData<LineGraphSeries<DataPoint>>()
            askLiveData.postValue(LineGraphSeries(askDataPoints.toTypedArray()))
            exchangeOrdersLiveDataMap[exchange] = PriceGraphLiveData(bidLiveData, askLiveData)
        } else {
            val bidDataPoints = exchangeOrdersPointsMap[exchange]?.bidsLiveData
            bidDataPoints?.add(bidDataPoint)
            val askDataPoints = exchangeOrdersPointsMap[exchange]?.asksLiveData
            askDataPoints?.add(askDataPoint)

            exchangeOrdersLiveDataMap[exchange]?.bidsLiveData?.postValue(
                    LineGraphSeries(bidDataPoints?.toTypedArray()))
            exchangeOrdersLiveDataMap[exchange]?.asksLiveData?.postValue(
                    LineGraphSeries(askDataPoints?.toTypedArray()))
        }
        findMinAndMaxGraphConstraints(baseToQuoteBid, baseToQuoteAsk)
        return Observable.just(exchangeOrdersLiveDataMap)
    }

    private fun getFunction5(): Function5<
            HashMap<Exchange, PriceGraphLiveData>,
            HashMap<Exchange, PriceGraphLiveData>,
            HashMap<Exchange, PriceGraphLiveData>,
            HashMap<Exchange, PriceGraphLiveData>,
            HashMap<Exchange, PriceGraphLiveData>,
            Status> {
        return Function5 { gdaxPriceGraphLiveData,
                           binancePriceGraphLiveData,
                           geminiPriceGraphLiveData,
                           kucoinPriceGraphLiveData,
                           krakenPriceGraphLiveData ->
            graphLiveData.postValue(gdaxPriceGraphLiveData)
            graphLiveData.postValue(binancePriceGraphLiveData)
            graphLiveData.postValue(geminiPriceGraphLiveData)
            graphLiveData.postValue(kucoinPriceGraphLiveData)
            graphLiveData.postValue(krakenPriceGraphLiveData)
            SUCCESS
        }
    }

    private fun findMinAndMaxGraphConstraints(baseToQuoteBid: Double, baseToQuoteAsk: Double) {
        findMinAndMaxY(Math.max(baseToQuoteBid, baseToQuoteAsk))
        findMinAndMaxX(xIndex)
        graphConstraintsLiveData.postValue(PriceGraphXAndYConstraints(minX, maxX, minY, maxY))
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
