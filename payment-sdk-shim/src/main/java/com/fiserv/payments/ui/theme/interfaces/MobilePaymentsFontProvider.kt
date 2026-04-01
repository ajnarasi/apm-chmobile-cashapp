package com.fiserv.payments.ui.theme.interfaces

import androidx.compose.ui.text.font.FontFamily

interface MobilePaymentsFontProvider {
    fun getHeaderFont(): FontFamily = FontFamily.Default
    fun getBodyFont(): FontFamily = FontFamily.Default
}
