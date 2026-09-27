package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.rememberResponsiveLayout
import kotlinx.coroutines.flow.distinctUntilChanged

private data class MainTabSpec(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    dashboardContent: @Composable () -> Unit,
    foldersContent: @Composable () -> Unit,
    studyContent: @Composable () -> Unit,
    quizContent: @Composable () -> Unit,
    onOpenAssistant: () -> Unit = {},
    onOpenWordSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    grammarMode: Boolean = false,
    onToggleGrammarMode: () -> Unit = {},
    updateAvailable: Boolean = false,
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
    val tabs = if (grammarMode) {
        listOf(
            MainTabSpec("總覽", Icons.Default.Home, "總覽", "nav_tab_home"),
            MainTabSpec("文法庫", Icons.Default.MenuBook, "文法庫", "nav_tab_folders"),
            MainTabSpec("學習", Icons.Default.Style, "學習", "nav_tab_study"),
            MainTabSpec("測驗", Icons.Default.Quiz, "測驗", "nav_tab_quiz")
        )
    } else {
        listOf(
            MainTabSpec("主畫面", Icons.Default.Home, "主畫面", "nav_tab_home"),
            MainTabSpec("資料夾", Icons.Default.Folder, "資料夾", "nav_tab_folders"),
            MainTabSpec("學習", Icons.Default.Style, "學習", "nav_tab_study"),
            MainTabSpec("測驗", Icons.Default.Extension, "測驗", "nav_tab_quiz")
        )
    }

    LaunchedEffect(currentTab) {
        if (pagerState.currentPage != currentTab) {
            pagerState.scrollToPage(currentTab)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                if (page != currentTab) {
                    onTabSelected(page)
                }
            }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showBottomNavigation) {
                TopAppBar(
                    expandedHeight = 48.dp,
                    windowInsets = TopAppBarDefaults.windowInsets,
                    title = {
                        Crossfade(
                            targetState = pagerState.currentPage to grammarMode,
                            label = "main_page_title"
                        ) { (page, grammar) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Surface(
                                    onClick = onToggleGrammarMode,
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.SwapHoriz,
                                            contentDescription = "切換單字與文法",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (grammar) "文法" else "單字", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = if (grammar) {
                                        when (page) {
                                            0 -> "文法總覽"
                                            1 -> "文法庫"
                                            2 -> "學習"
                                            else -> "測驗"
                                        }
                                    } else {
                                        when (page) {
                                            0 -> "Vocab"
                                            1 -> "資料夾"
                                            2 -> "學習"
                                            else -> "測驗"
                                        }
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        if (!grammarMode) {
                            IconButton(onClick = onOpenWordSearch) {
                                Icon(Icons.Default.Search, contentDescription = "搜尋英文單字")
                            }
                        }
                        IconButton(onClick = onOpenAssistant) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Vocab AI",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "設定",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (updateAvailable) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(8.dp)
                                            .background(MaterialTheme.colorScheme.error, CircleShape)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (showBottomNavigation) NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = { onTabSelected(index) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.contentDescription) },
                        label = if (showNavigationLabels) {
                            {
                                Text(
                                    tab.label,
                                    fontWeight = if (currentTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        } else null,
                        alwaysShowLabel = showNavigationLabels,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = showBottomNavigation,
            modifier = Modifier
                .padding(innerPadding)
                .padding(top = 6.dp)
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
