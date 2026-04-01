package com.fiserv.payments.cashapppay.server

import com.fiserv.payments.cashapppay.server.client.CashAppSandboxClient
import com.fiserv.payments.cashapppay.server.routes.onboardingRoutes
import com.fiserv.payments.cashapppay.server.routes.paymentRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        configureRoutes()
    }.start(wait = true)
}

fun Application.configureRoutes() {
    val clientId = System.getenv("CASHAPP_CLIENT_ID") ?: "CAS-CI_FISERV_TEST"
    val apiKey = System.getenv("CASHAPP_API_KEY") ?: "KEY_ksbja4hqrgtahqmw6nn5gyv1b"
    val sandboxClient = CashAppSandboxClient(clientId, apiKey)

    routing {
        get("/api/health") {
            call.respond(mapOf("status" to "ok", "service" to "cashapp-pay-sandbox-proxy"))
        }
        onboardingRoutes(sandboxClient)
        paymentRoutes(sandboxClient)
    }
}
