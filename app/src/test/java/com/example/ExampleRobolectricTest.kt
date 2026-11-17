package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name and multi-language resources resolve cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Luma Player", context.getString(R.string.app_name))

        val ptConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("pt"))
        }
        val ptContext = context.createConfigurationContext(ptConfig)
        assertEquals("Início", ptContext.getString(R.string.nav_home))

        val esConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("es"))
        }
        val esContext = context.createConfigurationContext(esConfig)
        assertEquals("Inicio", esContext.getString(R.string.nav_home))

        val frConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("fr"))
        }
        val frContext = context.createConfigurationContext(frConfig)
        assertEquals("Accueil", frContext.getString(R.string.nav_home))
    }

    @Test
    fun `navigate across main tabs and switch language and theme in settings`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_home").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_search").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search_input").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_library").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("library_add_btn").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_history").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lang_chip_en").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("theme_chip_dark").performClick()
        composeRule.waitForIdle()
    }
}
