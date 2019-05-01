package app.coinverse.utils

import app.coinverse.R.color.*
import app.coinverse.utils.Enums.Exchange
import app.coinverse.utils.Enums.Exchange.*
import app.coinverse.utils.Enums.OrderType
import app.coinverse.utils.Enums.OrderType.ASK

fun getExchangeColor(exchange: Exchange, orderType: OrderType) = when (exchange) {
    COINBASE -> if (orderType == ASK) coinbaseAsks else coinbaseBids
    BINANCE -> if (orderType == ASK) binanceAsks else binanceBids
    GEMINI -> if (orderType == ASK) geminiAsks else geminiBids
    KRAKEN -> if (orderType == ASK) krakenAsks else krakenBids
    EMPTY -> transparent
}

fun getExchangeColor(exchange: Exchange) = when (exchange) {
    COINBASE -> coinbaseBids
    BINANCE -> binanceBids
    GEMINI -> geminiBids
    KRAKEN -> krakenBids
    EMPTY -> transparent
}
