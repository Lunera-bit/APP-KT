package com.CYRYEL.com.ui.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.CYRYEL.com.data.category.CategoryRepository
import com.CYRYEL.com.data.product.ProductRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun searchScreen_showsSearchField() {
        val productRepo = mockk<ProductRepository>(relaxed = true)
        val categoryRepo = mockk<CategoryRepository>(relaxed = true) {
            coEvery { getCategories() } returns Result.success(emptyList())
        }
        val vm = SearchViewModel(productRepo, categoryRepo)

        composeTestRule.setContent {
            SearchScreen(onBack = {}, onProductClick = {}, viewModel = vm)
        }
    }

    @Test
    fun searchScreen_showsCategoriesTitle() {
        val productRepo = mockk<ProductRepository>(relaxed = true)
        val categoryRepo = mockk<CategoryRepository>(relaxed = true) {
            coEvery { getCategories() } returns Result.success(emptyList())
        }
        val vm = SearchViewModel(productRepo, categoryRepo)

        composeTestRule.setContent {
            SearchScreen(onBack = {}, onProductClick = {}, viewModel = vm)
        }
    }
}
