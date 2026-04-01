package com.fiserv.payments.ui.theme

import com.fiserv.payments.ui.theme.interfaces.MobilePaymentsColorProvider
import com.fiserv.payments.ui.theme.interfaces.MobilePaymentsFontProvider
import com.fiserv.payments.ui.theme.interfaces.MobilePaymentsShapeProvider

object MobilePaymentsStyleProvider {
    var colors: MobilePaymentsColorProvider = object : MobilePaymentsColorProvider {}
    var fonts: MobilePaymentsFontProvider = object : MobilePaymentsFontProvider {}
    var shapes: MobilePaymentsShapeProvider = object : MobilePaymentsShapeProvider {}

    fun applyStyle() { }
}
