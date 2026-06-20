package com.example.testeableapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testeableapp.model.MenuData
import com.example.testeableapp.model.MenuItem
import com.example.testeableapp.ui.theme.TesteableAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TesteableAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RestaurantOrderApp(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel()
                    )
                }
            }
        }
    }
}

@Composable
fun RestaurantOrderApp(
    modifier: Modifier = Modifier,
    viewModel: RestaurantViewModel
) {
    val quantities by viewModel.quantities.collectAsState()
    val orderedItems by viewModel.orderedItems.collectAsState()
    val total by viewModel.total.collectAsState()
    val isEmpty by viewModel.isEmpty.collectAsState()
    val confirmation by viewModel.confirmation.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Restaurante El Sabor",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("appTitle"),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Haz tu pedido",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("appSubtitle"),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Menú",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("menuSectionTitle")
                )

                Spacer(modifier = Modifier.height(12.dp))

                MenuData.categories.forEach { category ->
                    CategorySection(
                        category = category,
                        items = MenuData.items.filter { it.category == category },
                        onAddItem = { item -> viewModel.addItem(item.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tu Pedido",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("orderSectionTitle")
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isEmpty) {
                    Text(
                        text = "El pedido está vacío. Añade productos del menú.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("emptyOrderMessage"),
                        textAlign = TextAlign.Center
                    )
                } else {
                    orderedItems.forEach { item ->
                        val quantity = quantities[item.id] ?: 0
                        OrderItemRow(
                            item = item,
                            quantity = quantity,
                            onIncrement = { viewModel.incrementItem(item.id) },
                            onDecrement = { viewModel.decrementItem(item.id) },
                            modifier = Modifier.testTag("orderItem_${item.id}")
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("orderTotalRow"),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("totalLabel")
                        )
                        Text(
                            text = "%.2f €".format(total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("totalValue")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.placeOrder() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("placeOrderButton")
                    ) {
                        Text("Realizar Pedido")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    confirmation?.let { orderConfirmation ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmation() },
            title = {
                Text(
                    text = "Pedido Confirmado",
                    modifier = Modifier.testTag("confirmationTitle")
                )
            },
            text = {
                Text(
                    text = "¡Pedido de ${orderConfirmation.itemCount} artículos por un total de %.2f € recibido! Preparen los fogones.".format(orderConfirmation.total),
                    modifier = Modifier.testTag("confirmationMessage")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.dismissConfirmation() },
                    modifier = Modifier.testTag("confirmationOkButton")
                ) {
                    Text("Aceptar")
                }
            },
            modifier = Modifier.testTag("confirmationDialog")
        )
    }
}

@Composable
private fun CategorySection(
    category: String,
    items: List<MenuItem>,
    onAddItem: (MenuItem) -> Unit
) {
    Text(
        text = category,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 8.dp, bottom = 4.dp)
            .testTag("categoryTitle_$category")
    )

    items.forEach { item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("menuItem_${item.id}"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("menuItemName_${item.id}")
                )
                Text(
                    text = "%.2f €".format(item.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("menuItemPrice_${item.id}")
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { onAddItem(item) },
                modifier = Modifier.testTag("addButton_${item.id}")
            ) {
                Text("+")
            }
        }
    }
}

@Composable
private fun OrderItemRow(
    item: MenuItem,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("orderItemName_${item.id}")
            )
            Text(
                text = "%.2f €".format(item.price),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedButton(
                onClick = onDecrement,
                modifier = Modifier.testTag("decrementOrderItem_${item.id}")
            ) {
                Text("-")
            }

            Text(
                text = "$quantity",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .width(24.dp)
                    .testTag("orderItemQuantity_${item.id}"),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onIncrement,
                modifier = Modifier.testTag("incrementOrderItem_${item.id}")
            ) {
                Text("+")
            }

            val subtotal = quantity * item.price
            Text(
                text = "%.2f €".format(subtotal),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .width(64.dp)
                    .testTag("orderItemSubtotal_${item.id}"),
                textAlign = TextAlign.End
            )
        }
    }
}

