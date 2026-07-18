package com.CYRYEL.com

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun app_packageNameIsCorrect() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.CYRYEL.com", appContext.packageName)
    }

    @Test
    fun app_hasCorrectActivity() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = appContext.packageManager
            .getLaunchIntentForPackage("com.CYRYEL.com")
        assertEquals("com.CYRYEL.com.MainActivity", intent?.component?.className)
    }
}
