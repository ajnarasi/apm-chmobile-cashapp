package com.fiserv.payments.merchantdemo.data

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val priceInCents: Long,
    val imageEmoji: String,
    val category: String = "General"
) {
    val priceFormatted: String
        get() {
            val dollars = priceInCents / 100
            val cents = priceInCents % 100
            return "$${"%,d".format(dollars)}.${"%02d".format(cents)}"
        }
}
