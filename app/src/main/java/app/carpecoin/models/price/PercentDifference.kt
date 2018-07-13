package app.carpecoin.models.price
import app.carpecoin.Enums.Exchange
import app.carpecoin.Enums.Exchange.EMPTY

data class PercentDifference(val percent: Double,
                             val baseToQuoteBid: Double, val bidExchange: Exchange,
                             val baseToQuoteAsk: Double, val askExchange: Exchange) {
    constructor(): this(0.0, 0.0, EMPTY, 0.0, EMPTY)
}