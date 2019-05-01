package app.coinverse.priceGraph.models

import app.coinverse.utils.Enums.Exchange.EMPTY
import com.google.firebase.Timestamp

data class MaximumPercentPriceDifference(
        val timestamp: Timestamp,
        val pair: String,
        val coinbaseExchangeOrderData: ExchangeOrderData,
        val krakenExchangeOrderData: ExchangeOrderData,
        val binanceExchangeOrderData: ExchangeOrderData,
        val geminiExchangeOrderData: ExchangeOrderData,
        val percentDifference: PercentDifference) {
    constructor() : this(
            Timestamp.now(),
            "",
            ExchangeOrderData(EMPTY, 0.0, 0.0, Order(EMPTY, 0.0),
                    Order(EMPTY, 0.0), Order(EMPTY, 0.0), Order(EMPTY, 0.0)),
            ExchangeOrderData(EMPTY, 0.0, 0.0, Order(EMPTY, 0.0),
                    Order(EMPTY, 0.0), Order(EMPTY, 0.0), Order(EMPTY, 0.0)),
            ExchangeOrderData(EMPTY, 0.0, 0.0, Order(EMPTY, 0.0),
                    Order(EMPTY, 0.0), Order(EMPTY, 0.0), Order(EMPTY, 0.0)),
            ExchangeOrderData(EMPTY, 0.0, 0.0, Order(EMPTY, 0.0),
                    Order(EMPTY, 0.0), Order(EMPTY, 0.0), Order(EMPTY, 0.0)),
            PercentDifference(0.0, 0.0, EMPTY, 0.0, EMPTY)
    )
}