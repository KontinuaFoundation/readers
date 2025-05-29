package com.kontinua.readersandroidjetpack.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kontinua.readersandroidjetpack.R
import com.kontinua.readersandroidjetpack.serialization.WorkbookPreview
import com.kontinua.readersandroidjetpack.util.NavbarManager

@Composable
fun UnifiedSidebar(navbarManager: NavbarManager) {
    // common duration + easing
    val duration = 300
    val easing = FastOutSlowInEasing

    // Dp-based spec for padding animation
    val sidebarSpecDp: FiniteAnimationSpec<Dp> =
        tween(durationMillis = duration, easing = easing)

    // IntOffset-based spec for slideIn/slideOut
    val sidebarSpecOffset: FiniteAnimationSpec<IntOffset> =
        tween(durationMillis = duration, easing = easing)

    val animatedChapterSidebarWidth by animateDpAsState(
        targetValue = if (navbarManager.isWorkbookVisible) 200.dp else 0.dp,
        animationSpec = sidebarSpecDp,
        label = "chapterSidebarWidth"
    )

    Box(Modifier.fillMaxSize()) {
        // dimmer
        AnimatedVisibility(
            visible = navbarManager.isChapterVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { navbarManager.closeSidebar() }
                    .zIndex(1f)
            )
        }

        // Chapter Sidebar
        AnimatedVisibility(
            visible = navbarManager.isChapterVisible,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = sidebarSpecOffset
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = sidebarSpecOffset
            ),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = animatedChapterSidebarWidth)
        ) {
            ChapterSidebar(
                onClose = { navbarManager.closeSidebar() },
                onButtonClick = { navbarManager.toggleWorkbookSidebar() },
                navbarManager = navbarManager
            )
        }

        // Workbook Sidebar
        AnimatedVisibility(
            visible = navbarManager.isWorkbookVisible,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = sidebarSpecOffset
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = sidebarSpecOffset
            ),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            WorkbookSidebar(
                onClose = { navbarManager.closeSidebar() },
                navbarManager = navbarManager
            )
        }
    }
}

@Composable
fun ChapterSidebar(
    onClose: () -> Unit,
    onButtonClick: () -> Unit,
    navbarManager: NavbarManager
) {
    val collectionVM = navbarManager.collectionVM!!
    val chapters = collectionVM.chapters
    val scroll = rememberScrollState()
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .width(350.dp)
            .background(Color.White)
            .fillMaxHeight()
            .padding(
                start = 16.dp,
                top = 32.dp,
                end = 10.dp,
                bottom = 10.dp
            )
            .verticalScroll(scroll)
            .clickable( // consume clicks
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* no-op, prevents clicks from propagating */ },
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // ← back‐to‐workbooks button
        WorkbookButton(
            onClick = onButtonClick,
            isWorkbookVisible = navbarManager.isWorkbookVisible
        )

        // ← search bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search icon"
                )
            },
            placeholder = {
                Text("Search chapters and words")
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF0F0F0),
                focusedContainerColor = Color(0xFFF0F0F0)
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // ← “Chapters” heading
        Text(
            text = stringResource(R.string.chapter_heading),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(top = 8.dp, bottom = 4.dp)
        )

        ListingDivider()

        // ← Each chapter + divider
        chapters.forEachIndexed { i, chapter ->
            val bgColor =
                if (i == navbarManager.currentChapterIndex) {
                    Color.LightGray
                } else {
                    Color.Transparent
                }

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
                    .clickable {
                        collectionVM.setWorkbook(collectionVM.currentWorkbook)
                        //this line fixed the page issue
                        navbarManager.setPage(chapter.startPage - 1)
                        onClose()
                    }
                    .padding(vertical = 3.dp, horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = stringResource(R.string.chapter_num, chapter.chapNum),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = stringResource(R.string.chapter_title, chapter.title),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            ListingDivider()
        }
    }
}

@Composable
fun ListingDivider() {
    Divider(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 1.dp),
        color = Color.LightGray,
        thickness = 1.dp
    )
}

@Composable
fun WorkbookSidebar(onClose: () -> Unit, navbarManager: NavbarManager) {
    val collectionVM = navbarManager.collectionVM
    val collection = collectionVM!!.collectionState.collectAsState()
    val workbooks: List<WorkbookPreview> = collection.value!!.workbooks
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .width(200.dp)
            .background(Color.White)
            .drawWithContent {
                drawContent()
                val stroke = with(density) { 1.dp.toPx() }
                drawLine(
                    color = Color.LightGray,
                    start = Offset(size.width - stroke / 2, 0f),
                    end = Offset(size.width - stroke / 2, size.height),
                    strokeWidth = stroke
                )
            }
            .fillMaxHeight()
            .padding(
                start = 16.dp,
                top = 86.dp,
                end = 10.dp,
                bottom = 10.dp
            )
            .verticalScroll(state = scroll)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Prevent clicks from propagating */ },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (workbook in workbooks) {
            val bgColor =
                if (workbook == collectionVM.currentWorkbook) {
                    Color.LightGray
                } else {
                    Color.Transparent
                }
            Text(
                "Workbook ${workbook.number}",
                modifier = Modifier
                    .clickable {
                        collectionVM.setWorkbook(workbook)
                        navbarManager.setPage(0)
                        onClose()
                    }
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
            )
        }
    }
}

@Composable
fun WorkbookButton(
    onClick: () -> Unit,
    isWorkbookVisible: Boolean,
    modifier: Modifier = Modifier
) {
    // animate between 0° (closed) and 180° (open)
    val rotation by animateFloatAsState(
        targetValue = if (isWorkbookVisible) 180f else 0f,
        animationSpec = tween(durationMillis = 250)
    )

    TextButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 0.dp, minHeight = 0.dp),
        contentPadding = PaddingValues(0.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.textButtonColors()
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowForwardIos,
            contentDescription = "Toggle workbooks",
            modifier = Modifier.rotate(rotation)
        )
        Spacer(Modifier.width(8.dp))
        Text("Workbooks")
    }
}
