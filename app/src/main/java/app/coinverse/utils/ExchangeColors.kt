package app.coinverse.utils

import app.coinverse.Enums.Exchange
import app.coinverse.Enums.Exchange.*
import app.coinverse.Enums.OrderType
import app.coinverse.Enums.OrderType.ASK
import app.coinverse.R

object ExchangeColors {
    fun get(exchange: Exchange, orderType: OrderType): Int {
        when (exchange) {
            GDAX -> {
                if (orderType == ASK) {
                    return R.color.coinbaseAsks
                } else {
                    return R.color.coinbaseBids
                }
            }
            BINANCE -> {
                if (orderType == ASK) {
                    return R.color.binanceAsks
                } else {
                    return R.color.binanceBids
                }
            }
            GEMINI -> {
                if (orderType == ASK) {
                    return R.color.geminiAsks
                } else {
                    return R.color.geminiBids
                }
            }
            KUCOIN -> {
                if (orderType == ASK) {
                    return R.color.kucoinAsks
                } else {
                    return R.color.kucoinBids
                }
            }
            KRAKEN -> {
                if (orderType == ASK) {
                    return R.color.krakenAsks
                } else {
                    return R.color.krakenBids
                }
            }
        }
        return 0
    }
}