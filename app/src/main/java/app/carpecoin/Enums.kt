package app.carpecoin

object Enums {

    enum class Exchange {
        GDAX, BINANCE, KUCOIN, KRAKEN, GEMINI, EMPTY
    }

    enum class OrderType {
        ASK, BID
    }

    enum class Currency {
        BTC, ETH
    }

    enum class Timeframe {
        HOUR, DAY, WEEK, MONTH, YEAR
    }
}