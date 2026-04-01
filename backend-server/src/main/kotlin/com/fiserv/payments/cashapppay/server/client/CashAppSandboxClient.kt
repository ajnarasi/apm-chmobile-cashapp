package com.fiserv.payments.cashapppay.server.client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CashAppSandboxClient(
    private val clientId: String,
    private val apiKeyId: String
) {

    private val networkBaseUrl = "https://sandbox.api.cash.app/network/v1"
    private val merchantId = "MMI_1nk0ecoa69ilax9gno1lz6luh"

    // No ContentNegotiation plugin — we set all headers manually to match
    // Cash App's exact requirements (Accept: application/json is critical)
    private val httpClient = HttpClient(CIO)

    /**
     * Cash App API requires exactly these headers:
     *   Authorization: Client {CLIENT_ID} {API_KEY_ID}
     *   Accept: application/json
     *   Content-Type: application/json
     *   X-Region: IATA airport code (e.g. SFO)
     *   x-signature: sandbox:skip-signature-check (sandbox only)
     */
    private fun HttpRequestBuilder.applyHeaders() {
        header(HttpHeaders.Authorization, "Client $clientId $apiKeyId")
        header(HttpHeaders.Accept, "application/json")
        header(HttpHeaders.ContentType, "application/json")
        header("X-Region", "SFO")
        header("x-signature", "sandbox:skip-signature-check")
    }

    suspend fun createBrand(name: String, referenceId: String): HttpResponse {
        return httpClient.post("$networkBaseUrl/brands") {
            applyHeaders()
            setBody(buildJsonObject {
                put("idempotency_key", "brand-${System.currentTimeMillis()}")
                put("brand", buildJsonObject {
                    put("name", name)
                    put("reference_id", referenceId)
                })
            }.toString())
        }
    }

    suspend fun createMerchant(brandId: String, name: String, referenceId: String): HttpResponse {
        return httpClient.post("$networkBaseUrl/merchants") {
            applyHeaders()
            setBody(buildJsonObject {
                put("idempotency_key", "merchant-${System.currentTimeMillis()}")
                put("merchant", buildJsonObject {
                    put("name", name)
                    put("brand_id", brandId)
                    put("country", "US")
                    put("currency", "USD")
                    put("category", "5411")
                    put("reference_id", referenceId)
                    put("address", buildJsonObject {
                        put("address_line_1", "255 King Street")
                        put("locality", "San Francisco")
                        put("administrative_district_level_1", "CA")
                        put("postal_code", "94107")
                        put("country", "US")
                    })
                })
            }.toString())
        }
    }

    suspend fun createPayment(grantId: String, amountCents: Long, idempotencyKey: String): HttpResponse {
        return httpClient.post("$networkBaseUrl/payments") {
            applyHeaders()
            setBody(buildJsonObject {
                put("idempotency_key", idempotencyKey)
                put("payment", buildJsonObject {
                    put("amount", amountCents)
                    put("currency", "USD")
                    put("merchant_id", merchantId)
                    put("grant_id", grantId)
                    put("capture", true)
                    put("reference_id", "order-${System.currentTimeMillis()}")
                })
            }.toString())
        }
    }
}
