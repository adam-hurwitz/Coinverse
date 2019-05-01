package app.coinverse.priceGraph.models

import app.coinverse.utils.Enums.Exchange
import app.coinverse.utils.Enums.Exchange.EMPTY

data class PercentDifference(val percent: Double,
                             val baseToQuoteBid: Double, val bidExchange: Exchange,
                             val baseToQuoteAsk: Double, val askExchange: Exchange) {
    constructor(): this(0.0, 0.0, EMPTY, 0.0, EMPTY)
}