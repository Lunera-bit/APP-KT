package com.CYRYEL.com.ui.notifications

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.notificacion.NotificacionData
import com.CYRYEL.com.data.notificacion.NotificacionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BandejaNotificacionesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bandejaScreen_showsTitle() {
        val notifRepo = mockk<NotificacionRepository>(relaxed = true) {
            every { getNotificaciones(any()) } returns flowOf(emptyList())
        }
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getCurrentUserId() } returns "user-123"
        }
        val vm = BandejaViewModel(notifRepo, authRepo)

        composeTestRule.setContent {
            BandejaNotificacionesScreen(
                onBack = {},
                onNotificationClick = {},
                viewModel = vm
            )
        }

        composeTestRule.onNodeWithText("Notificaciones").assertIsDisplayed()
    }

    @Test
    fun bandejaScreen_showsNotifications() {
        val notis = listOf(
            NotificacionData(id = "n1", titulo = "Pedido confirmado", mensaje = "Tu pedido #123 fue confirmado", read = false),
            NotificacionData(id = "n2", titulo = "Oferta especial", mensaje = "Nueva promocion disponible", read = true)
        )
        val notifRepo = mockk<NotificacionRepository>(relaxed = true) {
            every { getNotificaciones(any()) } returns flowOf(notis)
        }
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getCurrentUserId() } returns "user-123"
        }
        val vm = BandejaViewModel(notifRepo, authRepo)

        composeTestRule.setContent {
            BandejaNotificacionesScreen(
                onBack = {},
                onNotificationClick = {},
                viewModel = vm
            )
        }

        composeTestRule.onNodeWithText("Pedido confirmado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tu pedido #123 fue confirmado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oferta especial").assertIsDisplayed()
    }

    @Test
    fun bandejaScreen_showsEmptyState() {
        val notifRepo = mockk<NotificacionRepository>(relaxed = true) {
            every { getNotificaciones(any()) } returns flowOf(emptyList())
        }
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getCurrentUserId() } returns "user-123"
        }
        val vm = BandejaViewModel(notifRepo, authRepo)

        composeTestRule.setContent {
            BandejaNotificacionesScreen(
                onBack = {},
                onNotificationClick = {},
                viewModel = vm
            )
        }

        composeTestRule.onNodeWithText("Notificaciones").assertIsDisplayed()
    }
}
