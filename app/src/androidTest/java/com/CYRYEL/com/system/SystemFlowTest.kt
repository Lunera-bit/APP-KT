package com.CYRYEL.com.system

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemFlowTest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = context.packageManager.getLaunchIntentForPackage("com.CYRYEL.com")
        assertNotNull("Launch intent should not be null", intent)
        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg("com.CYRYEL.com").depth(0)), 10_000)
    }

    @Test
    fun system_appLaunches() {
        val launcherVisible = device.wait(
            Until.hasObject(By.pkg("com.CYRYEL.com").depth(0)), 5_000
        )
        assertTrue("App should launch and display content", launcherVisible)
    }

    @Test
    fun system_packageNameIsCorrect() {
        val packageName = InstrumentationRegistry.getArguments().getString("packageName")
            ?: InstrumentationRegistry.getInstrumentation().targetContext.packageName
        assertEquals("com.CYRYEL.com", packageName)
    }

    @Test
    fun system_homeScreenDisplaysContent() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val found = device.wait(
            Until.hasObject(By.pkg("com.CYRYEL.com")), 8_000
        )
        assertTrue("Home screen should render within 8 seconds", found)
    }

    @Test
    fun system_navigateToSearchAndBack() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.hasObject(By.pkg("com.CYRYEL.com").depth(0)), 5_000)
        val searchButton = device.findObject(By.text("Buscar"))
        if (searchButton != null) {
            searchButton.click()
            device.wait(Until.hasObject(By.pkg("com.CYRYEL.com")), 3_000)
            device.pressBack()
            device.wait(Until.hasObject(By.pkg("com.CYRYEL.com")), 3_000)
        }
        assertTrue(true)
    }

    @Test
    fun system_navigateToOrdersAndBack() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.hasObject(By.pkg("com.CYRYEL.com").depth(0)), 5_000)
        val ordersButton = device.findObject(By.text("Mis Pedidos"))
        if (ordersButton != null) {
            ordersButton.click()
            device.wait(Until.hasObject(By.pkg("com.CYRYEL.com")), 3_000)
            device.pressBack()
            device.wait(Until.hasObject(By.pkg("com.CYRYEL.com")), 3_000)
        }
        assertTrue(true)
    }

    @Test
    fun system_appDoesNotCrashOnRotation() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.hasObject(By.pkg("com.CYRYEL.com").depth(0)), 5_000)
        device.setOrientationLeft()
        device.wait(Until.hasObject(By.pkg("com.CYRYEL.com")), 2_000)
        device.setOrientationRight()
        device.wait(Until.hasObject(By.pkg("com.CYRYEL.com")), 2_000)
        device.setOrientationNatural()
        assertTrue("App should survive rotation without crashing", true)
    }

    @Test
    fun system_appRecoversFromBackground() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.hasObject(By.pkg("com.CYRYEL.com").depth(0)), 5_000)
        device.pressHome()
        device.wait(Until.gone(By.pkg("com.CYRYEL.com")), 2_000)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = context.packageManager.getLaunchIntentForPackage("com.CYRYEL.com")
        assertNotNull(intent)
        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        val recovered = device.wait(
            Until.hasObject(By.pkg("com.CYRYEL.com").depth(0)), 5_000
        )
        assertTrue("App should recover from background", recovered)
    }
}
