package com.fiserv.payments.cashapppay.server.routes

import com.fiserv.payments.cashapppay.server.client.CashAppSandboxClient
import com.fiserv.payments.cashapppay.server.models.OnboardMerchantRequest
import com.fiserv.payments.cashapppay.server.models.OnboardMerchantResponse
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun Route.onboardingRoutes(client: CashAppSandboxClient) {
    post("/api/setup/onboard-merchant") {
        val request = call.receive<OnboardMerchantRequest>()

        // Step 1: Create brand
        val brandResponse = client.createBrand(
            name = request.brandName,
            referenceId = "fiserv-brand-${System.currentTimeMillis()}"
        )
        val brandBody = brandResponse.bodyAsText()

        if (!brandResponse.status.isSuccess()) {
            call.respond(HttpStatusCode.fromValue(brandResponse.status.value), brandBody)
            return@post
        }

        // Extract brand_id from response
        val brandJson = Json.parseToJsonElement(brandBody).jsonObject
        val brandId = brandJson["brand_id"]?.jsonPrimitive?.content
            ?: brandJson["id"]?.jsonPrimitive?.content
            ?: ""

        // Step 2: Create merchant
        val merchantResponse = client.createMerchant(
            brandId = brandId,
            name = request.merchantName,
            referenceId = "fiserv-merchant-${System.currentTimeMillis()}"
        )
        val merchantBody = merchantResponse.bodyAsText()

        if (!merchantResponse.status.isSuccess()) {
            call.respond(HttpStatusCode.fromValue(merchantResponse.status.value), merchantBody)
            return@post
        }

        // Extract merchant info and return combined response
        val merchantJson = Json.parseToJsonElement(merchantBody).jsonObject
        call.respond(OnboardMerchantResponse(
            brandId = brandId,
            merchantId = merchantJson["merchant_id"]?.jsonPrimitive?.content
                ?: merchantJson["id"]?.jsonPrimitive?.content ?: "",
            scopeId = merchantJson["scope_id"]?.jsonPrimitive?.content ?: brandId
        ))
    }
}
