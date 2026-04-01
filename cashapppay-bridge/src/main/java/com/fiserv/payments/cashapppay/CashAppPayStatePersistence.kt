package com.fiserv.payments.cashapppay

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Extension property that lazily creates a single DataStore instance per [Context]. */
private val Context.cashAppPayDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cash_app_pay_state"
)

/**
 * Persists in-flight payment state to AndroidX DataStore so that an authorized
 * payment can survive process death and still be captured on restart.
 *
 * @param context Application [Context] used to obtain the DataStore instance.
 */
class CashAppPayStatePersistence(context: Context) {

    private val dataStore: DataStore<Preferences> = context.cashAppPayDataStore

    // ---- DataStore keys -----------------------------------------------------

    private companion object Keys {
        val IDEMPOTENCY_KEY = stringPreferencesKey("idempotency_key")
        val GRANT_ID = stringPreferencesKey("grant_id")
        val AMOUNT_CENTS = longPreferencesKey("amount_cents")
        val CUSTOMER_REQUEST_ID = stringPreferencesKey("customer_request_id")
    }

    // ---- Persisted snapshot -------------------------------------------------

    /**
     * Snapshot of the payment state that was persisted to disk.
     *
     * @param idempotencyKey Unique key for the payment attempt.
     * @param grantId        Cash App grant ID; null if authorization has not yet completed.
     * @param amountCents    Payment amount in cents.
     */
    data class PersistedPaymentState(
        val idempotencyKey: String,
        val grantId: String?,
        val amountCents: Long
    )

    // ---- Write operations ---------------------------------------------------

    /**
     * Persist the initial pre-authorization state.
     *
     * Call this just before [CashAppPayAuthorizer.startPayment] so that we know
     * the intended amount and idempotency key even if the process dies during
     * authorization.
     */
    suspend fun savePreAuth(idempotencyKey: String, amountCents: Long) {
        dataStore.edit { prefs ->
            prefs[IDEMPOTENCY_KEY] = idempotencyKey
            prefs[AMOUNT_CENTS] = amountCents
            prefs.remove(GRANT_ID)
            prefs.remove(CUSTOMER_REQUEST_ID)
        }
    }

    /**
     * Persist the grant ID once the user has approved the payment in Cash App.
     */
    suspend fun saveGrantId(grantId: String) {
        dataStore.edit { prefs ->
            prefs[GRANT_ID] = grantId
        }
    }

    // ---- Read operations ----------------------------------------------------

    /**
     * Load any previously persisted in-flight payment state.
     *
     * @return [PersistedPaymentState] if a valid snapshot exists, or null otherwise.
     */
    suspend fun loadPersistedState(): PersistedPaymentState? {
        return dataStore.data.map { prefs ->
            val idempotencyKey = prefs[IDEMPOTENCY_KEY] ?: return@map null
            val amountCents = prefs[AMOUNT_CENTS] ?: return@map null
            PersistedPaymentState(
                idempotencyKey = idempotencyKey,
                grantId = prefs[GRANT_ID],
                amountCents = amountCents
            )
        }.first()
    }

    // ---- Cleanup ------------------------------------------------------------

    /**
     * Clear all persisted payment state. Call after a successful capture or explicit reset.
     */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
