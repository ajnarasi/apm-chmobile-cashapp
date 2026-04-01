package com.fiserv.payments.merchantdemo.ui.catalog

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiserv.payments.merchantdemo.data.Product
import com.fiserv.payments.merchantdemo.data.SampleProducts
import com.fiserv.payments.merchantdemo.ui.cart.CartViewModel
import com.fiserv.payments.merchantdemo.ui.theme.FiservOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    cartViewModel: CartViewModel,
    onViewCart: () -> Unit,
    catalogViewModel: CatalogViewModel = viewModel()
) {
    val products by catalogViewModel.products.collectAsState()
    val cartItems by cartViewModel.cartItems.collectAsState()
    val cartItemCount = cartItems.sumOf { it.quantity }
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredProducts = if (selectedCategory == "All") products
        else products.filter { it.category == selectedCategory }

    Scaffold(
        floatingActionButton = {
            if (cartItemCount > 0) {
                LargeFloatingActionButton(
                    onClick = onViewCart,
                    containerColor = FiservOrange,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = Color.White,
                                contentColor = FiservOrange
                            ) {
                                Text("$cartItemCount", fontWeight = FontWeight.Bold)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hero banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF6600), Color(0xFFFF8533))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Text(
                        text = "CommerceHub",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Powered by Fiserv",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Category chips
            LazyRow(
                modifier = Modifier.padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SampleProducts.categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FiservOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Product grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(filteredProducts) { product ->
                    ProductCard(
                        product = product,
                        onAddToCart = { cartViewModel.addToCart(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, onAddToCart: () -> Unit) {
    val gradientColors = when (product.category) {
        "Audio" -> listOf(Color(0xFFE8EAF6), Color(0xFFC5CAE9))
        "Charging" -> listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
        "Cases" -> listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2))
        "Displays" -> listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB))
        "Input" -> listOf(Color(0xFFF3E5F5), Color(0xFFE1BEE7))
        else -> listOf(Color(0xFFF5F5F5), Color(0xFFEEEEEE))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Product image area with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(brush = Brush.verticalGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = product.imageEmoji,
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = product.priceFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FiservOrange
                    )
                    FilledTonalButton(
                        onClick = onAddToCart,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
