package com.fiserv.payments.api.creditcard

import kotlinx.serialization.Serializable

@Serializable
data class CreditCard(
    val token: String? = null,
    val gatewayToken: String? = null,
    val cardType: String? = null,
    val lastFour: String? = null,
    val expirationMonth: String? = null,
    val expirationYear: String? = null,
    val cardHolderName: String? = null,
    val isDefault: Boolean = false
)
