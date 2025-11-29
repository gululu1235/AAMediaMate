package com.gululu.aamediamate.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(private val context: Context, private val scope: CoroutineScope) {

    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases = _purchases.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            _purchases.value = purchases
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d("BillingManager", "User cancelled the purchase flow.")
        } else {
            // Handle any other error codes.
        }
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    private val _billingState = MutableStateFlow<BillingUiState>(BillingUiState.Loading)
    val billingState = _billingState.asStateFlow()

    fun startConnection() {
        _billingState.value = BillingUiState.Loading
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingManager", "Billing client connected.")
                    queryProducts()
                    queryPurchases()
                } else {
                    _billingState.value = BillingUiState.Error("Setup failed: ${billingResult.responseCode} ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d("BillingManager", "Billing client disconnected.")
                _billingState.value = BillingUiState.Error("Service disconnected")
            }
        })
    }

    fun retryConnection() {
        startConnection()
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _purchases.value = purchasesList
                Log.d("BillingManager", "Query purchases success: ${purchasesList.size} items")
            } else {
                Log.e("BillingManager", "Query purchases failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun queryProducts() {
        scope.launch(Dispatchers.IO) {
            val productList = listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId("donate_tier_1")
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build(),
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId("donate_tier_2")
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                            )
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val productDetailsResult = billingClient.queryProductDetails(params)
            val billingResult = productDetailsResult.billingResult
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val products = productDetailsResult.productDetailsList ?: emptyList()
                _billingState.value = BillingUiState.Success(products)
                Log.d("BillingManager", "Product query successful. Found ${products.size} products.")
            } else {
                _billingState.value = BillingUiState.Error("Query failed: ${billingResult.responseCode} ${billingResult.debugMessage}")
                Log.e("BillingManager", "Product query failed with response code: ${billingResult.responseCode} and message: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("BillingManager", "Purchase acknowledged.")
                    }
                }
            }
        }
    }
}

sealed interface BillingUiState {
    data object Loading : BillingUiState
    data class Success(val products: List<ProductDetails>) : BillingUiState
    data class Error(val message: String) : BillingUiState
}
