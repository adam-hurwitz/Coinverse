package app.coinverse.priceGraph.models

import app.coinverse.utils.Exchange
import app.coinverse.utils.Exchange.EMPTY

data class Order(var exchange: Exchange = EMPTY, var price: Double = 0.0) : Comparable<Order> {
    override fun compareTo(otherOrder: Order): Int {
        if (price > otherOrder.price) {
            return 1
        } else {
            return -1
        }
    }
}