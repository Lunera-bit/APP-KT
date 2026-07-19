package com.CYRYEL.com.ui.billetera

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.product.Product
import com.CYRYEL.com.data.product.ProductRepository
import com.CYRYEL.com.data.promotion.PromotionRepository
import com.CYRYEL.com.data.user.User
import com.CYRYEL.com.data.user.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BilleteraScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun fakeUserRepo(user: User): UserRepository = object : UserRepository {
        override suspend fun getUser(userId: String): Result<User> = Result.success(user)
        override suspend fun saveUser(u: User): Result<Unit> = Result.success(Unit)
        override suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<Unit> = Result.success(Unit)
        override fun getCurrentUserId(): String? = null
    }

    private fun fakeProductRepo(result: Result<List<Product>> = Result.success(emptyList())): ProductRepository =
        object : ProductRepository {
            override suspend fun getRandomProducts(limit: Int) = result
            override suspend fun getProductById(productId: String) = error("not called")
            override fun observeProduct(productId: String) = error("not called")
            override suspend fun getProductsByCategory(category: String, limit: Int) =
                Result.success(emptyList<Product>())
            override suspend fun getProductsByCategoryPaged(
                category: String, limit: Int, startAfter: String?
            ) = error("not called")
            override suspend fun searchProducts(query: String, limit: Int) =
                Result.success(emptyList<Product>())
            override suspend fun searchProductsByCategory(query: String, category: String, limit: Int) =
                Result.success(emptyList<Product>())
            override suspend fun getCategories() = error("not called")
            override suspend fun getRedeemableProducts(limit: Int) = result
        }

    @Test
    fun billeteraScreen_showsTitle() {
        val userRepo = fakeUserRepo(User(id = "u1", name = "Test", points = 100))
        val productRepo = fakeProductRepo()
        val promoRepo = mockk<PromotionRepository>(relaxed = true) {
            every { getActivePromotions() } returns flowOf(emptyList())
        }
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getCurrentUserId() } returns "user-123"
        }
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val cartManager = mockk<CartManager>(relaxed = true)

        val vm = BilleteraViewModel(userRepo, promoRepo, productRepo, authRepo, firestore, cartManager)

        composeTestRule.setContent {
            BilleteraScreen(
                onNavigateToOffers = {},
                onNavigateToHistorial = {},
                viewModel = vm
            )
        }

        composeTestRule.onNodeWithText("Billetera").assertIsDisplayed()
    }

    @Test
    fun billeteraScreen_showsUserPoints() {
        val userRepo = fakeUserRepo(User(id = "u1", name = "Test User", points = 500))
        val productRepo = fakeProductRepo()
        val promoRepo = mockk<PromotionRepository>(relaxed = true) {
            every { getActivePromotions() } returns flowOf(emptyList())
        }
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getCurrentUserId() } returns "user-123"
        }
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val cartManager = mockk<CartManager>(relaxed = true)

        val vm = BilleteraViewModel(userRepo, promoRepo, productRepo, authRepo, firestore, cartManager)

        composeTestRule.setContent {
            BilleteraScreen(
                onNavigateToOffers = {},
                onNavigateToHistorial = {},
                viewModel = vm
            )
        }
    }

    @Test
    fun billeteraScreen_showsErrorState() {
        val userRepo = object : UserRepository {
            override suspend fun getUser(userId: String): Result<User> = Result.failure(Exception("Error"))
            override suspend fun saveUser(u: User): Result<Unit> = Result.success(Unit)
            override suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<Unit> = Result.success(Unit)
            override fun getCurrentUserId(): String? = null
        }
        val productRepo = fakeProductRepo()
        val promoRepo = mockk<PromotionRepository>(relaxed = true) {
            every { getActivePromotions() } returns flowOf(emptyList())
        }
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getCurrentUserId() } returns "user-123"
        }
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val cartManager = mockk<CartManager>(relaxed = true)

        val vm = BilleteraViewModel(userRepo, promoRepo, productRepo, authRepo, firestore, cartManager)

        composeTestRule.setContent {
            BilleteraScreen(
                onNavigateToOffers = {},
                onNavigateToHistorial = {},
                viewModel = vm
            )
        }

        composeTestRule.onNodeWithText("Reintentar").assertIsDisplayed()
    }
}
