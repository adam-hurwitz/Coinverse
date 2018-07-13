package app.carpecoin.models.price

import app.carpecoin.Enums.PairValue
import app.carpecoin.Enums.Exchange.EMPTY
import java.sql.Timestamp

data class MaximumPricePercentDifference(
        val timestamp: Timestamp?,
        val pair: PairValue,
        val gdaxOrderAverages: OrderAverages,
        val krakenOrderAverages: OrderAverages,
        val binanceOrderAverages: OrderAverages,
        val kucoinOrderAverages: OrderAverages,
        val geminiOrderAverages: OrderAverages,
        val percentDifference: PercentDifference
) {
    constructor() : this(
            null,
            PairValue.EMPTY,
            OrderAverages(EMPTY, Order(EMPTY, 0.0), Order(EMPTY, 0.0),
                    Order(EMPTY, 0.0), Order(EMPTY, 0.0)),
            OrderAverages(EMPTY, Order(EMPTY, 0.0), Order(EMPTY, 0.0),
                    Order(EMPTY, 0.0), Order(EMPTY, 0.0)),
            OrderAverages(EMPTY, Order(EMPTY, 0.0), Order(EMPTY, 0.0),
                    Order(EMPTY, 0.0), Order(EMPTY, 0.0)),
            OrderAverages(EMPTY, Order(EMPTY, 0.0), Order(EMPTY, 0.0),
                    Order(EMPTY, 0.0), Order(EMPTY, 0.0)),
            OrderAverages(EMPTY, Order(EMPTY, 0.0), Order(EMPTY, 0.0),
                    Order(EMPTY, 0.0), Order(EMPTY, 0.0)),
            PercentDifference(0.0, 0.0, EMPTY, 0.0, EMPTY)
    )
}