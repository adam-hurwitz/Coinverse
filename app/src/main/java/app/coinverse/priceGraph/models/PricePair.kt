package app.coinverse.priceGraph.models

import app.coinverse.utils.Enums.Currency

data class PricePair(var BASE_CURRENCY: Currency, var QUOTE_CURRENCY: Currency)