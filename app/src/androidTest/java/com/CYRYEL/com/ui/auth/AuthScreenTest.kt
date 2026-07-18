package com.CYRYEL.com.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.user.UserRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AuthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(
        isLoggedIn: Boolean = false,
        errorMessage: String? = null,
        isLoading: Boolean = false,
        termsAccepted: Boolean = false
    ): AuthViewModel {
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { isLoggedIn() } returns isLoggedIn
            every { authStateFlow() } returns emptyFlow()
        }
        val userRepo = mockk<UserRepository>(relaxed = true)
        val vm = AuthViewModel(authRepo, userRepo)
        if (errorMessage != null) vm.showError(errorMessage)
        if (termsAccepted) vm.onTermsAcceptedChange(true)
        return vm
    }

    @Test
    fun authScreen_showsBrandHeader() {
        val vm = createViewModel()
        composeTestRule.setContent {
            AuthRoute(viewModel = vm)
        }
        composeTestRule.onNodeWithText("CYRYEL STORE").assertIsDisplayed()
    }

    @Test
    fun authScreen_showsSubtitle() {
        val vm = createViewModel()
        composeTestRule.setContent {
            AuthRoute(viewModel = vm)
        }
        composeTestRule.onNodeWithText("Inicia sesion para continuar").assertIsDisplayed()
    }

    @Test
    fun authScreen_googleButtonDisabledWithoutTerms() {
        val vm = createViewModel(termsAccepted = false)
        composeTestRule.setContent {
            AuthRoute(viewModel = vm)
        }
        composeTestRule.onNodeWithText("Continuar con Google").assertIsNotEnabled()
    }

    @Test
    fun authScreen_googleButtonEnabledWhenTermsAccepted() {
        val vm = createViewModel(termsAccepted = true)
        composeTestRule.setContent {
            AuthRoute(viewModel = vm)
        }
        composeTestRule.onNodeWithText("Continuar con Google").assertIsEnabled()
    }

    @Test
    fun authScreen_showsErrorMessage() {
        val vm = createViewModel(errorMessage = "Credenciales invalidas")
        composeTestRule.setContent {
            AuthRoute(viewModel = vm)
        }
        composeTestRule.onNodeWithText("Credenciales invalidas").assertIsDisplayed()
    }

    @Test
    fun authScreen_showsLoadingOverlay() {
        val vm = createViewModel(isLoading = true)
        composeTestRule.setContent {
            AuthRoute(viewModel = vm)
        }
        composeTestRule.onNodeWithText("Iniciando sesion...").assertIsDisplayed()
    }

    @Test
    fun authScreen_hidesLoginFormWhenAuthenticated() {
        val vm = createViewModel(isLoggedIn = true)
        composeTestRule.setContent {
            AuthRoute(
                viewModel = vm,
                onNavigateToMain = {}
            )
        }
        composeTestRule.onNodeWithText("CYRYEL STORE").assertDoesNotExist()
    }

    @Test
    fun authScreen_disablesButtonDuringLoading() {
        val vm = createViewModel(isLoading = true, termsAccepted = true)
        composeTestRule.setContent {
            AuthRoute(viewModel = vm)
        }
        composeTestRule.onNodeWithText("Continuar con Google").assertIsNotEnabled()
    }
}
