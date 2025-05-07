package com.insnaejack.pdfgenerator.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// Define your product IDs (SKUs) - these must match what you set up in Google Play Console
object ProductIds {
    const val PREMIUM_UPGRADE_ID = "premium_pdf_generator" // Example ID
    // Add other product IDs if needed, e.g., for subscriptions or other consumables
}

data class ProductDetailsWrapper(
    val productId: String,
    val name: String,
    val description: String,
    val formattedPrice: String,
    val productDetails: ProductDetails // Keep original for launching purchase flow
)

@Singleton
class BillingClientWrapper @Inject constructor(
    private val context: Context // ApplicationContext
) : PurchasesUpdatedListener, BillingClientStateListener {

    private lateinit var billingClient: BillingClient

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetailsWrapper>>(emptyMap())
    val productDetailsMap = _productDetailsMap.asStateFlow()

    private val _purchaseUpdateEvent = MutableStateFlow<Pair<BillingResult, List<Purchase>?>?>(null)
    val purchaseUpdateEvent = _purchaseUpdateEvent.asStateFlow()

    // To track if user has the premium upgrade
    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser = _isPremiumUser.asStateFlow()

    private var isBillingClientConnected = false

    companion object {
        private const val TAG = "BillingClientWrapper"
    }

    init {
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases() // Required
            .build()

        if (!billingClient.isReady) {
            Log.d(TAG, "BillingClient: Attempting to connect...")
            billingClient.startConnection(this)
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "BillingClient: Setup successful")
            isBillingClientConnected = true
            queryProductDetails()
            queryPurchasesAsync() // Check for existing purchases
        } else {
            Log.e(TAG, "BillingClient: Setup failed with code ${billingResult.responseCode} - ${billingResult.debugMessage}")
            isBillingClientConnected = false
            // Retry connection or handle error
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "BillingClient: Service disconnected. Retrying...")
        isBillingClientConnected = false
        // Implement retry logic if desired, e.g., with backoff
        // billingClient.startConnection(this)
    }

    fun queryProductDetails() {
        if (!isBillingClientConnected) {
            Log.w(TAG, "BillingClient not connected. Cannot query products.")
            // Optionally try to reconnect
            // initializeBillingClient()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(ProductIds.PREMIUM_UPGRADE_ID)
                .setProductType(BillingClient.ProductType.INAPP) // For one-time purchase
                .build()
            // Add other products here if needed
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                val newDetailsMap = productDetailsList.associate { details ->
                    val wrapper = ProductDetailsWrapper(
                        productId = details.productId,
                        name = details.name,
                        description = details.description,
                        formattedPrice = details.oneTimePurchaseOfferDetails?.formattedPrice ?: "N/A",
                        productDetails = details
                    )
                    details.productId to wrapper
                }
                _productDetailsMap.value = newDetailsMap
                Log.d(TAG, "Product details queried: $newDetailsMap")
            } else {
                Log.e(TAG, "Failed to query product details: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        if (!isBillingClientConnected) {
            Log.e(TAG, "BillingClient not connected. Cannot launch purchase flow.")
            // Show error to user or try to reconnect
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            // Handle error, e.g., show a message to the user
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        Log.d(TAG, "onPurchasesUpdated: ${billingResult.responseCode}, Purchases: $purchases")
        _purchaseUpdateEvent.value = Pair(billingResult, purchases) // Notify observers

        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "User cancelled the purchase.")
        } else {
            Log.w(TAG, "Purchase error: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { ackResult ->
                    if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged: ${purchase.products.joinToString()}")
                        // Grant entitlement to the user.
                        if (purchase.products.contains(ProductIds.PREMIUM_UPGRADE_ID)) {
                            _isPremiumUser.value = true
                        }
                    } else {
                        Log.e(TAG, "Failed to acknowledge purchase: ${ackResult.debugMessage}")
                    }
                }
            } else {
                // Purchase already acknowledged
                Log.d(TAG, "Purchase already acknowledged: ${purchase.products.joinToString()}")
                if (purchase.products.contains(ProductIds.PREMIUM_UPGRADE_ID)) {
                    _isPremiumUser.value = true
                }
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase is pending. Grant entitlement when confirmed.")
            // Handle pending transactions (e.g., user needs to complete payment via another method)
        } else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            Log.w(TAG, "Purchase in unspecified state.")
        }
    }

    fun queryPurchasesAsync() {
        if (!isBillingClientConnected) {
            Log.w(TAG, "BillingClient not connected. Cannot query purchases.")
            return
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var premiumFound = false
                purchasesList.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.products.contains(ProductIds.PREMIUM_UPGRADE_ID)) {
                        premiumFound = true
                        // Ensure it's acknowledged if not already
                        if (!purchase.isAcknowledged) {
                            handlePurchase(purchase) // This will acknowledge it
                        }
                    }
                }
                _isPremiumUser.value = premiumFound
                Log.d(TAG, "Active INAPP purchases queried. Premium: $premiumFound")
            } else {
                Log.e(TAG, "Failed to query INAPP purchases: ${billingResult.debugMessage}")
            }
        }
        // Also query subscriptions if you have them
    }

    fun consumePurchaseUpdateEvent() {
        _purchaseUpdateEvent.value = null
    }

    // Call this when your app is closing or the relevant lifecycle owner is destroyed
    fun endConnection() {
        if (billingClient.isReady) {
            Log.d(TAG, "BillingClient: Ending connection.")
            billingClient.endConnection()
        }
        isBillingClientConnected = false
    }
}