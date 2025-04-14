package com.kontinua.readersandroidjetpack.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.kontinua.readersandroidjetpack.R
import com.kontinua.readersandroidjetpack.serialization.Chapter
import com.kontinua.readersandroidjetpack.serialization.WorkbookPreview
import com.kontinua.readersandroidjetpack.util.AnnotationManager
import com.kontinua.readersandroidjetpack.util.NavbarManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarWithPDFViewer(navbarManager: NavbarManager, annotationManager: AnnotationManager) {
    val density = LocalDensity.current

    val animatedChapterSidebarWidth by animateDpAsState(
        targetValue = if (navbarManager.isWorkbookVisible) 200.dp else 0.dp,
        label = "chapterSidebarWidthAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // PDF Viewer
        PDFViewer(modifier = Modifier.fillMaxSize(),
                navbarManager = navbarManager,
                annotationManager = annotationManager)

        //Transparent clickable overlay.
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
                    })
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
            WorkbookSidebar(onClose = { navbarManager.closeSidebar() },
                navbarManager = navbarManager)
        }
    }
}

@Composable
fun ChapterSidebar(onClose: () -> Unit, onButtonClick: () -> Unit, navbarManager: NavbarManager) {
    val collectionVM = navbarManager.collectionVM
    val chapters: List<Chapter> = collectionVM?.chapters ?: emptyList()
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .width(250.dp)
            .background(Color.White)
            .fillMaxHeight()
            .padding(48.dp)
            .verticalScroll(state = scroll)
            .clickable(
                indication = null,
                interactionSource = remember   { MutableInteractionSource() }) { /* Prevent clicks from propagating */ },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WorkbookButton(onClick = onButtonClick)
        for (i in chapters.indices){
            val chapter = chapters[i]
            val bgColor = if(i == navbarManager.currentChapterIndex) Color.LightGray else Color.Transparent
            Text(stringResource(id = R.string.chapter_info, chapter.chapNum, chapter.title), modifier = Modifier
                .background(bgColor)
                .clickable {
                collectionVM?.setWorkbook(collectionVM.currentWorkbook)
                navbarManager.setPage(chapter.startPage - 1)
                onClose()
            })
        }
    }
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
            .fillMaxHeight()
            .padding(48.dp)
            .verticalScroll(state = scroll)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }) { /* Prevent clicks from propagating */ },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (workbook in workbooks) {
            Text("Workbook ${workbook.number}", modifier = Modifier.clickable {
                collectionVM.setWorkbook(workbook)
                navbarManager.setPage(0)
                navbarManager.setWorkbook("Workbook ${workbook.number}")
                onClose()
            })
        }
    }
}

@Composable
fun WorkbookButton(onClick: () -> Unit) {
    Button(onClick = { onClick() }) {
        Text("Workbooks")
    }
}