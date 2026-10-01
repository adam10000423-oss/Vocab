package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    libraryToolContent: @Composable () -> Unit = {},
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
    val pageLayer = rememberGraphicsLayer()
    var pagePosition by remember { mutableStateOf(Offset.Zero) }
    var barPosition by remember { mutableStateOf(Offset.Zero) }
    val showNavigationLabels = !responsive.isLandscape &&
        !responsive.isSmallWidth &&
        !responsive.isLargeText
    val pagerState = rememberPagerState(
        initialPage = currentTab,
        pageCount = { 5 }
    )
    val tabs = if (grammarMode) {
        listOf(
            MainTabSpec("總覽", Icons.Default.Home, "總覽", "nav_tab_home"),
            MainTabSpec("文法庫", Icons.Default.MenuBook, "文法庫", "nav_tab_folders"),
            MainTabSpec("學習", Icons.Default.Style, "學習", "nav_tab_study"),
            MainTabSpec("測驗", Icons.Default.Quiz, "測驗", "nav_tab_quiz"),
            MainTabSpec("作文", Icons.Default.EditNote, "作文", "nav_tab_library_tool")
        )
    } else {
        listOf(
            MainTabSpec("主畫面", Icons.Default.Home, "主畫面", "nav_tab_home"),
            MainTabSpec("資料夾", Icons.Default.Folder, "資料夾", "nav_tab_folders"),
            MainTabSpec("學習", Icons.Default.Style, "學習", "nav_tab_study"),
            MainTabSpec("測驗", Icons.Default.Extension, "測驗", "nav_tab_quiz"),
            MainTabSpec("文章", Icons.Default.Article, "文章", "nav_tab_library_tool")
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
                // Always report the settled page. The previous conditional captured
                // an old currentTab value and left the bottom bar behind after swipes.
                onTabSelected(page)
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
                            if (page == 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(Modifier.padding(3.dp)) {
                                        listOf(false to "單字", true to "文法").forEach { (mode, label) ->
                                            Surface(
                                                onClick = { if (grammar != mode) onToggleGrammarMode() },
                                                shape = CircleShape,
                                                color = if (grammar == mode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                contentColor = if (grammar == mode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            ) {
                                                Text(
                                                    label,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    tabs[page].label,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenWordSearch) {
                            Icon(Icons.Default.Search, contentDescription = if (grammarMode) "搜尋文法" else "搜尋英文單字")
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
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = showBottomNavigation,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { pagePosition = it.positionInRoot() }
                .drawWithContent {
                    pageLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(pageLayer)
                }
        ) { page ->
            when (page) {
                0 -> dashboardContent()
                1 -> foldersContent()
                2 -> studyContent()
                3 -> quizContent()
                4 -> libraryToolContent()
            }
        }

            if (showBottomNavigation) Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.fillMaxWidth()
                        .shadow(20.dp, RoundedCornerShape(34.dp))
                        .clip(RoundedCornerShape(34.dp))
                        .onGloballyPositioned { barPosition = it.positionInRoot() }
                ) {
                Canvas(
                    Modifier.matchParentSize().graphicsLayer {
                        renderEffect = BlurEffect(24f, 24f)
                    }
                ) {
                    val delta = pagePosition - barPosition
                    with(drawContext.canvas) {
                        save()
                        translate(delta.x, delta.y)
                        drawLayer(pageLayer)
                        restore()
                    }
                }
                Surface(
                    shape = RoundedCornerShape(34.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(70.dp).padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val selected = pagerState.currentPage == index
                            Surface(
                                onClick = { onTabSelected(index) },
                                shape = RoundedCornerShape(28.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                } else Color.Transparent,
                                contentColor = if (selected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .testTag(tab.testTag)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.contentDescription,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    if (showNavigationLabels) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            tab.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }

    }
}
