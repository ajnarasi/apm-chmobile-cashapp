package com.fiserv.payments.cashapppay.server.models

import kotlinx.serialization.Serializable

@Serializable
data class OnboardMerchantRequest(
    val brandName: String,
    val merchantName: String
)

@Serializable
data class OnboardMerchantResponse(
    val brandId: String,
    val merchantId: String,
    val scopeId: String
)

@Serializable
data class CapturePaymentRequest(
    val grantId: String,
    val amountCents: Long,
    val idempotencyKey: String
)

@Serializable
data class PaymentCaptureResponse(
    val paymentId: String,
    val status: String,
    val amount: Long
)
