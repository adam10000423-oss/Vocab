package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeLeft
import com.example.ui.components.FlipCard
import com.example.data.entity.Flashcard
import org.junit.Assert.assertEquals
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun mainNavigation_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        MainScreen(
          currentTab = 0,
          onTabSelected = {},
          dashboardContent = { SampleContent() },
          foldersContent = { Text("資料夾") },
          studyContent = { Text("學習") },
          quizContent = { Text("遊戲") }
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun darkNavigationAndHomeOnlyModeSwitch() {
    composeTestRule.setContent {
      var page by remember { mutableIntStateOf(0) }
      MyApplicationTheme(darkTheme = true) {
        MainScreen(
          currentTab = page,
          onTabSelected = { page = it },
          dashboardContent = { SampleContent() },
          foldersContent = { SampleContent() },
          studyContent = {},
          quizContent = {}
        )
      }
    }
    composeTestRule.onNodeWithText("文法").assertIsDisplayed()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/modern-dark.png")
    composeTestRule.onNodeWithTag("nav_tab_folders").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("文法").assertDoesNotExist()
  }

  @androidx.compose.runtime.Composable
  private fun SampleContent() {
    LazyColumn(
      contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 120.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(12) { index ->
        Card(Modifier.fillMaxWidth()) {
          Column(Modifier.padding(20.dp)) {
            Text("Learning collection ${index + 1}", style = MaterialTheme.typography.titleLarge)
            Text("40 words · Continue learning", style = MaterialTheme.typography.bodyLarge)
          }
        }
      }
    }
  }

  @Test
  fun learningCardUsesDirectionalGestures() {
    var remembered = 0
    var unfamiliar = 0
    composeTestRule.setContent {
      MyApplicationTheme {
        FlipCard(
          card = Flashcard(id = 1, deckId = 1, word = "learn", definition = "學習"),
          isFlipped = false,
          onFlip = {},
          onSpeak = {},
          onToggleFavorite = {},
          onSwipeLeft = { unfamiliar++ },
          onSwipeRight = { remembered++ }
        )
      }
    }
    composeTestRule.onNodeWithTag("flashcard_flip_card").performTouchInput { swipeRight() }
    composeTestRule.runOnIdle { assertEquals(1, remembered); assertEquals(0, unfamiliar) }
    composeTestRule.onNodeWithTag("flashcard_flip_card").performTouchInput { swipeLeft() }
    composeTestRule.runOnIdle { assertEquals(1, remembered); assertEquals(1, unfamiliar) }
    composeTestRule.onNodeWithTag("rating_button_again").assertDoesNotExist()
    composeTestRule.onNodeWithTag("rating_button_good").assertDoesNotExist()
  }
}
