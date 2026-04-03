package com.fiserv.payments.cashapppay.klarna

sealed class KlarnaFlowState {
    data object NotStarted : KlarnaFlowState()
    data class SessionCreated(
        val clientToken: String,
        val sessionId: String,
        val categories: List<String>
    ) : KlarnaFlowState()
    data object WidgetLoading : KlarnaFlowState()
    data object WidgetLoaded : KlarnaFlowState()
    data object Authorizing : KlarnaFlowState()
    data class Authorized(val authToken: String) : KlarnaFlowState()
    data class FinalizeRequired(val authToken: String) : KlarnaFlowState()
    data class Finalized(val authToken: String) : KlarnaFlowState()
    data class OrderCreated(
        val orderId: String,
        val transactionId: String,
        val amount: Long,
        val paymentType: String
    ) : KlarnaFlowState()
    data object Declined : KlarnaFlowState()
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val retryAction: (() -> Unit)? = null
    ) : KlarnaFlowState()
}
