package com.montanhajr.petclicker.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onPremiumPurchased: (Boolean) -> Unit
) : PurchasesUpdatedListener {

    private val tag = "BillingManager"

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails = _productDetails.asStateFlow()

    private val _isBillingReady = MutableStateFlow(false)

    fun startConnection() {
        Log.d(tag, "Iniciando conexão com Google Play...")
        if (billingClient.isReady) {
            Log.d(tag, "BillingClient já está pronto.")
            _isBillingReady.value = true
            queryPurchases()
            queryProductDetails()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    Log.d(tag, "Conexão estabelecida com sucesso.")
                    _isBillingReady.value = true
                    queryPurchases()
                    queryProductDetails()
                } else {
                    Log.e(tag, "Erro ao conectar: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                    // Se o erro for 3, sugira verificar a conta/dispositivo
                    if (billingResult.responseCode == 3) {
                        Log.e(tag, "DICA: Verifique se a Google Play Store está logada e atualizada no dispositivo.")
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(tag, "Serviço desconectado.")
                _isBillingReady.value = false
            }
        })
    }

    private fun queryProductDetails() {
        if (!billingClient.isReady) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_access")
                .setProductType(ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                val product = productDetailsList.find { it.productId == "premium_access" }
                _productDetails.value = product
                if (product != null) {
                    Log.d(tag, "Produto encontrado: ${product.name} - ${product.oneTimePurchaseOfferDetails?.formattedPrice}")
                } else {
                    Log.e(tag, "Produto 'premium_access' não encontrado no console. Verifique o ID.")
                }
            } else {
                Log.e(tag, "Erro ao buscar detalhes do produto: ${billingResult.debugMessage}")
            }
        }
    }

    fun queryPurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                val isPremium = purchases.any { purchase ->
                    purchase.products.contains("premium_access") && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                Log.d(tag, "Status Premium atual: $isPremium")
                onPremiumPurchased(isPremium)
                
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
            }
        }
    }

    fun launchBillingFlow(activity: Activity) {
        val product = _productDetails.value
        
        if (!billingClient.isReady) {
            Log.w(tag, "BillingClient não está pronto. Tentando reconectar...")
            startConnection()
            return
        }

        if (product == null) {
            Log.e(tag, "Impossível iniciar compra: Detalhes do produto nulos. Verifique o Logcat para erros anteriores.")
            queryProductDetails()
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgePurchase(purchase)
                }
            }
        } else if (billingResult.responseCode != BillingResponseCode.USER_CANCELED) {
            Log.e(tag, "Erro na atualização de compra: ${billingResult.debugMessage}")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                Log.d(tag, "Compra confirmada.")
                onPremiumPurchased(true)
            }
        }
    }
}
