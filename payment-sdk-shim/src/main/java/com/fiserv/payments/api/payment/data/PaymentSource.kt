package com.fiserv.payments.api.payment.data

import com.fiserv.payments.api.creditcard.CreditCard
import com.fiserv.payments.api.googlepay.GooglePay

data class PaymentSource(
    val paymentMethod: String? = null,
    val creditCard: CreditCard? = null,
    val googlePay: GooglePay? = null
)
