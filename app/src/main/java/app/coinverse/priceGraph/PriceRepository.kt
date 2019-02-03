package app.coinverse.priceGraph

import android.util.Log
import androidx.lifecycle.MutableLiveData
import app.coinverse.Enums.Exchange
import app.coinverse.Enums.Status
import app.coinverse.Enums.Status.SUCCESS
import app.coinverse.Enums.Timeframe
import app.coinverse.firebase.contentEthBtcCollection
import app.coinverse.priceGraph.models.*
import app.coinverse.utils.DateAndTime.getTimeframe
import app.coinverse.utils.TIMESTAMP
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function5
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers

private val LOG_TAG = PriceRepository::class.java.simpleName

object PriceRepository {

    val compositeDisposable = CompositeDisposable()
    var graphLiveData = MutableLiveData<HashMap<Exchange, PriceGraphData>>()
    var priceDifferenceDetailsLiveData = MutableLiveData<PercentDifference>()
    var graphConstraintsLiveData = MutableLiveData<PriceGraphXAndYConstraints>()

    private var xIndex = 0.0
    private var index = 0
    private var minY: Double = 1000000000000.0
    private var maxY: Double = -1000000000000.0
    private var minX: Double = 1000000000000.0
    private var maxX: Double = -1000000000000.0

    private var exchangeOrdersPointsMap = HashMap<Exchange, ExchangeOrdersDataPoints>()
    private var exchangeOrdersDataMap = HashMap<Exchange, PriceGraphData>()

    private lateinit var listenerRegistration: ListenerRegistration

    fun getPrices(isRealtime: Boolean, isOnCreateCall: Boolean, timeframe: Timeframe) {
        if (isRealtime) {
            if (isOnCreateCall) {
                exchangeOrdersPointsMap.clear()
                exchangeOrdersDataMap.clear()
                index = 0
            }
            listenerRegistration = contentEthBtcCollection
                    .orderBy(TIMESTAMP, Query.Direction.ASCENDING)
                    .whereGreaterThan(TIMESTAMP, getTimeframe(timeframe))
                    .addSnapshotListener(EventListener { value, error ->
                        error?.run {
                            Log.e(LOG_TAG, "Price Data EventListener Failed.", error)
                            return@EventListener
                        }
                        parsePriceData(value!!.documentChanges)
                    })
        } else {
            exchangeOrdersPointsMap.clear()
            exchangeOrdersDataMap.clear()
            index = 0
            contentEthBtcCollection
                    .orderBy(TIMESTAMP, Query.Direction.ASCENDING)
                    .whereGreaterThan(TIMESTAMP, getTimeframe(timeframe))
                    .get()
                    .addOnCompleteListener {
                        parsePriceData(it.result!!.documentChanges)
                    }
        }
    }

    private fun parsePriceData(documentChanges: List<DocumentChange>) {
        for (priceDataDocument in documentChanges) {
            xIndex = index++.toDouble()
            val priceData = priceDataDocument.document
                    .toObject(MaximumPercentPriceDifference::class.java)
            priceDifferenceDetailsLiveData.value = priceData.percentDifference
            //TODO: Refactor axis to use dates.

            compositeDisposable.add(Observable.zip(
                    generateGraphData(Exchange.COINBASE, priceData.coinbaseExchangeOrderData).subscribeOn(Schedulers.io()),
                    generateGraphData(Exchange.BINANCE, priceData.binanceExchangeOrderData).subscribeOn(Schedulers.io()),
                    generateGraphData(Exchange.GEMINI, priceData.geminiExchangeOrderData).subscribeOn(Schedulers.io()),
                    generateGraphData(Exchange.KUCOIN, priceData.kucoinExchangeOrderData).subscribeOn(Schedulers.io()),
                    generateGraphData(Exchange.KRAKEN, priceData.krakenExchangeOrderData).subscribeOn(Schedulers.io()),
                    getFunction5())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableObserver<Status>() {
                        override fun onNext(status: Status) {}

                        override fun onError(e: Throwable) {
                            e.printStackTrace()
                        }

                        override fun onComplete() {
                            compositeDisposable.clear()
                        }
                    }))
        }
    }

    private fun generateGraphData(exchange: Exchange, exchangeOrderData: ExchangeOrderData)
            : Observable<HashMap<Exchange, PriceGraphData>> {
        val baseToQuoteBid = exchangeOrderData.baseToQuoteBid.price
        val baseToQuoteAsk = exchangeOrderData.baseToQuoteAsk.price
        val bidDataPoint = DataPoint(xIndex, baseToQuoteBid)
        val askDataPoint = DataPoint(xIndex, baseToQuoteAsk)
        if (!exchangeOrdersPointsMap.containsKey(exchange)
                && !exchangeOrdersDataMap.containsKey(exchange)) {
            val bidDataPoints = arrayListOf(bidDataPoint)
            val askDataPoints = arrayListOf(askDataPoint)
            exchangeOrdersPointsMap[exchange] = ExchangeOrdersDataPoints(bidDataPoints, askDataPoints)
            exchangeOrdersDataMap[exchange] =
                    PriceGraphData(
                            LineGraphSeries(bidDataPoints.toTypedArray()),
                            LineGraphSeries(askDataPoints.toTypedArray()))
        } else {
            val bidDataPoints = exchangeOrdersPointsMap[exchange]?.bidsLiveData
            bidDataPoints?.add(bidDataPoint)
            val askDataPoints = exchangeOrdersPointsMap[exchange]?.asksLiveData
            askDataPoints?.add(askDataPoint)
            exchangeOrdersDataMap[exchange]?.bids = LineGraphSeries(bidDataPoints?.toTypedArray())
            exchangeOrdersDataMap[exchange]?.asks = LineGraphSeries(askDataPoints?.toTypedArray())
        }
        findMinAndMaxGraphConstraints(baseToQuoteBid, baseToQuoteAsk)
        return Observable.just(exchangeOrdersDataMap)
    }

    private fun getFunction5(): Function5<
            HashMap<Exchange, PriceGraphData>,
            HashMap<Exchange, PriceGraphData>,
            HashMap<Exchange, PriceGraphData>,
            HashMap<Exchange, PriceGraphData>,
            HashMap<Exchange, PriceGraphData>,
            Status> {
        return Function5 { coinbasePriceGraphData,
                           binancePriceGraphData,
                           geminiPriceGraphData,
                           kucoinPriceGraphData,
                           krakenPriceGraphData ->
            graphLiveData.postValue(coinbasePriceGraphData)
            graphLiveData.postValue(binancePriceGraphData)
            graphLiveData.postValue(geminiPriceGraphData)
            graphLiveData.postValue(kucoinPriceGraphData)
            graphLiveData.postValue(krakenPriceGraphData)
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
