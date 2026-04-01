package com.fiserv.payments.merchantdemo.data

data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    val totalCents: Long get() = product.priceInCents * quantity
    val totalFormatted: String
        get() {
            val dollars = totalCents / 100
            val cents = totalCents % 100
            return "$${"%,d".format(dollars)}.${"%02d".format(cents)}"
        }
}
