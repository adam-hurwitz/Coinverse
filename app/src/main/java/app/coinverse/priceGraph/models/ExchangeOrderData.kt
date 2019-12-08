package app.coinverse.priceGraph.models

import app.coinverse.utils.Exchange
import app.coinverse.utils.Exchange.EMPTY

data class ExchangeOrderData(var exchange: Exchange = EMPTY,
                             var baseVolume: Double = 0.0,
                             var quoteVolume: Double = 0.0,
                             var baseToQuoteBid: Order = Order(EMPTY, 0.0),
                             var baseToQuoteAsk: Order = Order(EMPTY, 0.0),
                             var baseToFiatBid: Order? = null,
                             var quoteToFiatBid: Order? = null)