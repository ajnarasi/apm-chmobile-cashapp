package com.fiserv.payments.merchantdemo.ui.checkout

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiserv.payments.cashapppay.CashAppPayFlowState
import com.fiserv.payments.cashapppay.CashAppPayViewModel
import com.fiserv.payments.cashapppay.klarna.KlarnaFlowState
import com.fiserv.payments.cashapppay.klarna.KlarnaViewModel
import com.fiserv.payments.cashapppay.ui.CashAppPayButton
import com.fiserv.payments.merchantdemo.MerchantApplication
import com.fiserv.payments.merchantdemo.ui.cart.CartViewModel
import com.fiserv.payments.merchantdemo.ui.theme.CashAppGreen
import com.fiserv.payments.merchantdemo.ui.theme.FiservOrange

private val KlarnaPink = Color(0xFFFFB3C7)
private val KlarnaDark = Color(0xFF0A0B09)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartViewModel: CartViewModel,
    onPaymentComplete: (transactionId: String, amount: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MerchantApplication

    // --- Cash App Pay ---
    val cashAppPayViewModel: CashAppPayViewModel = viewModel()
    val cashAppPayState by cashAppPayViewModel.flowState.collectAsState()

    // --- Klarna ---
    val klarnaViewModel: KlarnaViewModel = viewModel()
    val klarnaState by klarnaViewModel.flowState.collectAsState()
    var selectedKlarnaType by remember { mutableStateOf("klarna") }

    // Initialize both payment SDKs
    LaunchedEffect(Unit) {
        cashAppPayViewModel.initialize(
            identity = app.cashAppPayIdentity,
            infraConfig = app.cashAppPayInfraConfig
        )
        cashAppPayViewModel.restoreFromProcessDeath()
        klarnaViewModel.initialize(app.cashAppPayInfraConfig)
    }

    // Handle Cash App Pay completion
    LaunchedEffect(cashAppPayState) {
        when (val state = cashAppPayState) {
            is CashAppPayFlowState.PaymentComplete -> {
                onPaymentComplete(state.transactionId, cartViewModel.formatCents(state.amount))
                cartViewModel.clearCart()
                cashAppPayViewModel.reset()
            }
            else -> {}
        }
    }

    // Handle Klarna completion
    LaunchedEffect(klarnaState) {
        when (val state = klarnaState) {
            is KlarnaFlowState.OrderCreated -> {
                onPaymentComplete(state.transactionId, cartViewModel.formatCents(state.amount))
                cartViewModel.clearCart()
                klarnaViewModel.reset()
            }
            else -> {}
        }
    }

    val isCashAppProcessing = cashAppPayState is CashAppPayFlowState.CreatingCustomerRequest
            || cashAppPayState is CashAppPayFlowState.Authorizing
            || cashAppPayState is CashAppPayFlowState.CapturingPayment

    val isKlarnaProcessing = klarnaState is KlarnaFlowState.WidgetLoading
            || klarnaState is KlarnaFlowState.Authorizing
            || klarnaState is KlarnaFlowState.SessionCreated

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Sandbox badge
            Surface(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "\uD83E\uDDEA  Sandbox Environment \u2014 No real charges",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE65100),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Order summary
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Order Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    SummaryLine("Subtotal", cartViewModel.formatCents(cartViewModel.subtotalCents))
                    SummaryLine("Tax (8.5%)", cartViewModel.formatCents(cartViewModel.taxCents), subtle = true)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            cartViewModel.formatCents(cartViewModel.totalCents),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = FiservOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ========================================
            // PAYMENT METHOD 1: CASH APP PAY (Hero)
            // ========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FFF0))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Recommended", style = MaterialTheme.typography.labelSmall, color = CashAppGreen, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    CashAppPayButton(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isCashAppProcessing && cashAppPayState !is CashAppPayFlowState.Approved,
                        isLoading = isCashAppProcessing,
                        onClick = { cashAppPayViewModel.startCashAppPayment(cartViewModel.totalDollars) }
                    )

                    AnimatedVisibility(visible = isCashAppProcessing || cashAppPayState is CashAppPayFlowState.Error || cashAppPayState is CashAppPayFlowState.Declined) {
                        Column(modifier = Modifier.padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            when (cashAppPayState) {
                                is CashAppPayFlowState.CreatingCustomerRequest -> StatusChip("Preparing payment request...", Color(0xFF1565C0))
                                is CashAppPayFlowState.Authorizing -> StatusChip("Waiting for Cash App authorization...", CashAppGreen)
                                is CashAppPayFlowState.CapturingPayment -> StatusChip("Capturing payment...", FiservOrange)
                                is CashAppPayFlowState.Error -> {
                                    val state = cashAppPayState as CashAppPayFlowState.Error
                                    Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                    val retry = state.retryAction
                                    if (retry != null) { TextButton(onClick = { retry.invoke() }) { Text("Retry Payment") } }
                                }
                                is CashAppPayFlowState.Declined -> Text("Payment declined.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                else -> {}
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ========================================
            // PAYMENT METHOD 2: KLARNA
            // Uses real Klarna Mobile SDK (com.klarna.mobile:sdk:2.11.1)
            // Flow: createSession → KlarnaPaymentBridge → authorize → capture
            // ========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F7))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Klarna.",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = KlarnaDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Payment type selector (mirrors Klarna's pay_now/pay_later/pay_over_time categories)
                    val klarnaTypes = listOf(
                        "klarna" to "Pay with Klarna",
                        "pay_over_time" to "Pay in 4 installments",
                        "pay_later" to "Pay in 30 days"
                    )
                    klarnaTypes.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = selectedKlarnaType == value, onClick = { selectedKlarnaType = value })
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedKlarnaType == value, onClick = { selectedKlarnaType = value })
                            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Klarna pay button
                    Button(
                        onClick = { klarnaViewModel.startKlarnaPayment(cartViewModel.totalDollars, selectedKlarnaType) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isKlarnaProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = KlarnaDark)
                    ) {
                        if (isKlarnaProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing...", color = Color.White)
                        } else {
                            Text("Pay with Klarna", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }

                    // Klarna status messages
                    AnimatedVisibility(visible = isKlarnaProcessing || klarnaState is KlarnaFlowState.Error || klarnaState is KlarnaFlowState.Declined) {
                        Column(modifier = Modifier.padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            when (klarnaState) {
                                is KlarnaFlowState.WidgetLoading -> StatusChip("Creating Klarna session...", KlarnaPink)
                                is KlarnaFlowState.SessionCreated -> StatusChip("Klarna session ready", KlarnaPink)
                                is KlarnaFlowState.Authorizing -> StatusChip("Authorizing with Klarna...", KlarnaPink)
                                is KlarnaFlowState.Error -> {
                                    val state = klarnaState as KlarnaFlowState.Error
                                    Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                                is KlarnaFlowState.Declined -> Text("Klarna payment declined.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                else -> {}
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f))
                Text("  or pay with  ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PAYMENT METHOD 3: Credit Card (CardFree SDK)
            OutlinedButton(
                onClick = { /* CardFree PurchaseButton integration */ },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Text("\uD83D\uDCB3  Credit / Debit Card", fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PAYMENT METHOD 4: Google Pay (CardFree SDK)
            OutlinedButton(
                onClick = { /* Google Pay integration */ },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Text("G Pay  Google Pay", fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "All payment methods powered by Fiserv CommerceHub + Cardfree",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, subtle: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (subtle) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
        Text(value, color = if (subtle) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = color)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}
