package com.fiserv.payments.cashapppay

import com.fiserv.payments.cashapppay.api.CapturePaymentRequest
import com.fiserv.payments.cashapppay.api.PaymentCaptureResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * HTTP client that calls the payment-capture backend to finalize an authorized
 * Cash App Pay grant.
 *
 * Uses OkHttp for networking and kotlinx.serialization for JSON (de)serialization.
 *
 * @param config Infrastructure configuration holding the backend base URL.
 */
class CashAppPayCaptureClient(
    private val config: CashAppPayInfraConfig
) {
    private val httpClient = OkHttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Capture (initiate) a payment against the backend.
     *
     * @param grantId        One-time grant ID from Cash App authorization.
     * @param amountCents    Amount in cents to capture.
     * @param idempotencyKey Unique key to guarantee at-most-once semantics.
     * @return [Result] wrapping [PaymentCaptureResponse] on success, or the error on failure.
     */
    suspend fun capturePayment(
        grantId: String,
        amountCents: Long,
        idempotencyKey: String
    ): Result<PaymentCaptureResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val requestBody = json.encodeToString(
                CapturePaymentRequest(
                    grantId = grantId,
                    amountCents = amountCents,
                    idempotencyKey = idempotencyKey
                )
            )

            val request = Request.Builder()
                .url("${config.backendBaseUrl}/api/payments/initiate")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                error("Payment capture failed: HTTP ${response.code} -- ${response.body?.string()}")
            }

            val body = response.body?.string()
                ?: error("Payment capture returned empty body")

            json.decodeFromString<PaymentCaptureResponse>(body)
        }
    }
}
