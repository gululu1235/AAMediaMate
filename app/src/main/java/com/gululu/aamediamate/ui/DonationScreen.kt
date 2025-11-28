package com.gululu.aamediamate.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gululu.aamediamate.billing.BillingManager

@Composable
fun DonationScreen(billingManager: BillingManager) {
    val context = LocalContext.current
    val products by billingManager.products.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (products.isEmpty()) {
            CircularProgressIndicator()
            Text(text = "Loading donation options...", modifier = Modifier.padding(top = 8.dp))
        } else {
            Text(text = "Support the development of MediaBridge by making a donation.", modifier = Modifier.padding(bottom = 16.dp))
            products.forEach { product ->
                Button(
                    onClick = {
                        billingManager.launchPurchaseFlow(context as Activity, product)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(text = "${product.name} - ${product.oneTimePurchaseOfferDetails?.formattedPrice}")
                }
            }
        }
    }
}
