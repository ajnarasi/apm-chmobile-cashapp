package com.fiserv.payments.merchantdemo.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.ui.Modifier
import com.fiserv.payments.merchantdemo.ui.cart.CartScreen
import com.fiserv.payments.merchantdemo.ui.cart.CartViewModel
import com.fiserv.payments.merchantdemo.ui.catalog.CatalogScreen
import com.fiserv.payments.merchantdemo.ui.checkout.CheckoutScreen
import com.fiserv.payments.merchantdemo.ui.confirmation.ConfirmationScreen

@Composable
fun MerchantNavGraph(
    navController: NavHostController,
    cartViewModel: CartViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = "catalog", modifier = modifier) {
        composable("catalog") {
            CatalogScreen(
                cartViewModel = cartViewModel,
                onViewCart = { navController.navigate("cart") }
            )
        }
        composable("cart") {
            CartScreen(
                cartViewModel = cartViewModel,
                onCheckout = { navController.navigate("checkout") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("checkout") {
            CheckoutScreen(
                cartViewModel = cartViewModel,
                onPaymentComplete = { txnId, amount ->
                    navController.navigate("confirmation/$txnId/$amount") {
                        popUpTo("catalog") { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "confirmation/{transactionId}/{amount}",
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType },
                navArgument("amount") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ConfirmationScreen(
                transactionId = backStackEntry.arguments?.getString("transactionId") ?: "",
                amount = backStackEntry.arguments?.getString("amount") ?: "",
                onBackToCatalog = {
                    navController.navigate("catalog") {
                        popUpTo("catalog") { inclusive = true }
                    }
                }
            )
        }
    }
}
