package com.fiserv.payments.cashapppay.server.routes

import com.fiserv.payments.cashapppay.server.client.CashAppSandboxClient
import com.fiserv.payments.cashapppay.server.models.CapturePaymentRequest
import com.fiserv.payments.cashapppay.server.models.PaymentCaptureResponse
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonObject

fun Route.paymentRoutes(client: CashAppSandboxClient) {
    post("/api/payments/initiate") {
        val request = call.receive<CapturePaymentRequest>()

        val response = client.createPayment(
            grantId = request.grantId,
            amountCents = request.amountCents,
            idempotencyKey = request.idempotencyKey
        )
        val responseBody = response.bodyAsText()

        if (!response.status.isSuccess()) {
            call.respond(HttpStatusCode.fromValue(response.status.value), responseBody)
            return@post
        }

        val rootJson = Json.parseToJsonElement(responseBody).jsonObject
        val paymentJson = rootJson["payment"]?.jsonObject ?: rootJson
        call.respond(PaymentCaptureResponse(
            paymentId = paymentJson["id"]?.jsonPrimitive?.content ?: "",
            status = paymentJson["status"]?.jsonPrimitive?.content ?: "CAPTURED",
            amount = request.amountCents
        ))
    }

    get("/api/payments/{id}/status") {
        val paymentId = call.parameters["id"] ?: return@get call.respond(
            HttpStatusCode.BadRequest, mapOf("error" to "Missing payment ID")
        )
        // In production, this would query Cash App API for payment status
        // For sandbox, return a mock status
        call.respond(mapOf(
            "payment_id" to paymentId,
            "status" to "COMPLETED"
        ))
    }
}
