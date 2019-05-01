package app.coinverse.priceGraph.models

import app.coinverse.utils.Enums.Exchange
import app.coinverse.utils.Enums.Exchange.EMPTY

data class ExchangeOrderData(var exchange: Exchange,
                             var baseVolume: Double, var quoteVolume: Double,
                             var baseToQuoteBid: Order, var baseToQuoteAsk: Order,
                             var baseToFiatBid: Order?, var quoteToFiatBid: Order?) {
    constructor() : this(
            EMPTY,
            0.0, 0.0,
            Order(EMPTY, 0.0), Order(EMPTY, 0.0),
            null, null)
}
