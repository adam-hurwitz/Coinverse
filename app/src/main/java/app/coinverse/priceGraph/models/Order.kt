package app.coinverse.priceGraph.models

import app.coinverse.utils.Enums.Exchange

data class Order(var exchange: Exchange, var price: Double) : Comparable<Order> {
    override fun compareTo(otherOrder: Order): Int {
        if (price > otherOrder.price) {
            return 1
        } else {
            return -1
        }
    }
    constructor() : this(
            Exchange.EMPTY, 0.0)
}