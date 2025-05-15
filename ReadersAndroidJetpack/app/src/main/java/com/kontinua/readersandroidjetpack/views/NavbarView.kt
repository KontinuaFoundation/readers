package com.kontinua.readersandroidjetpack.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kontinua.readersandroidjetpack.R
import com.kontinua.readersandroidjetpack.serialization.Chapter
import com.kontinua.readersandroidjetpack.serialization.WorkbookPreview
import com.kontinua.readersandroidjetpack.util.AnnotationManager
import com.kontinua.readersandroidjetpack.util.NavbarManager
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel

@Composable
fun SidebarWithPDFViewer(
    navbarManager: NavbarManager,
    collectionViewModel: CollectionViewModel,
    annotationManager: AnnotationManager
) {
    val density = LocalDensity.current

    val animatedChapterSidebarWidth by animateDpAsState(
        targetValue = if (navbarManager.isWorkbookVisible) 200.dp else 0.dp,
        label = "chapterSidebarWidthAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // Transparent clickable overlay.
        if (navbarManager.isChapterVisible) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(navbarManager.isChapterVisible) {
                        detectTapGestures { offset ->
                            if (navbarManager.isChapterVisible) {
                                val sidebarWidthPx = with(density) { animatedChapterSidebarWidth.toPx() }
                                // Check if tap is outside the sidebar
                                if (offset.x > sidebarWidthPx) {
                                    navbarManager.closeSidebar()
                                }
                            }
                        }
                    }
            )
        }

        // Chapter Sidebar
        AnimatedVisibility(
            visible = navbarManager.isChapterVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = animatedChapterSidebarWidth)
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
            enter = slideInHorizontally(initialOffsetX = { -it }), // Slide from left
            exit = slideOutHorizontally(targetOffsetX = { -it }), // Slide to left
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

    Column(
        modifier = Modifier
            .width(350.dp)
            .background(Color.White)
            .border(1.dp, Color.DarkGray)
            .fillMaxHeight()
            .padding(
                start = 16.dp,
                top = 10.dp,
                end = 10.dp,
                bottom = 10.dp
            )
            .verticalScroll(scroll)
            .clickable( // consume clicks
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* no-op */ },
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // ← back‐to‐workbooks button
        WorkbookButton(
            onClick = onButtonClick,
            modifier = Modifier.align(Alignment.Start)
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
                if (i == navbarManager.currentChapterIndex) Color.LightGray
                else Color.Transparent

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .background(bgColor)
                    .clickable {
                        collectionVM.setWorkbook(collectionVM.currentWorkbook)
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
fun ListingDivider(){
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
            .border(1.dp, Color.DarkGray)
            .fillMaxHeight()
            .padding(48.dp)
            .verticalScroll(state = scroll)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Prevent clicks from propagating */ },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (workbook in workbooks) {
            Text(
                "Workbook ${workbook.number}",
                modifier = Modifier.clickable {
                    collectionVM.setWorkbook(workbook)
                    navbarManager.setPage(0)
                    onClose()
                }
            )
        }
    }
}

@Composable
fun WorkbookButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            // 1) allow it to shrink to exactly its content
            .defaultMinSize(minWidth = 0.dp, minHeight = 0.dp),
        // 2) remove all inset padding
        contentPadding = PaddingValues(0.dp),
        // 3) ditch the rounded shape so the ripple isn’t clipped like an oval
        shape = RectangleShape,
        colors = ButtonDefaults.textButtonColors() // still gives you ripple & disabled styles
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBackIosNew,
            contentDescription = "Back to Workbooks"
        )
        Spacer(Modifier.width(8.dp))
        Text("Workbooks")
    }
}
