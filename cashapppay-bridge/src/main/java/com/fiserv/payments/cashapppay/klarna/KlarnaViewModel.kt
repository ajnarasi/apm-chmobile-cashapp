package com.fiserv.payments.cashapppay.klarna

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fiserv.payments.cashapppay.CashAppPayInfraConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.long
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Orchestrates the Klarna payment lifecycle:
 *   1. Create session (server-side via backend)
 *   2. Initialize KlarnaPaymentBridge with client_token
 *   3. Bridge handles load → authorize → callbacks
 *   4. On Authorized, call backend to create order
 *
 * Follows the same pattern as CashAppPayViewModel.
 */
class KlarnaViewModel(application: Application) : AndroidViewModel(application) {

    private val _flowState = MutableStateFlow<KlarnaFlowState>(KlarnaFlowState.NotStarted)
    val flowState: StateFlow<KlarnaFlowState> = _flowState.asStateFlow()

    private var bridge: KlarnaPaymentBridge? = null
    private var infraConfig: CashAppPayInfraConfig? = null
    private var currentAmountCents: Long = 0
    private val httpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    fun initialize(infraConfig: CashAppPayInfraConfig) {
        this.infraConfig = infraConfig
    }

    /**
     * Create a Klarna session via backend, then initialize the bridge.
     */
    fun createSession(amountCents: Long, category: String = "klarna") {
        viewModelScope.launch {
            _flowState.value = KlarnaFlowState.WidgetLoading
            currentAmountCents = amountCents
            try {
                val baseUrl = infraConfig?.backendBaseUrl ?: "http://10.0.2.2:8080"
                val body = """{"amountCents":$amountCents,"locale":"en-US","paymentMethodCategory":"$category"}"""
                val request = Request.Builder()
                    .url("$baseUrl/api/klarna/session")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
                val responseBody = response.body?.string() ?: throw Exception("Empty response")

                if (!response.isSuccessful) throw Exception("Session creation failed: $responseBody")

                val jsonObj = json.parseToJsonElement(responseBody)
                val sessionId = jsonObj.jsonObject["sessionId"]?.jsonPrimitive?.content ?: ""
                val clientToken = jsonObj.jsonObject["clientToken"]?.jsonPrimitive?.content ?: ""
                val categories = jsonObj.jsonObject["paymentMethodCategories"]?.jsonArray?.map {
                    it.jsonPrimitive.content
                } ?: listOf("klarna")

                _flowState.value = KlarnaFlowState.SessionCreated(clientToken, sessionId, categories)
            } catch (e: Exception) {
                _flowState.value = KlarnaFlowState.Error(message = e.message ?: "Session failed", cause = e)
            }
        }
    }

    /**
     * Create a bridge and start the native Klarna flow.
     * Call this after session is created and you have a container view.
     */
    fun createBridge(context: android.content.Context, category: String): KlarnaPaymentBridge? {
        val currentState = _flowState.value
        if (currentState !is KlarnaFlowState.SessionCreated) return null

        val newBridge = KlarnaPaymentBridge(context)
        bridge = newBridge

        // Collect bridge state changes
        viewModelScope.launch {
            newBridge.state.collect { bridgeState ->
                when (bridgeState) {
                    is KlarnaFlowState.Authorized -> capturePayment(bridgeState.authToken, category)
                    is KlarnaFlowState.Finalized -> capturePayment(bridgeState.authToken, category)
                    else -> _flowState.value = bridgeState
                }
            }
        }

        // Create the WebView with the Klarna payment widget
        newBridge.createWebView(currentState.clientToken, category)
        return newBridge
    }

    private fun capturePayment(authToken: String, category: String) {
        viewModelScope.launch {
            _flowState.value = KlarnaFlowState.Authorizing
            try {
                val baseUrl = infraConfig?.backendBaseUrl ?: "http://10.0.2.2:8080"
                val sessionState = _flowState.value
                val amountCents = currentAmountCents
                val body = """{"amountCents":$amountCents,"authorizationToken":"$authToken","paymentMethodCategory":"$category"}"""
                val request = Request.Builder()
                    .url("$baseUrl/api/klarna/payment")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
                val responseBody = response.body?.string() ?: throw Exception("Empty response")

                val jsonObj = json.parseToJsonElement(responseBody)
                _flowState.value = KlarnaFlowState.OrderCreated(
                    orderId = jsonObj.jsonObject["orderId"]?.jsonPrimitive?.content ?: "",
                    transactionId = jsonObj.jsonObject["transactionId"]?.jsonPrimitive?.content ?: "",
                    amount = jsonObj.jsonObject["amount"]?.jsonPrimitive?.long ?: 0,
                    paymentType = jsonObj.jsonObject["paymentType"]?.jsonPrimitive?.content ?: "Klarna"
                )
            } catch (e: Exception) {
                _flowState.value = KlarnaFlowState.Error(message = e.message ?: "Capture failed", cause = e)
            }
        }
    }

    fun reset() {
        bridge?.reset()
        _flowState.value = KlarnaFlowState.NotStarted
    }

    /**
     * Convenience method matching CashAppPayViewModel.startCashAppPayment() pattern.
     * Converts dollars to cents and kicks off session creation.
     */
    fun startKlarnaPayment(amountDollars: Double, category: String = "klarna") {
        val amountCents = (amountDollars * 100).toLong()
        createSession(amountCents, category)
    }

    override fun onCleared() {
        super.onCleared()
        bridge = null
    }
}
