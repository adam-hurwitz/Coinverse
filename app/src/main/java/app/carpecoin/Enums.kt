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
        YOUTUBE(1), NONE(-1)
    }

    enum class UserAction {
        SAVE, ARCHIVE
    }

    enum class FeedType(val code: Int) {
        MAIN(1), SAVED(2), ARCHIVED(3), NONE(-1)
    }
}