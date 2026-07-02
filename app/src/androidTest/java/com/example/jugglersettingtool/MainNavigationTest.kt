package com.example.jugglersettingtool

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.jugglersettingtool.theme.JugglerSettingToolTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        composeTestRule.setContent {
            JugglerSettingToolTheme {
                MainNavigation()
            }
        }
    }

    @Test
    fun counterScreen_isShownInitially() {
        composeTestRule.onNodeWithText("カウンター & 設定推測").fetchSemanticsNode()
        composeTestRule.onNodeWithText("対象機種").fetchSemanticsNode()
    }

    @Test
    fun bottomNavigation_opensSettingsScreen() {
        composeTestRule.onNodeWithText("設定").performClick()
        composeTestRule.onNodeWithText("設定 & ログ管理").fetchSemanticsNode()
    }
}
