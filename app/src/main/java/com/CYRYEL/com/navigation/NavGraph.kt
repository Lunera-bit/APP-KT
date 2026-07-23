package com.CYRYEL.com.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.CYRYEL.com.ui.auth.AuthRoute
import com.CYRYEL.com.ui.billetera.BilleteraScreen
import com.CYRYEL.com.ui.categoryproducts.CategoryProductsScreen
import com.CYRYEL.com.ui.cart.CartScreen
import com.CYRYEL.com.ui.checkout.CheckoutScreen
import com.CYRYEL.com.ui.delivery.DeliveryMainScreen
import com.CYRYEL.com.ui.home.MainScreen
import com.CYRYEL.com.ui.notifications.BandejaNotificacionesScreen
import com.CYRYEL.com.ui.orders.OrderDetailScreen
import com.CYRYEL.com.ui.productdetail.ProductDetailScreen
import com.CYRYEL.com.ui.promotiondetail.PromotionDetailScreen
import com.CYRYEL.com.ui.search.SearchScreen
import com.CYRYEL.com.ui.auth.AuthViewModel
import com.CYRYEL.com.ui.profile.AddressScreen
import com.CYRYEL.com.ui.settings.SettingsScreen
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.hilt.navigation.compose.hiltViewModel

object Routes {
    const val AUTH = "auth"
    const val MAIN = "main"
    const val DELIVERY = "delivery"
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
    const val PROMOTION_DETAIL = "promotion/{promotionId}"

    fun productDetail(id: String) = "product/$id"
    fun orderDetail(id: String) = "order/$id"
    fun categoryProducts(name: String) = "category/$name"
    fun promotionDetail(id: String) = "promotion/$id"
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
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentRoute by navController.currentBackStackEntryAsState()

    LaunchedEffect(Unit) {
        var wasLoggedIn = authViewModel.uiState.value.isAuthenticated
        authViewModel.authStateFlow.collect { user ->
            val isLoggedIn = user != null
            if (wasLoggedIn && !isLoggedIn && currentRoute?.destination?.route != Routes.AUTH) {
                navController.navigate(Routes.AUTH) {
                    popUpTo(0) { inclusive = true }
                }
            }
            wasLoggedIn = isLoggedIn
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH,
        modifier = modifier
    ) {
        composable(Routes.AUTH) {
            AuthRoute(
                modifier = androidx.compose.ui.Modifier,
                onNavigateToMain = { role ->
                    val dest = if (role == "delivery") Routes.DELIVERY else Routes.MAIN
                    navController.navigate(dest) {
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
                },
                onNavigateToPromotion = { promotionId ->
                    navController.navigate(Routes.promotionDetail(promotionId))
                }
            )
        }

        composable(Routes.DELIVERY) {
            val context = LocalContext.current
            DeliveryMainScreen(
                onSignOut = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail().build()
                    GoogleSignIn.getClient(context, gso).signOut()
                    authViewModel.signOut()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
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
                },
                onPromotionClick = { promotionId ->
                    navController.navigate(Routes.promotionDetail(promotionId))
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
            deepLinks = listOf(
                navDeepLink { uriPattern = "cyryel://order/{orderId}" },
                navDeepLink { uriPattern = "cyryel://shop/pedidos/{orderId}" },
                navDeepLink { uriPattern = "cyryel://delivery/pedidos/{orderId}" }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailScreen(
                orderId = orderId,
                onBack = rememberBackHandler { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PROMOTION_DETAIL,
            arguments = listOf(navArgument("promotionId") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "cyryel://promotion/{promotionId}" }
            )
        ) { backStackEntry ->
            val promotionId = backStackEntry.arguments?.getString("promotionId") ?: ""
            PromotionDetailScreen(
                promotionId = promotionId,
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
                },
                onCategoryClick = { categoryName ->
                    navController.navigate(Routes.categoryProducts(categoryName))
                }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            BandejaNotificacionesScreen(
                onBack = rememberBackHandler { navController.popBackStack() },
                onNotificationClick = { link ->
                    if (link != null) {
                        val uri = android.net.Uri.parse(link)
                        val path = uri.pathSegments
                        when {
                            path.size >= 2 && path[0] == "pedidos" ->
                                navController.navigate(Routes.orderDetail(path[1]))
                            path.size >= 1 && uri.host == "promotion" ->
                                navController.navigate(Routes.promotionDetail(path[0]))
                        }
                    }
                }
            )
        }

        composable(Routes.BILLETERA_OFFERS) {
            com.CYRYEL.com.ui.billetera.OffersListScreen(
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
            com.CYRYEL.com.ui.billetera.PointsHistoryScreen(
                onBack = rememberBackHandler { navController.popBackStack() }
            )
        }
    }
}
