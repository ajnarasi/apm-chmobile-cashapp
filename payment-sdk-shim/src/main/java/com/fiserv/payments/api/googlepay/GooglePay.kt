package com.fiserv.payments.api.googlepay

import com.fiserv.payments.api.payment.interfaces.PaymentMethod

data class GooglePay(
    val walletToken: String? = null
) : PaymentMethod {
    override fun toJson(): Any = mapOf("walletToken" to walletToken)
}
