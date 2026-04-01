package com.fiserv.payments.api.core

import android.app.Application
import com.fiserv.payments.api.http.data.Environment

object MobilePayments {
    private var customerId: String? = null
    private var businessLocationId: String? = null

    fun initialize(
        application: Application,
        environment: Environment,
        clientToken: String,
        businessLocationId: String? = null
    ) {
        this.businessLocationId = businessLocationId
        android.util.Log.i("MobilePayments", "SDK initialized [shim] env=$environment")
    }

    fun setCustomerId(customerId: String?) { this.customerId = customerId }
    fun setBusinessLocationId(id: String?) { this.businessLocationId = id }
    fun setGooglePayEnabled(enabled: Boolean) { }
}
