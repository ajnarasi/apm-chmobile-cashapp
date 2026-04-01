package com.fiserv.payments.cashapppay

import app.cash.paykit.core.CashAppPay
import app.cash.paykit.core.CashAppPayFactory
import app.cash.paykit.core.CashAppPayListener
import app.cash.paykit.core.CashAppPayState
import app.cash.paykit.core.models.sdk.CashAppPayCurrency
import app.cash.paykit.core.models.sdk.CashAppPayPaymentAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wraps the Cash App Pay Kit SDK and translates its callback-driven state machine
 * into a Kotlin [StateFlow] of [CashAppPayFlowState].
 *
 * Lifecycle:
 *  1. [startPayment] -- creates a one-time customer request.
 *  2. SDK transitions to ReadyToAuthorize -- we auto-invoke [CashAppPay.authorizeCustomerRequest].
 *  3. SDK transitions to Approved / Declined -- we surface the result.
 *
 * @param identity Merchant credentials and redirect configuration.
 */
class CashAppPayAuthorizer(
    private val identity: CashAppPayIdentity
) : CashAppPayListener {

    private val cashAppPay: CashAppPay = if (identity.isSandbox) {
        CashAppPayFactory.createSandbox(identity.clientId)
    } else {
        CashAppPayFactory.create(identity.clientId)
    }

    private val _state = MutableStateFlow<CashAppPayFlowState>(CashAppPayFlowState.NotStarted)

    /** Observable flow of the current payment-flow state. */
    val state: StateFlow<CashAppPayFlowState> = _state.asStateFlow()

    init {
        cashAppPay.registerForStateUpdates(this)
    }

    // ---- public API ---------------------------------------------------------

    /**
     * Kick off a new one-time payment for [amountCents] (USD).
     */
    fun startPayment(amountCents: Long) {
        _state.value = CashAppPayFlowState.CreatingCustomerRequest

        val action = CashAppPayPaymentAction.OneTimeAction(
            currency = CashAppPayCurrency.USD,
            amount = amountCents.toInt(),
            scopeId = identity.scopeId
        )
        cashAppPay.createCustomerRequest(action, identity.redirectUri)
    }

    /**
     * Reset the authorizer to its idle state so a new payment can be started.
     */
    fun reset() {
        _state.value = CashAppPayFlowState.NotStarted
    }

    /**
     * Unregister from SDK state updates. Call when the owning component is destroyed.
     */
    fun destroy() {
        cashAppPay.unregisterFromStateUpdates()
    }

    // ---- CashAppPayListener -------------------------------------------------

    override fun cashAppPayStateDidChange(newState: CashAppPayState) {
        when (newState) {
            is CashAppPayState.ReadyToAuthorize -> {
                _state.value = CashAppPayFlowState.ReadyToAuthorize
                // Immediately hand off to Cash App for authorization.
                cashAppPay.authorizeCustomerRequest()
                _state.value = CashAppPayFlowState.Authorizing
            }

            is CashAppPayState.Approved -> {
                val grant = newState.responseData.grants?.firstOrNull()
                if (grant != null) {
                    _state.value = CashAppPayFlowState.Approved(
                        grantId = grant.id,
                        customerId = grant.customerId,
                        cashTag = newState.responseData.customerProfile?.cashTag?.toString()
                    )
                } else {
                    _state.value = CashAppPayFlowState.Error(
                        message = "Approved but no grants received from Cash App."
                    )
                }
            }

            is CashAppPayState.Declined -> {
                _state.value = CashAppPayFlowState.Declined
            }

            is CashAppPayState.CashAppPayExceptionState -> {
                _state.value = CashAppPayFlowState.Error(
                    message = newState.exception.message ?: "Unknown Cash App Pay error",
                    cause = newState.exception
                )
            }

            else -> {
                // Other transient SDK states (e.g. Creating, UpdatingCustomerRequest)
                // are handled implicitly by CreatingCustomerRequest above.
            }
        }
    }
}
