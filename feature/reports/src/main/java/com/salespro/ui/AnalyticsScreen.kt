package com.salespro.ui

import androidx.compose.ui.res.stringResource
import com.salespro.core.R
import com.salespro.settings.Money

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salespro.data.Product
import com.salespro.data.Sale
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(sales: List<Sale>) {

    // Derived state for Analytics
    val totalRevenue = remember(sales) { sales.sumOf { (it.totalMinor / 100.0) } }
    val totalProfit = remember(sales) { sales.sumOf { 0.0 } }
    val itemsSold = sales.size

    // Group by Day for chart (Last 7 Days)
    val last7DaysMap = remember(sales) {
        val last7Days = (0..6).map {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -it)
            SimpleDateFormat("MM/dd", Locale.getDefault()).format(cal.time)
        }.reversed()

        val groupedByDateStr = sales.groupBy {
            SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(it.createdAt))
        }

        last7Days.associateWith { dateStr ->
            (groupedByDateStr[dateStr]?.sumOf { s -> (s.totalMinor / 100.0) } ?: 0.0).toFloat()
        }
    }
    val chartMax = remember(last7DaysMap) {
        (last7DaysMap.values.maxOrNull() ?: 100f).coerceAtLeast(100f)
    }

    // Top 3 Sellers
    val topSellers = remember(sales) {
        val sorted = sales.groupBy { "Invoice ${it.invoiceNumber}" }
             .mapValues { entry -> entry.value.sumOf { (it.totalMinor / 100.0) } }
             .entries.sortedByDescending { it.value }
             .take(3)
        sorted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Phase 1: High Level KPI Cards
            item {
                Text(stringResource(R.string.lifetime_overview), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        title = stringResource(R.string.revenue),
                        value = Money.formatMajor(totalRevenue, maxDecimals = 0),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                    MetricCard(
                        title = stringResource(R.string.profit),
                        value = Money.formatMajor(totalProfit, maxDecimals = 0),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        icon = Icons.Default.ShoppingCart
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                MetricCard(
                    title = stringResource(R.string.total_items_sold),
                    value = "$itemsSold",
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    icon = Icons.Default.List
                )
            }

            // Phase 2: Native Canvas 7-Day Chart
            item {
                Text(stringResource(R.string.last_7_days_revenue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val barColor = MaterialTheme.colorScheme.primary
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = size.width / (last7DaysMap.size * 1.5f)
                            val spaceWidth = (size.width - (barWidth * last7DaysMap.size)) / (last7DaysMap.size - 1)

                            var currentX = 0f

                            last7DaysMap.values.forEach { amount ->
                                val normalizedHeight = (amount / chartMax) * size.height
                                val topY = size.height - normalizedHeight

                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(currentX, topY),
                                    size = Size(barWidth, normalizedHeight.coerceAtLeast(10f)), // Min bar height of 10f for zeros
                                    cornerRadius = CornerRadius(4.dp.toPx())
                                )

                                currentX += barWidth + spaceWidth
                            }
                        }
                    }
                }
            }

            // Phase 3: Top Sellers Feed
            item {
                Text(stringResource(R.string.top_items), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (topSellers.isEmpty()) {
                item {
                    Text(stringResource(R.string.not_enough_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(topSellers) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(entry.key, fontWeight = FontWeight.SemiBold)
                            }
                            Text(Money.formatMajor(entry.value, maxDecimals = 0), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}
