package com.fiserv.payments.merchantdemo

import android.app.Application
import com.fiserv.payments.api.core.MobilePayments
import com.fiserv.payments.api.http.data.Environment
import com.fiserv.payments.cashapppay.CashAppPayIdentity
import com.fiserv.payments.cashapppay.CashAppPayInfraConfig

class MerchantApplication : Application() {

    lateinit var cashAppPayIdentity: CashAppPayIdentity
        private set
    lateinit var cashAppPayInfraConfig: CashAppPayInfraConfig
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Fiserv Mobile Payments SDK
        MobilePayments.initialize(
            application = this,
            environment = Environment.SANDBOX,
            clientToken = "ec4b3f05035f4ab594ea14471885908d",
            businessLocationId = "10000001",
        )

        // Cash App Pay — real sandbox credentials
        cashAppPayIdentity = CashAppPayIdentity(
            clientId = "CAS-CI_FISERV_TEST",
            isSandbox = true,
            scopeId = "MERCHANT_SCOPE_ID",
            redirectUri = "merchantdemo://cashapppay/checkout"
        )
        cashAppPayInfraConfig = CashAppPayInfraConfig(
            backendBaseUrl = "http://10.0.2.2:8080"
        )
    }
}
