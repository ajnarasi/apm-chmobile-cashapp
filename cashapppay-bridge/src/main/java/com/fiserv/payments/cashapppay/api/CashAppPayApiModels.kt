package com.fiserv.payments.cashapppay.api

import kotlinx.serialization.Serializable

/**
 * Request body sent to the backend to capture an authorized Cash App Pay grant.
 */
@Serializable
data class CapturePaymentRequest(
    val grantId: String,
    val amountCents: Long,
    val idempotencyKey: String
)

/**
 * Response returned by the backend after a successful payment capture.
 */
@Serializable
data class PaymentCaptureResponse(
    val paymentId: String,
    val status: String,
    val amount: Long
)

/**
 * Request body sent to the backend to onboard a new merchant with Cash App Pay.
 */
@Serializable
data class OnboardMerchantRequest(
    val brandName: String,
    val merchantName: String
)

/**
 * Response returned by the backend after successful merchant onboarding.
 */
@Serializable
data class OnboardMerchantResponse(
    val brandId: String,
    val merchantId: String,
    val scopeId: String
)
