package com.fiserv.payments.cashapppay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel that orchestrates the entire Cash App Pay payment lifecycle:
 *
 *  1. Initiates a payment via [CashAppPayAuthorizer].
 *  2. Persists in-flight state via [CashAppPayStatePersistence].
 *  3. Captures the payment via [CashAppPayCaptureClient] once authorized.
 *  4. Exposes a single [flowState] for the UI to observe.
 *
 * Uses [AndroidViewModel] because DataStore requires an [Application] context.
 */
class CashAppPayViewModel(application: Application) : AndroidViewModel(application) {

    private var authorizer: CashAppPayAuthorizer? = null
    private var captureClient: CashAppPayCaptureClient? = null
    private val statePersistence = CashAppPayStatePersistence(application)

    private val _flowState = MutableStateFlow<CashAppPayFlowState>(CashAppPayFlowState.NotStarted)

    /** Observable state for the Compose UI layer. */
    val flowState: StateFlow<CashAppPayFlowState> = _flowState.asStateFlow()

    /** Current idempotency key for the in-flight payment, if any. */
    private var currentIdempotencyKey: String? = null

    /** Current amount (cents) for the in-flight payment. */
    private var currentAmountCents: Long = 0L

    // ---- Initialization -----------------------------------------------------

    /**
     * Initialize (or re-initialize) the bridge with the given credentials and backend config.
     * Must be called before [startCashAppPayment].
     */
    fun initialize(identity: CashAppPayIdentity, infraConfig: CashAppPayInfraConfig = CashAppPayInfraConfig()) {
        // Tear down any previous authorizer.
        authorizer?.destroy()

        val newAuthorizer = CashAppPayAuthorizer(identity)
        authorizer = newAuthorizer
        captureClient = CashAppPayCaptureClient(infraConfig)

        // Collect authorizer state changes and react accordingly.
        viewModelScope.launch {
            newAuthorizer.state.collect { authState ->
                when (authState) {
                    is CashAppPayFlowState.Approved -> onApproved(authState)
                    else -> _flowState.value = authState
                }
            }
        }
    }

    // ---- Public actions -----------------------------------------------------

    /**
     * Start a Cash App Pay payment for the given dollar amount.
     *
     * @param amountDollars Payment amount in dollars (e.g. 12.50).
     */
    fun startCashAppPayment(amountDollars: Double) {
        val amountCents = (amountDollars * 100).toLong()
        val idempotencyKey = UUID.randomUUID().toString()

        currentIdempotencyKey = idempotencyKey
        currentAmountCents = amountCents

        viewModelScope.launch {
            statePersistence.savePreAuth(idempotencyKey, amountCents)
            authorizer?.startPayment(amountCents)
                ?: run {
                    _flowState.value = CashAppPayFlowState.Error(
                        message = "CashAppPayViewModel not initialized. Call initialize() first."
                    )
                }
        }
    }

    /**
     * Attempt to restore and resume a payment that was interrupted by process death.
     *
     * If a persisted grant ID exists the bridge will skip straight to capture.
     */
    fun restoreFromProcessDeath() {
        viewModelScope.launch {
            val persisted = statePersistence.loadPersistedState() ?: return@launch

            currentIdempotencyKey = persisted.idempotencyKey
            currentAmountCents = persisted.amountCents

            if (persisted.grantId != null) {
                // Authorization was already approved before the process died -- resume capture.
                capturePayment(persisted.grantId, persisted.amountCents, persisted.idempotencyKey)
            }
            // If grantId is null the authorization was not yet complete.
            // The user will need to start a new flow.
        }
    }

    /**
     * Reset all state so a new payment flow can begin.
     */
    fun reset() {
        authorizer?.reset()
        _flowState.value = CashAppPayFlowState.NotStarted
        currentIdempotencyKey = null
        currentAmountCents = 0L

        viewModelScope.launch {
            statePersistence.clear()
        }
    }

    // ---- Internal -----------------------------------------------------------

    /**
     * Called when the authorizer transitions to [CashAppPayFlowState.Approved].
     * Persists the grant ID and initiates the backend capture call.
     */
    private fun onApproved(approved: CashAppPayFlowState.Approved) {
        viewModelScope.launch {
            statePersistence.saveGrantId(approved.grantId)
            capturePayment(approved.grantId, currentAmountCents, currentIdempotencyKey!!)
        }
    }

    /**
     * Calls the backend to capture (finalize) the authorized payment.
     */
    private suspend fun capturePayment(grantId: String, amountCents: Long, idempotencyKey: String) {
        _flowState.value = CashAppPayFlowState.CapturingPayment

        val client = captureClient ?: run {
            _flowState.value = CashAppPayFlowState.Error(
                message = "CaptureClient not initialized. Call initialize() first."
            )
            return
        }

        val result = client.capturePayment(grantId, amountCents, idempotencyKey)

        result.fold(
            onSuccess = { response ->
                _flowState.value = CashAppPayFlowState.PaymentComplete(
                    transactionId = response.paymentId,
                    amount = response.amount
                )
                statePersistence.clear()
            },
            onFailure = { throwable ->
                _flowState.value = CashAppPayFlowState.Error(
                    message = throwable.message ?: "Payment capture failed",
                    cause = throwable,
                    retryAction = {
                        viewModelScope.launch {
                            capturePayment(grantId, amountCents, idempotencyKey)
                        }
                    }
                )
            }
        )
    }

    // ---- Cleanup ------------------------------------------------------------

    override fun onCleared() {
        super.onCleared()
        authorizer?.destroy()
    }
}
