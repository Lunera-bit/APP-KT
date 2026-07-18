package com.CYRYEL.com.ui.notifications

import app.cash.turbine.test
import com.CYRYEL.com.TestCoroutineRule
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.notificacion.NotificacionData
import com.CYRYEL.com.data.notificacion.NotificacionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BandejaViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    @RelaxedMockK
    private lateinit var notificacionRepository: NotificacionRepository

    @RelaxedMockK
    private lateinit var authRepository: AuthRepository

    private lateinit var viewModel: BandejaViewModel

    private val sampleNotificaciones = listOf(
        NotificacionData(id = "n1", titulo = "Noti 1", read = false, userId = "user-123"),
        NotificacionData(id = "n2", titulo = "Noti 2", read = true, userId = "user-123"),
        NotificacionData(id = "n3", titulo = "Noti 3", read = false, userId = "user-123")
    )

    @Before
    fun setUp() {
        every { authRepository.getCurrentUserId() } returns "user-123"
        every { notificacionRepository.getNotificaciones("user-123") } returns flowOf(sampleNotificaciones)
        coEvery { notificacionRepository.marcarLeida(any()) } just runs
        coEvery { notificacionRepository.marcarTodasLeidas(any()) } just runs

        viewModel = BandejaViewModel(notificacionRepository, authRepository)
    }

    @Test
    fun `init loads notificaciones`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(3, state.notificaciones.size)
            assertEquals(2, state.unreadCount)
        }
    }

    @Test
    fun `marcarLeida marks single notification as read`() = runTest {
        viewModel.marcarLeida(sampleNotificaciones[0])

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.notificaciones.find { it.id == "n1" }?.read ?: false)
            assertEquals(1, state.unreadCount)
        }
        coVerify { notificacionRepository.marcarLeida("n1") }
    }

    @Test
    fun `marcarLeida does nothing if already read`() = runTest {
        viewModel.marcarLeida(sampleNotificaciones[1])

        coVerify(inverse = true) { notificacionRepository.marcarLeida(any()) }
    }

    @Test
    fun `marcarTodasLeidas marks all as read`() = runTest {
        viewModel.marcarTodasLeidas()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.notificaciones.all { it.read })
            assertEquals(0, state.unreadCount)
        }
        coVerify { notificacionRepository.marcarTodasLeidas("user-123") }
    }

    @Test
    fun `cargarNotificaciones handles null userId`() {
        every { authRepository.getCurrentUserId() } returns null

        viewModel.cargarNotificaciones()

        assertTrue(viewModel.uiState.value.isLoading)
    }
}
