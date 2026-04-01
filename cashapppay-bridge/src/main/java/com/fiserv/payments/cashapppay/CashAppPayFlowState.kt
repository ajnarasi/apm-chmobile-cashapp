package com.fiserv.payments.cashapppay

/**
 * Sealed state hierarchy representing every stage of a Cash App Pay payment flow.
 *
 * The bridge ViewModel emits these states so the UI layer can react declaratively.
 */
sealed class CashAppPayFlowState {

    /** Initial idle state -- no payment flow in progress. */
    data object NotStarted : CashAppPayFlowState()

    /** A customer-request is being created with the Cash App Pay SDK. */
    data object CreatingCustomerRequest : CashAppPayFlowState()

    /** The SDK signals it is ready for the user to authorize in the Cash App. */
    data object ReadyToAuthorize : CashAppPayFlowState()

    /** The authorization hand-off to Cash App is in progress. */
    data object Authorizing : CashAppPayFlowState()

    /**
     * The user approved the payment in Cash App.
     *
     * @param grantId    One-time grant returned by Cash App.
     * @param customerId Cash App customer identifier (may be null).
     * @param cashTag    The user's $cashtag (may be null).
     */
    data class Approved(
        val grantId: String,
        val customerId: String?,
        val cashTag: String?
    ) : CashAppPayFlowState()

    /** The user declined or dismissed the authorization prompt. */
    data object Declined : CashAppPayFlowState()

    /** The bridge is calling the backend to capture the authorized payment. */
    data object CapturingPayment : CashAppPayFlowState()

    /**
     * Terminal success state -- the backend confirmed payment capture.
     *
     * @param transactionId Backend-assigned transaction identifier.
     * @param amount        Captured amount in cents.
     */
    data class PaymentComplete(
        val transactionId: String,
        val amount: Long
    ) : CashAppPayFlowState()

    /**
     * An error occurred at any point in the flow.
     *
     * @param message     Human-readable error description.
     * @param cause       Optional underlying throwable.
     * @param retryAction Optional lambda that the UI can invoke to retry the failed step.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val retryAction: (() -> Unit)? = null
    ) : CashAppPayFlowState()
}
