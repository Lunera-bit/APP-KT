package com.cyryel.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.cyryel.ui.auth.AuthRoute
import com.cyryel.ui.billetera.BilleteraScreen
import com.cyryel.ui.categoryproducts.CategoryProductsScreen
import com.cyryel.ui.cart.CartScreen
import com.cyryel.ui.checkout.CheckoutScreen
import com.cyryel.ui.home.MainScreen
import com.cyryel.ui.notifications.NotificationScreen
import com.cyryel.ui.orders.OrderDetailScreen
import com.cyryel.ui.productdetail.ProductDetailScreen
import com.cyryel.ui.search.SearchScreen
import com.cyryel.ui.auth.AuthViewModel
import com.cyryel.ui.profile.AddressScreen
import com.cyryel.ui.settings.SettingsScreen
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.hilt.navigation.compose.hiltViewModel

object Routes {
    const val AUTH = "auth"
    const val MAIN = "main"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val CART = "cart"
    const val CHECKOUT = "checkout"
    const val ORDER_DETAIL = "order/{orderId}"
    const val CATEGORY_PRODUCTS = "category/{categoryName}"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val NOTIFICATIONS = "notifications"
    const val ADDRESSES = "addresses"
    const val BILLETERA_OFFERS = "billetera/offers"
    const val BILLETERA_HISTORIAL = "billetera/historial"

    fun productDetail(id: String) = "product/$id"
    fun orderDetail(id: String) = "order/$id"
    fun categoryProducts(name: String) = "category/$name"
}

/**
 * Prevents rapid multiple invocations of [onBack] (e.g. double-tapping the back arrow)
 * by disabling the callback after the first call.
 * Resets automatically when the composable is removed from the composition
 * (i.e. when navigation completes and the screen is popped).
 */
@Composable
private fun rememberBackHandler(onBack: () -> Unit): () -> Unit {
    var enabled by remember { mutableStateOf(true) }
    return {
        if (enabled) {
            enabled = false
            onBack()
        }
    }
}

@Composable
fun AppNavGraph(navController: NavHostController, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    NavHost(
        navController = navController,
        startDestination = Routes.AUTH,
        modifier = modifier
    ) {
        composable(Routes.AUTH) {
            AuthRoute(
                modifier = androidx.compose.ui.Modifier,
                onNavigateToMain = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToProduct = { productId ->
                    navController.navigate(Routes.productDetail(productId))
                },
                onNavigateToOrderDetail = { orderId ->
                    navController.navigate(Routes.orderDetail(orderId))
                },
                onNavigateToCategory = { categoryName ->
                    navController.navigate(Routes.categoryProducts(categoryName))
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToSearch = {
                    navController.navigate(Routes.SEARCH)
                },
                onNavigateToOffers = {
                    navController.navigate(Routes.BILLETERA_OFFERS)
                },
                onNavigateToHistorial = {
                    navController.navigate(Routes.BILLETERA_HISTORIAL)
                },
                onNavigateToNotifications = {
                    navController.navigate(Routes.NOTIFICATIONS)
                },
                onNavigateToCart = {
                    navController.navigate(Routes.CART)
                },
                onNavigateToAddresses = {
                    navController.navigate(Routes.ADDRESSES)
                }
            )
        }

        composable(
            route = Routes.PRODUCT_DETAIL,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                onBack = rememberBackHandler { navController.popBackStack() },
                onAddToCart = {}
            )
        }

        composable(Routes.CART) {
            CartScreen(
                onBack = rememberBackHandler { navController.popBackStack() },
                onCheckout = {
                    navController.navigate(Routes.CHECKOUT)
                },
                onProductClick = { productId ->
                    navController.navigate(Routes.productDetail(productId))
                }
            )
        }

        composable(Routes.CHECKOUT) {
            CheckoutScreen(
                onBack = rememberBackHandler { navController.popBackStack() },
                onOrderCreated = { orderId ->
                    navController.navigate(Routes.orderDetail(orderId)) {
                        popUpTo(Routes.MAIN)
                    }
                },
                onGoHome = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "cyryel://order/{orderId}" })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailScreen(
                orderId = orderId,
                onBack = rememberBackHandler { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CATEGORY_PRODUCTS,
            arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            CategoryProductsScreen(
                categoryName = categoryName,
                onBack = rememberBackHandler { navController.popBackStack() },
                onProductClick = { productId ->
                    navController.navigate(Routes.productDetail(productId))
                }
            )
        }

        composable(Routes.SETTINGS) {
            val authViewModel: AuthViewModel = hiltViewModel()
            val context = LocalContext.current
            SettingsScreen(
                onBack = rememberBackHandler { navController.popBackStack() },
                onLogout = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail().build()
                    GoogleSignIn.getClient(context, gso).signOut()
                    authViewModel.signOut()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = rememberBackHandler { navController.popBackStack() },
                onProductClick = { productId ->
                    navController.navigate(Routes.productDetail(productId))
                }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationScreen(
                onBack = rememberBackHandler { navController.popBackStack() }
            )
        }

        composable(Routes.BILLETERA_OFFERS) {
            com.cyryel.ui.billetera.OffersListScreen(
                onBack = rememberBackHandler { navController.popBackStack() },
                onProductClick = { productId ->
                    navController.navigate("product/$productId")
                },
                onNavigateToCart = {
                    navController.navigate(Routes.CART)
                }
            )
        }

        composable(Routes.ADDRESSES) {
            AddressScreen(
                onBack = rememberBackHandler { navController.popBackStack() }
            )
        }

        composable(Routes.BILLETERA_HISTORIAL) {
            com.cyryel.ui.billetera.PointsHistoryScreen(
                onBack = rememberBackHandler { navController.popBackStack() }
            )
        }
    }
}
