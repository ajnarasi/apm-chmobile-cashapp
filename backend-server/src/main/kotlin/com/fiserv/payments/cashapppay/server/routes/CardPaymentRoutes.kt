package com.fiserv.payments.cashapppay.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class TokenizeCardRequest(
    val cardNumber: String,
    val expiryMonth: String,
    val expiryYear: String,
    val cvv: String,
    val cardholderName: String,
    val postalCode: String = ""
)

@Serializable
data class TokenizeCardResponse(
    val token: String,
    val lastFour: String,
    val cardType: String,
    val expiryMonth: String,
    val expiryYear: String
)

@Serializable
data class CardPaymentRequest(
    val amount: Double,
    val token: String,
    val transactionType: String = "SALE",
    val cardType: String = "",
    val lastFour: String = ""
)

@Serializable
data class CardPaymentResponse(
    val transactionId: String,
    val amount: Double,
    val status: String,
    val cardType: String,
    val lastFour: String,
    val paymentMethod: String
)

@Serializable
data class GooglePayRequest(
    val amount: Double,
    val walletToken: String
)

fun Route.cardPaymentRoutes() {
    // Simulate card tokenization (what CreditCardManager.addCreditCard does)
    post("/api/card/tokenize") {
        val request = call.receive<TokenizeCardRequest>()

        val lastFour = request.cardNumber.takeLast(4)
        val cardType = when {
            request.cardNumber.startsWith("4") -> "VISA"
            request.cardNumber.startsWith("5") -> "MASTERCARD"
            request.cardNumber.startsWith("3") -> "AMEX"
            request.cardNumber.startsWith("6") -> "DISCOVER"
            else -> "UNKNOWN"
        }

        // Simulate tokenization delay
        kotlinx.coroutines.delay(500)

        call.respond(TokenizeCardResponse(
            token = "tok_${System.currentTimeMillis()}_$lastFour",
            lastFour = lastFour,
            cardType = cardType,
            expiryMonth = request.expiryMonth,
            expiryYear = request.expiryYear
        ))
    }

    // Simulate card payment (what PaymentManager.sale does)
    post("/api/card/payment") {
        val request = call.receive<CardPaymentRequest>()

        // Simulate payment processing delay
        kotlinx.coroutines.delay(800)

        // Simulate decline for specific amounts (matching CardFree SDK behavior)
        if (request.amount == 66.70) {
            call.respond(HttpStatusCode.BadRequest, mapOf(
                "error" to "DECLINED",
                "message" to "Card declined - insufficient funds"
            ))
            return@post
        }

        call.respond(CardPaymentResponse(
            transactionId = "TXN_CARD_${System.currentTimeMillis()}",
            amount = request.amount,
            status = "CAPTURED",
            cardType = request.cardType,
            lastFour = request.lastFour,
            paymentMethod = "CREDIT_CARD"
        ))
    }

    // Simulate Google Pay payment (what GooglePay + PaymentManager.sale does)
    post("/api/googlepay/payment") {
        val request = call.receive<GooglePayRequest>()

        // Simulate processing delay
        kotlinx.coroutines.delay(600)

        call.respond(CardPaymentResponse(
            transactionId = "TXN_GPAY_${System.currentTimeMillis()}",
            amount = request.amount,
            status = "CAPTURED",
            cardType = "GOOGLE_PAY",
            lastFour = "****",
            paymentMethod = "GOOGLE_PAY"
        ))
    }
}
