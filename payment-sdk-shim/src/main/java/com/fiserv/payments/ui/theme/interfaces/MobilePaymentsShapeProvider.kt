package com.fiserv.payments.ui.theme.interfaces

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

interface MobilePaymentsShapeProvider {
    fun getButtonCornerRadius(): Dp = 8.dp
    fun getCornerRadius(): Dp = 8.dp
    fun getTextFieldCornerRadius(): Dp = 4.dp
    fun getSelectedBorderThickness(): Dp = 2.dp
    fun getBorderThickness(): Dp = 1.dp
}
