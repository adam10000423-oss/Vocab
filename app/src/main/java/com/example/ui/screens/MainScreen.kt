package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.components.rememberResponsiveLayout
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun MainScreen(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    dashboardContent: @Composable () -> Unit,
    foldersContent: @Composable () -> Unit,
    studyContent: @Composable () -> Unit,
    quizContent: @Composable () -> Unit,
    showBottomNavigation: Boolean = true,
    modifier: Modifier = Modifier
) {
    val responsive = rememberResponsiveLayout()
    val showNavigationLabels = !responsive.isLandscape &&
        !responsive.isSmallWidth &&
        !responsive.isLargeText
    val pagerState = rememberPagerState(
        initialPage = currentTab,
        pageCount = { 4 }
    )

    LaunchedEffect(currentTab) {
        if (pagerState.currentPage != currentTab) {
            pagerState.animateScrollToPage(currentTab)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                onTabSelected(page)
            }
    }

    Scaffold(
        bottomBar = {
            if (showBottomNavigation) NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                // Tab 0: 主畫面
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { onTabSelected(0) },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "主畫面") },
                    label = if (showNavigationLabels) {
                        { Text("主畫面", fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    } else null,
                    alwaysShowLabel = showNavigationLabels,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_home")
                )

                // Tab 1: 資料夾
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { onTabSelected(1) },
                    icon = { Icon(imageVector = Icons.Default.Folder, contentDescription = "資料夾") },
                    label = if (showNavigationLabels) {
                        { Text("資料夾", fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    } else null,
                    alwaysShowLabel = showNavigationLabels,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_folders")
                )

                // Tab 2: 學習
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { onTabSelected(2) },
                    icon = { Icon(imageVector = Icons.Default.Style, contentDescription = "學習") },
                    label = if (showNavigationLabels) {
                        { Text("學習", fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    } else null,
                    alwaysShowLabel = showNavigationLabels,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_study")
                )

                // Tab 3: 遊戲測驗
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { onTabSelected(3) },
                    icon = { Icon(imageVector = Icons.Default.Extension, contentDescription = "測驗") },
                    label = if (showNavigationLabels) {
                        { Text("測驗", fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal) }
                    } else null,
                    alwaysShowLabel = showNavigationLabels,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_quiz")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = showBottomNavigation,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) { page ->
            when (page) {
                0 -> dashboardContent()
                1 -> foldersContent()
                2 -> studyContent()
                3 -> quizContent()
            }
        }
    }
}
