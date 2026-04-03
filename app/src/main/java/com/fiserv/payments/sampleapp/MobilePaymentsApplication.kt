package com.fiserv.payments.sampleapp

import android.app.Application
import com.fiserv.payments.api.core.MobilePayments
import com.fiserv.payments.api.http.data.Environment

class MobilePaymentsApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        MobilePayments.initialize(
            application = this,
            environment = Environment.SANDBOX,
            clientToken = "ec4b3f05035f4ab594ea14471885908d",
            businessLocationId = "10000001",
        )
    }
}