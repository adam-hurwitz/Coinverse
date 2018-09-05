package app.carpecoin

object Enums {

    enum class Timeframe {
        HOUR, DAY, WEEK, MONTH, YEAR
    }

    // Price graph.
    enum class Exchange {
        GDAX, BINANCE, KUCOIN, KRAKEN, GEMINI, EMPTY
    }

    enum class OrderType {
        ASK, BID
    }

    enum class Currency {
        BTC, ETH
    }

    enum class Status {
        SUCCESS, ERROR
    }

    enum class ContentType(val code: Int) {
        EMPTY(0),
        YOUTUBE(1)
    }
}