package app.coinverse.utils

import app.coinverse.Enums.Exchange
import app.coinverse.Enums.Exchange.*
import app.coinverse.Enums.OrderType
import app.coinverse.Enums.OrderType.ASK
import app.coinverse.R

fun getExchangeColor(exchange: Exchange, orderType: OrderType): Int {
    when (exchange) {
        COINBASE -> {
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

fun getExchangeColor(exchange: Exchange) =
        when (exchange) {
            COINBASE -> R.color.coinbaseBids
            BINANCE -> R.color.binanceBids
            GEMINI -> R.color.geminiBids
            KUCOIN -> R.color.kucoinBids
            KRAKEN -> R.color.krakenBids
            EMPTY -> R.color.transparent
        }
