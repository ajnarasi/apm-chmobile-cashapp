package com.fiserv.payments.merchantdemo.ui.confirmation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiserv.payments.merchantdemo.ui.theme.CashAppGreen
import com.fiserv.payments.merchantdemo.ui.theme.FiservOrange

@Composable
fun ConfirmationScreen(
    transactionId: String,
    amount: String,
    onBackToCatalog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF0FFF0), Color.White),
                    startY = 0f,
                    endY = 600f
                )
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(CashAppGreen),
            contentAlignment = Alignment.Center
        ) {
            Text("\u2713", fontSize = 40.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Payment Successful!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Your order has been confirmed",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DetailRow("Amount Charged", amount, highlight = true)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                DetailRow("Payment Method", "Cash App Pay")
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Transaction ID", transactionId)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Status", "Completed")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = Color(0xFFFFF3E0),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "\uD83E\uDDEA Sandbox transaction \u2014 no real funds moved",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE65100),
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBackToCatalog,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FiservOrange)
        ) {
            Text("Continue Shopping", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, highlight: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value,
            style = if (highlight) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) FiservOrange else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
