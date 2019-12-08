package app.coinverse.priceGraph.models

import app.coinverse.utils.Exchange
import app.coinverse.utils.Exchange.EMPTY

data class PercentDifference(val percent: Double = 0.0,
                             val baseToQuoteBid: Double = 0.0,
                             val bidExchange: Exchange = EMPTY,
                             val baseToQuoteAsk: Double = 0.0,
                             val askExchange: Exchange = EMPTY)