package com.fiserv.payments.cashapppay.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class KlarnaSessionRequest(
    val amountCents: Long,
    val locale: String = "en-US",
    val paymentMethodCategory: String = "pay_over_time"
)

@Serializable
data class KlarnaSessionResponse(
    val sessionId: String,
    val clientToken: String,
    val paymentMethodCategories: List<String>
)

@Serializable
data class KlarnaPaymentRequest(
    val amount: Double,
    val authorizationToken: String,
    val paymentMethodCategory: String = "pay_over_time"
)

@Serializable
data class KlarnaPaymentResponse(
    val transactionId: String,
    val orderId: String,
    val amount: Double,
    val status: String,
    val paymentMethod: String,
    val paymentType: String
)

fun Route.klarnaPaymentRoutes() {
    // Create Klarna payment session
    post("/api/klarna/session") {
        val request = call.receive<KlarnaSessionRequest>()

        kotlinx.coroutines.delay(400)

        call.respond(KlarnaSessionResponse(
            sessionId = "klarna_session_${System.currentTimeMillis()}",
            clientToken = "klarna_test_client_QjNrWGw3I1kjITcoMVlzbFhGRVcweE9UbilUL3hpVDUsNTI1NTY1NDItYmViNi00MDkzLWI1OTItNzMxMGM2YmEwODdhLDEsMzhldFFmRDRSS3BVU2Q0azBwcjJsV2UzSEtXc3dxYmNCdEg2T2NHRy9oYz0",
            paymentMethodCategories = listOf("pay_now", "pay_later", "pay_over_time")
        ))
    }

    // Authorize and capture Klarna payment
    post("/api/klarna/payment") {
        val request = call.receive<KlarnaPaymentRequest>()

        kotlinx.coroutines.delay(900)

        val paymentTypeLabel = when (request.paymentMethodCategory) {
            "pay_later" -> "Pay in 30 days"
            "pay_over_time" -> "Pay in 4 installments"
            "pay_now" -> "Pay now"
            else -> "Klarna"
        }

        call.respond(KlarnaPaymentResponse(
            transactionId = "TXN_KLARNA_${System.currentTimeMillis()}",
            orderId = "KL_ORD_${System.currentTimeMillis()}",
            amount = request.amount,
            status = "AUTHORIZED",
            paymentMethod = "KLARNA",
            paymentType = paymentTypeLabel
        ))
    }
}
