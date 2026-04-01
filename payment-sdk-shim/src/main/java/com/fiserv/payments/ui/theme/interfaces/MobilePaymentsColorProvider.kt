package com.fiserv.payments.ui.theme.interfaces

import androidx.compose.ui.graphics.Color

interface MobilePaymentsColorProvider {
    fun getPrimary(): Color = Color(0xFF1A73E8)
    fun getHighlight(): Color = Color(0xFF4285F4)
    fun getDisabled(): Color = Color(0xFFBDBDBD)
    fun getDisabledText(): Color = Color(0xFF9E9E9E)
    fun getSuccess(): Color = Color(0xFF34A853)
    fun getError(): Color = Color(0xFFEA4335)
    fun getDarkText(): Color = Color(0xFF202124)
    fun getMediumText(): Color = Color(0xFF5F6368)
    fun getLightText(): Color = Color(0xFFFFFFFF)
    fun getLightBackground(): Color = Color(0xFFF8F9FA)
    fun getBackground(): Color = Color(0xFFFFFFFF)
}
