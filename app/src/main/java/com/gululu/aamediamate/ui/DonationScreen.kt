package com.gululu.aamediamate.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gululu.aamediamate.R
import com.gululu.aamediamate.billing.BillingManager
import com.gululu.aamediamate.billing.BillingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationScreen(billingManager: BillingManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val billingState by billingManager.billingState.collectAsState()
    val purchases by billingManager.purchases.collectAsState()

    androidx.activity.compose.BackHandler {
        onBack()
    }

    // Check actual purchase status
    val hasTier1 = purchases.any { it.products.contains("donate_tier_1") }
    val hasTier2 = purchases.any { it.products.contains("donate_tier_2") }

    // Toggle states for visuals (default to true if purchased)
    var isTipEnabled by remember { mutableStateOf(false) }
    var isCoffeeEnabled by remember { mutableStateOf(false) }

    // Sync toggle state with purchase state changes (only enable once when purchased)
    LaunchedEffect(hasTier1) { if (hasTier1) isTipEnabled = true }
    LaunchedEffect(hasTier2) { if (hasTier2) isCoffeeEnabled = true }

    // Determine background image based on TOGGLES
    val bgResource = when {
        isTipEnabled && isCoffeeEnabled -> R.drawable.donate_coffee_tip
        isTipEnabled -> R.drawable.donate_tip
        isCoffeeEnabled -> R.drawable.donate_coffee
        else -> R.drawable.donate
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.donate), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B1B2F), // Deep Purple Background
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply padding to avoid TopAppBar
                .background(Color(0xFF1B1B2F)) // Deep Purple Background
        ) {
            // Background Image - placed at top, behind text and buttons
            Image(
                painter = painterResource(id = bgResource),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter), // Align image to top
                contentScale = ContentScale.FillWidth
            )

            // Thank you text - Floats on top
            Text(
                text = stringResource(id = R.string.thank_you_support),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp) // Adjusted padding, relative to TopAppBar now
            )

            // Buttons Container - Bottom Center
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp) // No innerPadding here, as it was applied to the parent Box
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp) // Vertical spacing between buttons
            ) {
                when (val state = billingState) {
                    is BillingUiState.Loading -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    is BillingUiState.Error -> {
                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { billingManager.retryConnection() }) { Text("Retry") }
                    }
                    is BillingUiState.Success -> {
                        val allProducts = state.products
                        val tipProduct = allProducts.find { it.productId == "donate_tier_1" }
                        val coffeeProduct = allProducts.find { it.productId == "donate_tier_2" }

                        // --- Tier 1 (Tip) ---
                        if (hasTier1) {
                            FilterChip(
                                selected = isTipEnabled,
                                onClick = { isTipEnabled = !isTipEnabled },
                                label = { Text(stringResource(id = R.string.send_me_a_tip)) },
                                leadingIcon = if (isTipEnabled) {
                                    { Icon(Icons.Default.Check, null) }
                                } else null,
                                modifier = Modifier.fillMaxWidth(),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        } else if (tipProduct != null) {
                            Button(
                                onClick = { billingManager.launchPurchaseFlow(context as Activity, tipProduct) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                            ) {
                                Text("${stringResource(id = R.string.send_me_a_tip)} - ${tipProduct.oneTimePurchaseOfferDetails?.formattedPrice}")
                            }
                        }

                        // --- Tier 2 (Coffee) ---
                        if (hasTier2) {
                            FilterChip(
                                selected = isCoffeeEnabled,
                                onClick = { isCoffeeEnabled = !isCoffeeEnabled },
                                label = { Text(stringResource(id = R.string.buy_me_a_coffee)) },
                                leadingIcon = if (isCoffeeEnabled) {
                                    { Icon(Icons.Default.Check, null) }
                                } else null,
                                modifier = Modifier.fillMaxWidth(),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        } else if (coffeeProduct != null) {
                            Button(
                                onClick = { billingManager.launchPurchaseFlow(context as Activity, coffeeProduct) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                            ) {
                                Text("${stringResource(id = R.string.buy_me_a_coffee)} - ${coffeeProduct.oneTimePurchaseOfferDetails?.formattedPrice}")
                            }
                        }
                        
                        if (tipProduct == null && coffeeProduct == null && !hasTier1 && !hasTier2) {
                             Text("No donation options found.", color = Color.White)
                        }
                    }
                }
                // Extra spacer at bottom if needed
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
