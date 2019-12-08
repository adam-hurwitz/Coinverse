package app.coinverse.priceGraph.models

import app.coinverse.utils.Exchange.EMPTY
import com.google.firebase.Timestamp

data class MaximumPercentPriceDifference(
        val timestamp: Timestamp = Timestamp.now(),
        val pair: String = "",
        val coinbaseExchangeOrderData: ExchangeOrderData = ExchangeOrderData(EMPTY, 0.0,
                0.0, Order(EMPTY, 0.0), Order(EMPTY, 0.0),
                Order(EMPTY, 0.0), Order(EMPTY, 0.0)),
        val krakenExchangeOrderData: ExchangeOrderData = ExchangeOrderData(EMPTY, 0.0,
                0.0, Order(EMPTY, 0.0), Order(EMPTY, 0.0),
                Order(EMPTY, 0.0), Order(EMPTY, 0.0)),
        val binanceExchangeOrderData: ExchangeOrderData = ExchangeOrderData(EMPTY, 0.0,
                0.0, Order(EMPTY, 0.0), Order(EMPTY, 0.0),
                Order(EMPTY, 0.0), Order(EMPTY, 0.0)),
        val geminiExchangeOrderData: ExchangeOrderData = ExchangeOrderData(EMPTY, 0.0,
                0.0, Order(EMPTY, 0.0), Order(EMPTY, 0.0),
                Order(EMPTY, 0.0), Order(EMPTY, 0.0)),
        val percentDifference: PercentDifference =
                PercentDifference(0.0, 0.0, EMPTY, 0.0, EMPTY))