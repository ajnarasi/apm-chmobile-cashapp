package com.fiserv.payments.merchantdemo.ui.catalog

import androidx.lifecycle.ViewModel
import com.fiserv.payments.merchantdemo.data.Product
import com.fiserv.payments.merchantdemo.data.SampleProducts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CatalogViewModel : ViewModel() {
    private val _products = MutableStateFlow(SampleProducts.products)
    val products: StateFlow<List<Product>> = _products.asStateFlow()
}
