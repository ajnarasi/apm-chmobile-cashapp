package com.fiserv.payments.api.paypal

import com.fiserv.payments.api.payment.interfaces.PaymentMethod

data class PayPal(
    val orderId: String? = null
) : PaymentMethod {
    override fun toJson(): Any = mapOf("orderId" to orderId)
}
