package com.fiserv.payments.api.payment.data

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val paymentTransactionKey: Int? = null,
    val transactionId: String? = null,
    val clientTransactionId: String? = null,
    val description: String? = null,
    val cardType: String? = null,
    val cardHolderName: String? = null,
    val cardNumberLastFour: String? = null,
    val paymentMethod: String? = null,
    val amount: Double = 0.0,
    val surcharge: Double = 0.0,
    val currencyCode: String? = "USD",
    val transactionStatus: String? = null,
    val gatewayResultCode: String? = null,
    val gatewayResultMessage: String? = null,
    val message: String? = null,
    val failureReason: String? = null
)
