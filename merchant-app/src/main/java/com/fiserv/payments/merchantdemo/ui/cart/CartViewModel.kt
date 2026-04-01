package com.fiserv.payments.merchantdemo.ui.cart

import androidx.lifecycle.ViewModel
import com.fiserv.payments.merchantdemo.data.CartItem
import com.fiserv.payments.merchantdemo.data.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CartViewModel : ViewModel() {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val subtotalCents: Long
        get() = _cartItems.value.sumOf { it.totalCents }

    val taxCents: Long
        get() = (subtotalCents * 0.085).toLong()

    val totalCents: Long
        get() = subtotalCents + taxCents

    val totalDollars: Double
        get() = totalCents / 100.0

    fun addToCart(product: Product) {
        val current = _cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            current[existingIndex] = current[existingIndex].copy(
                quantity = current[existingIndex].quantity + 1
            )
        } else {
            current.add(CartItem(product = product))
        }
        _cartItems.value = current
    }

    fun removeFromCart(productId: String) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun updateQuantity(productId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(productId)
            return
        }
        _cartItems.value = _cartItems.value.map { item ->
            if (item.product.id == productId) item.copy(quantity = newQuantity) else item
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun formatCents(cents: Long): String {
        val dollars = cents / 100
        val remainder = cents % 100
        return "$${"%,d".format(dollars)}.${"%02d".format(remainder)}"
    }
}
