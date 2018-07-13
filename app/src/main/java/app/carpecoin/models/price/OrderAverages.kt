package app.carpecoin.models.price
import app.carpecoin.Enums.Exchange
import app.carpecoin.Enums.Exchange.EMPTY

data class OrderAverages(var exchange: Exchange,
                         var baseToQuoteBid: Order, var baseToQuoteAsk: Order,
                         var baseToFiatBid: Order?, var quoteToFiatBid: Order?) {
    constructor() : this(
            EMPTY,
            Order(EMPTY, 0.0), Order(EMPTY, 0.0),
            null, null)
}
