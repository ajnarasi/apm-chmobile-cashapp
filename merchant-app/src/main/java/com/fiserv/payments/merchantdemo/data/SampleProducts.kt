package com.fiserv.payments.merchantdemo.data

object SampleProducts {
    val products = listOf(
        Product(
            id = "1",
            name = "AirPods Pro",
            description = "Active Noise Cancellation, Adaptive Audio",
            priceInCents = 24999,
            imageEmoji = "\uD83C\uDFA7",
            category = "Audio"
        ),
        Product(
            id = "2",
            name = "MagSafe Charger",
            description = "15W wireless charging pad",
            priceInCents = 3999,
            imageEmoji = "\uD83D\uDD0B",
            category = "Charging"
        ),
        Product(
            id = "3",
            name = "Leather Wallet Case",
            description = "Premium Italian leather, MagSafe compatible",
            priceInCents = 5999,
            imageEmoji = "\uD83D\uDCF1",
            category = "Cases"
        ),
        Product(
            id = "4",
            name = "Studio Display",
            description = "27-inch 5K Retina, Nano-texture glass",
            priceInCents = 159999,
            imageEmoji = "\uD83D\uDDA5\uFE0F",
            category = "Displays"
        ),
        Product(
            id = "5",
            name = "Magic Keyboard",
            description = "Touch ID, numeric keypad, USB-C",
            priceInCents = 19999,
            imageEmoji = "\u2328\uFE0F",
            category = "Input"
        ),
        Product(
            id = "6",
            name = "USB-C Hub Pro",
            description = "8-in-1: HDMI, SD, USB-A, Ethernet",
            priceInCents = 7999,
            imageEmoji = "\uD83D\uDD0C",
            category = "Accessories"
        ),
    )

    val categories: List<String>
        get() = listOf("All") + products.map { it.category }.distinct()
}
