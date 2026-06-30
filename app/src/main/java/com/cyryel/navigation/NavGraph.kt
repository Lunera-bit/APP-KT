package com.cyryel.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cyryel.ui.auth.AuthRoute
import com.cyryel.ui.billetera.BilleteraScreen
import com.cyryel.ui.categoryproducts.CategoryProductsScreen
import com.cyryel.ui.checkout.CheckoutScreen
import com.cyryel.ui.home.MainScreen
import com.cyryel.ui.orders.OrderDetailScreen
import com.cyryel.ui.orders.OrdersScreen
import com.cyryel.ui.productdetail.ProductDetailScreen
import com.cyryel.ui.promotions.PromotionsScreen
import com.cyryel.ui.search.SearchScreen
import com.cyryel.ui.settings.SettingsScreen

object Routes {
    const val AUTH = "auth"
    const val MAIN = "main"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val CHECKOUT = "checkout"
    const val ORDER_DETAIL = "order/{orderId}"
    const val CATEGORY_PRODUCTS = "category/{categoryName}"
    const val PROMOTIONS = "promotions"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val BILLETERA_OFFERS = "billetera/offers"
    const val BILLETERA_HISTORIAL = "billetera/historial"

    fun productDetail(id: String) = "product/$id"
    fun orderDetail(id: String) = "order/$id"
    fun categoryProducts(name: String) = "category/$name"
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
                onNavigateToCheckout = {
                    navController.navigate(Routes.CHECKOUT)
                },
                onNavigateToOrderDetail = { orderId ->
                    navController.navigate(Routes.orderDetail(orderId))
                },
                onNavigateToCategory = { categoryName ->
                    navController.navigate(Routes.categoryProducts(categoryName))
                },
                onNavigateToPromotions = {
                    navController.navigate(Routes.PROMOTIONS)
                },
                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE)
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
                onSignOut = {
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
                onBack = { navController.popBackStack() },
                onAddToCart = { navController.popBackStack() }
            )
        }

        composable(Routes.CHECKOUT) {
            CheckoutScreen(
                onBack = { navController.popBackStack() },
                onOrderCreated = { orderId ->
                    navController.navigate(Routes.orderDetail(orderId)) {
                        popUpTo(Routes.MAIN)
                    }
                }
            )
        }

        composable(
            route = Routes.ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailScreen(
                orderId = orderId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CATEGORY_PRODUCTS,
            arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            CategoryProductsScreen(
                categoryName = categoryName,
                onBack = { navController.popBackStack() },
                onProductClick = { productId ->
                    navController.navigate(Routes.productDetail(productId))
                }
            )
        }

        composable(Routes.PROMOTIONS) {
            PromotionsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE) {
            com.cyryel.ui.profile.ProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onProductClick = { productId ->
                    navController.navigate(Routes.productDetail(productId))
                }
            )
        }

        composable(Routes.BILLETERA_OFFERS) {
            com.cyryel.ui.billetera.OffersListScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.BILLETERA_HISTORIAL) {
            com.cyryel.ui.billetera.PointsHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
