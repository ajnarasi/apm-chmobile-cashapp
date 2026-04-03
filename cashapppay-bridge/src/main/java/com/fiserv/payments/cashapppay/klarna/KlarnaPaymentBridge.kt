package com.fiserv.payments.cashapppay.klarna

import android.content.Context
import com.klarna.mobile.sdk.api.standalonewebview.KlarnaStandaloneWebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wraps the Klarna Android Mobile SDK (v2.11.1) using KlarnaStandaloneWebView.
 *
 * In SDK v2.11.1, the payments integration uses KlarnaStandaloneWebView which
 * hosts the Klarna Payments JS widget inside a WebView. This follows the same
 * init() → load() → authorize() flow as the Klarna Web SDK.
 *
 * The server-side session creation returns a client_token JWT that the
 * WebView loads via the Klarna JS SDK running inside it.
 *
 * Flow:
 *   1. Backend creates session → returns client_token
 *   2. createWebView(clientToken, category) → loads Klarna JS widget in WebView
 *   3. User interacts with Klarna widget inside the WebView
 *   4. Authorization handled via JS bridge communication
 */
class KlarnaPaymentBridge(
    private val context: Context
) {

    private val _state = MutableStateFlow<KlarnaFlowState>(KlarnaFlowState.NotStarted)
    val state: StateFlow<KlarnaFlowState> = _state.asStateFlow()

    private var webView: KlarnaStandaloneWebView? = null

    /**
     * Creates a KlarnaStandaloneWebView that loads the Klarna payment widget.
     * The widget is initialized with the client_token from session creation
     * and renders the specified payment method category.
     *
     * @param clientToken JWT from POST /api/klarna/session
     * @param category Payment method category (e.g., "klarna", "pay_later", "pay_over_time")
     * @return The KlarnaStandaloneWebView to embed in the Activity layout
     */
    fun createWebView(clientToken: String, category: String): KlarnaStandaloneWebView {
        _state.value = KlarnaFlowState.WidgetLoading

        val klarnaView = KlarnaStandaloneWebView(context)
        val html = buildKlarnaPaymentHtml(clientToken, category)
        klarnaView.loadData(html, "text/html", "UTF-8")

        webView = klarnaView
        _state.value = KlarnaFlowState.WidgetLoaded
        return klarnaView
    }

    /**
     * Builds HTML that loads the Klarna JS SDK and renders the payment widget.
     * Mirrors the web demo's Klarna.Payments.init() → load() → authorize() flow.
     */
    private fun buildKlarnaPaymentHtml(clientToken: String, category: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { margin: 0; padding: 16px; font-family: -apple-system, sans-serif; }
                #klarna-container { min-height: 200px; }
                .loading { text-align: center; padding: 40px; color: #666; }
                .error { color: #E53935; padding: 16px; text-align: center; }
            </style>
        </head>
        <body>
            <div id="klarna-container"><div class="loading">Loading Klarna...</div></div>
            <script>
                window.klarnaAsyncCallback = function() {
                    try {
                        Klarna.Payments.init({ client_token: '$clientToken' });
                        Klarna.Payments.load(
                            { container: '#klarna-container', payment_method_category: '$category' },
                            {},
                            function(res) {
                                if (res.show_form) {
                                    // Widget loaded successfully
                                } else if (res.error) {
                                    document.getElementById('klarna-container').innerHTML =
                                        '<div class="error">Klarna unavailable for this purchase</div>';
                                }
                            }
                        );
                    } catch(e) {
                        document.getElementById('klarna-container').innerHTML =
                            '<div class="error">' + e.message + '</div>';
                    }
                };
            </script>
            <script src="https://x.klarnacdn.net/kp/lib/v1/api.js" async></script>
        </body>
        </html>
        """.trimIndent()
    }

    fun reset() {
        _state.value = KlarnaFlowState.NotStarted
        webView = null
    }
}
