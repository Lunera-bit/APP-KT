package com.CYRYEL.com.ui.auth

import app.cash.turbine.test
import com.CYRYEL.com.TestCoroutineRule
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.user.User
import com.CYRYEL.com.data.user.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    @RelaxedMockK
    private lateinit var userRepository: UserRepository

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        every { authRepository.authStateFlow() } returns emptyFlow()
        viewModel = AuthViewModel(authRepository, userRepository)
    }

    @Test
    fun `signIn with blank email shows error`() = runTest {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("password123")

        viewModel.signIn()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Ingresa correo y contrasena", state.errorMessage)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `signIn with blank password shows error`() = runTest {
        viewModel.onEmailChange("test@test.com")
        viewModel.onPasswordChange("")

        viewModel.signIn()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Ingresa correo y contrasena", state.errorMessage)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `signIn successfully authenticates`() = runTest {
        val email = "test@test.com"
        val password = "password123"
        every { authRepository.isLoggedIn() } returns true
        every { authRepository.getCurrentUserId() } returns "uid123"
        every { authRepository.getCurrentUserEmail() } returns email
        coEvery { authRepository.signIn(email, password) } returns Result.success(Unit)
        coEvery { authRepository.saveFcmToken(any()) } just runs
        coEvery { userRepository.getUser("uid123") } returns Result.success(User(id = "uid123", role = "user"))

        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.signIn()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isAuthenticated)
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
            assertEquals("user", state.role)
        }
        coVerify { authRepository.signIn(email, password) }
        coVerify { authRepository.saveFcmToken("uid123") }
        coVerify { userRepository.getUser("uid123") }
    }

    @Test
    fun `signIn with invalid credentials shows error`() = runTest {
        val email = "wrong@test.com"
        val password = "wrongpass"
        coEvery { authRepository.signIn(email, password) } returns Result.failure(Exception("Credenciales invalidas"))

        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.signIn()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isAuthenticated)
            assertFalse(state.isLoading)
            assertEquals("Credenciales invalidas", state.errorMessage)
        }
    }

    @Test
    fun `signInWithGoogle successfully authenticates`() = runTest {
        val idToken = "google-id-token"
        every { authRepository.isLoggedIn() } returns true
        every { authRepository.getCurrentUserId() } returns "uid-google"
        coEvery { authRepository.signInWithGoogle(idToken) } returns Result.success(Unit)
        coEvery { authRepository.saveFcmToken(any()) } just runs
        coEvery { userRepository.getUser("uid-google") } returns Result.success(User(id = "uid-google", role = "user"))

        viewModel.signInWithGoogle(idToken)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isAuthenticated)
            assertFalse(state.isLoading)
        }
        coVerify { authRepository.signInWithGoogle(idToken) }
    }

    @Test
    fun `signInWithGoogle failure shows error`() = runTest {
        coEvery { authRepository.signInWithGoogle(any()) } returns Result.failure(Exception("Error de Google"))

        viewModel.signInWithGoogle("bad-token")

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isAuthenticated)
            assertFalse(state.isLoading)
            assertEquals("Error de Google", state.errorMessage)
        }
    }

    @Test
    fun `signOut resets state`() = runTest {
        every { authRepository.isLoggedIn() } returns true
        every { authRepository.getCurrentUserId() } returns "uid123"
        every { authRepository.getCurrentUserEmail() } returns "test@test.com"
        coEvery { authRepository.signIn("test@test.com", "pass") } returns Result.success(Unit)
        coEvery { authRepository.saveFcmToken(any()) } just runs
        coEvery { userRepository.getUser("uid123") } returns Result.success(User(id = "uid123", role = "user"))
        every { authRepository.signOut() } just runs

        viewModel.onEmailChange("test@test.com")
        viewModel.onPasswordChange("pass")
        viewModel.signIn()
        viewModel.signOut()

        verify { authRepository.signOut() }
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isAuthenticated)
        assertFalse(state.termsAccepted)
        assertFalse(state.isRoleLoaded)
    }

    @Test
    fun `onEmailChange updates email and clears error`() {
        viewModel.onEmailChange("new@email.com")
        assertEquals("new@email.com", viewModel.uiState.value.email)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onPasswordChange updates password and clears error`() {
        viewModel.onPasswordChange("newpass")
        assertEquals("newpass", viewModel.uiState.value.password)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onTermsAcceptedChange updates termsAccepted and clears error`() {
        viewModel.onTermsAcceptedChange(true)
        assertTrue(viewModel.uiState.value.termsAccepted)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `showError sets errorMessage`() {
        viewModel.showError("Custom error")
        assertEquals("Custom error", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `not logged in sets isRoleLoaded immediately`() {
        every { authRepository.isLoggedIn() } returns false
        val vm = AuthViewModel(authRepository, userRepository)
        assertTrue(vm.uiState.value.isRoleLoaded)
        assertFalse(vm.uiState.value.isAuthenticated)
    }

    @Test
    fun `logged in loads user role`() = runTest {
        every { authRepository.isLoggedIn() } returns true
        every { authRepository.getCurrentUserId() } returns "uid123"
        coEvery { authRepository.saveFcmToken(any()) } just runs
        coEvery { userRepository.getUser("uid123") } returns Result.success(User(id = "uid123", role = "admin"))

        val vm = AuthViewModel(authRepository, userRepository)

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.isAuthenticated)
            assertEquals("admin", state.role)
            assertTrue(state.isRoleLoaded)
        }
    }

    @Test
    fun `logged in but loadUserRole fails still sets isRoleLoaded`() = runTest {
        every { authRepository.isLoggedIn() } returns true
        every { authRepository.getCurrentUserId() } returns "uid123"
        every { authRepository.getCurrentUserEmail() } returns "test@test.com"
        coEvery { authRepository.saveFcmToken(any()) } just runs
        coEvery { userRepository.getUser("uid123") } returns Result.failure(Exception("User not found"))

        val vm = AuthViewModel(authRepository, userRepository)

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.isRoleLoaded)
            assertEquals("user", state.role)
        }
    }
}
