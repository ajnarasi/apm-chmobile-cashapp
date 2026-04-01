package com.fiserv.payments.cashapppay

/**
 * Identity credentials for Cash App Pay SDK initialization.
 *
 * @param clientId     CAS-CI_... identifier issued by Cash App (sandbox or production).
 * @param isSandbox    When true the SDK operates against the Cash App sandbox environment.
 * @param scopeId      Merchant scope ID registered with Cash App.
 * @param redirectUri  Deep-link URI the Cash App will redirect back to after authorization,
 *                     e.g. "merchantdemo://cashapppay/checkout".
 */
data class CashAppPayIdentity(
    val clientId: String,
    val isSandbox: Boolean = true,
    val scopeId: String,
    val redirectUri: String
)

/**
 * Infrastructure-level configuration for the bridge's own backend calls.
 *
 * @param backendBaseUrl Base URL of the payment-capture backend.
 *                       Defaults to the Android-emulator localhost alias.
 */
data class CashAppPayInfraConfig(
    val backendBaseUrl: String = "http://10.0.2.2:8080"
)
