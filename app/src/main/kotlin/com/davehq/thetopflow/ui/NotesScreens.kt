@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class
)

package com.davehq.thetopflow.ui

import androidx.activity.compose.BackHandler
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.metrics.performance.PerformanceMetricsState
import com.davehq.thetopflow.BuildConfig
import com.davehq.thetopflow.MediaUiState
import com.davehq.thetopflow.NotesUiState
import com.davehq.thetopflow.data.NoteUi
import com.davehq.thetopflow.data.StyleDefaults
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min
import java.text.DateFormat
import java.util.Date

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun NotesRoute(
    state: NotesUiState,
    onCreateNote: () -> Unit,
    onOpenNote: (String) -> Unit,
    onCloseEditor: () -> Unit,
    onSearch: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onDeleteNote: () -> Unit,
    onOpenRecentNote: () -> Unit,
    onAttachSong: () -> Unit,
    onToggleSong: () -> Unit,
    onSeekSong: (Int) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: (Boolean) -> Unit,
    onPlayRecording: (String) -> Unit,
    onRenameRecording: (String, String) -> Unit,
    onExportRecording: (String) -> Boolean,
    onApplyStyle: (String, Int, Int, Int, Int, Int, Int, Boolean) -> Unit
) {
    val gridState = rememberLazyStaggeredGridState()
    val isScrolling by remember { derivedStateOf { gridState.isScrollInProgress } }
    val screenLabel = when {
        state.selectedNote != null && state.isCreating -> "CreateNote"
        state.selectedNote != null -> "NoteEditor"
        state.query.isNotBlank() -> "Search"
        else -> "NotesGrid"
    }
    JankStateLabels(screenLabel = screenLabel, isScrolling = isScrolling)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics { testTagsAsResourceId = true },
        color = MaterialTheme.colorScheme.background
    ) {
        if (state.selectedNote == null) {
            NotesGridScreen(
                state = state,
                gridState = gridState,
                onCreateNote = onCreateNote,
                onOpenNote = onOpenNote,
                onSearch = onSearch,
                onOpenRecentNote = onOpenRecentNote
            )
        } else {
            NoteEditorScreen(
                note = state.selectedNote,
                isCreating = state.isCreating,
                media = state.media,
                styleDefaults = state.styleDefaults,
                rhymeSuggestions = state.rhymeSuggestions,
                rhymeLoading = state.rhymeLoading,
                onBack = onCloseEditor,
                onTitleChange = onTitleChange,
                onBodyChange = onBodyChange,
                onDeleteNote = onDeleteNote,
                onCreateNote = onCreateNote,
                onOpenRecentNote = onOpenRecentNote,
                onAttachSong = onAttachSong,
                onToggleSong = onToggleSong,
                onSeekSong = onSeekSong,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onPlayRecording = onPlayRecording,
                onRenameRecording = onRenameRecording,
                onExportRecording = onExportRecording,
                onApplyStyle = onApplyStyle
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun NotesGridScreen(
    state: NotesUiState,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    onCreateNote: () -> Unit,
    onOpenNote: (String) -> Unit,
    onSearch: (String) -> Unit,
    onOpenRecentNote: () -> Unit
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val gesturePx = with(density) { 96.dp.toPx() }
    val menuColor = Color(state.styleDefaults.menuColor)
    val menuAccent = Color(state.styleDefaults.menuAccentColor)
    var showMainMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val runMainAction: (() -> Unit) -> Unit = { action ->
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        action()
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(menuColor.copy(alpha = 0.42f))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "The Top Flow",
                            style = MaterialTheme.typography.headlineMedium,
                            color = readableColor(menuColor, menuAccent, MaterialTheme.colorScheme.onBackground),
                            maxLines = 1
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = "${state.notes.size} notes",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Box {
                        OutlinedButton(onClick = { showMainMenu = true }) {
                            Text("Menu")
                        }
                        DropdownMenu(
                            expanded = showMainMenu,
                            onDismissRequest = { showMainMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New note") },
                                onClick = { showMainMenu = false; runMainAction(onCreateNote) }
                            )
                            DropdownMenuItem(
                                text = { Text("Open latest note") },
                                onClick = { showMainMenu = false; runMainAction(onOpenRecentNote) }
                            )
                            DropdownMenuItem(
                                text = { Text("Check for app update") },
                                onClick = {
                                    showMainMenu = false
                                    runMainAction { launchTopFlowUpdate(context) }
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SearchField(
                    value = state.query,
                    onValueChange = onSearch,
                    accent = menuAccent,
                    menuColor = menuColor
                )
            }
        },
        bottomBar = {
            val focusManager = LocalFocusManager.current
            val keyboard = LocalSoftwareKeyboardController.current
            val clearInput = { action: () -> Unit ->
                focusManager.clearFocus(force = true)
                keyboard?.hide()
                action()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(menuColor.copy(alpha = 0.38f))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    modifier = Modifier.testTag("create_note"),
                    onClick = { clearInput(onCreateNote) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = menuAccent.copy(alpha = 0.28f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("New note")
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .topFlowHorizontalSwipe(
                        enabled = true,
                        direction = SwipeDirection.Left,
                        startZoneFraction = 1f,
                        thresholdPx = gesturePx,
                        onSwipe = onOpenRecentNote
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading notes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        if (state.visibleNotes.isEmpty()) {
            EmptyNotesState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                isSearching = state.query.isNotBlank(),
                onCreateNote = onCreateNote
            )
            return@Scaffold
        }

        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .topFlowHorizontalSwipe(
                    enabled = true,
                    direction = SwipeDirection.Left,
                    startZoneFraction = 1f,
                    thresholdPx = gesturePx,
                    onSwipe = onOpenRecentNote
                )
                .testTag("notes_grid"),
            state = gridState,
            columns = StaggeredGridCells.Adaptive(172.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp
        ) {
            items(
                items = state.visibleNotes,
                key = { note -> note.id },
                contentType = { "note-card" }
            ) { note ->
                NoteCard(
                    note = note,
                    selected = false,
                    onClick = { onOpenNote(note.id) }
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color = MaterialTheme.colorScheme.primary,
    menuColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("search_notes")
            .semantics { contentDescription = "Search notes" },
        shape = MaterialTheme.shapes.extraLarge,
        color = menuColor.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(
                            text = "Search notes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun NoteCard(
    note: NoteUi,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val accent = Color(note.accentColor)
    val pageColor = Color(note.noteColor).copy(alpha = 0.24f)
    val titleColor = readableColor(pageColor, Color(note.textColor), MaterialTheme.colorScheme.onSurface)
    val bodyColor = readableColor(pageColor, Color(note.textColor), MaterialTheme.colorScheme.onSurfaceVariant)
    val previewTextStyle = editorBodyStyle(note, bodyColor)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("note_card")
            .semantics { contentDescription = "Note card ${note.title}" }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(pageColor)
                .border(
                    width = 1.dp,
                    color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = noteFontFamily(note.font), color = titleColor),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AnimatedVisibility(visible = note.preview.isNotBlank()) {
                Text(
                    text = note.preview,
                    modifier = Modifier.padding(top = 12.dp),
                    style = previewTextStyle,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "${note.wordCount} words · ${shortDate(note.updatedAt)}",
                style = MaterialTheme.typography.labelMedium.copy(color = readableMetadataColor(Color(note.noteColor), titleColor)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (note.songUri.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Song linked",
                    style = MaterialTheme.typography.labelSmall.copy(color = accent)
                )
            }
            note.recordings.firstOrNull()?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Voice notes: ${note.recordings.size}",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

@Composable
fun NoteEditorScreen(
    note: NoteUi,
    isCreating: Boolean,
    media: MediaUiState,
    styleDefaults: StyleDefaults,
    rhymeSuggestions: List<String>,
    rhymeLoading: Boolean,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onDeleteNote: () -> Unit,
    onCreateNote: () -> Unit,
    onOpenRecentNote: () -> Unit,
    onAttachSong: () -> Unit,
    onToggleSong: () -> Unit,
    onSeekSong: (Int) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: (Boolean) -> Unit,
    onPlayRecording: (String) -> Unit,
    onRenameRecording: (String, String) -> Unit,
    onExportRecording: (String) -> Boolean,
    onApplyStyle: (String, Int, Int, Int, Int, Int, Int, Boolean) -> Unit
) {
    BackHandler(onBack = onBack)
    NoteEditor(
        note = note,
        isCreating = isCreating,
        media = media,
        styleDefaults = styleDefaults,
        rhymeSuggestions = rhymeSuggestions,
        rhymeLoading = rhymeLoading,
        onBack = onBack,
        onTitleChange = onTitleChange,
        onBodyChange = onBodyChange,
        onDeleteNote = onDeleteNote,
        onCreateNote = onCreateNote,
        onOpenRecentNote = onOpenRecentNote,
        onAttachSong = onAttachSong,
        onToggleSong = onToggleSong,
        onSeekSong = onSeekSong,
        onStartRecording = onStartRecording,
        onStopRecording = onStopRecording,
        onPlayRecording = onPlayRecording,
        onRenameRecording = onRenameRecording,
        onExportRecording = onExportRecording,
        onApplyStyle = onApplyStyle
    )
}

@Composable
fun NoteEditor(
    note: NoteUi,
    isCreating: Boolean,
    media: MediaUiState,
    styleDefaults: StyleDefaults = StyleDefaults(),
    rhymeSuggestions: List<String> = emptyList(),
    rhymeLoading: Boolean = false,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onTitleChange: (String) -> Unit = {},
    onBodyChange: (String) -> Unit = {},
    onDeleteNote: () -> Unit = {},
    onCreateNote: () -> Unit = {},
    onOpenRecentNote: () -> Unit = {},
    onAttachSong: () -> Unit = {},
    onToggleSong: () -> Unit = {},
    onSeekSong: (Int) -> Unit = {},
    onStartRecording: () -> Unit = {},
    onStopRecording: (Boolean) -> Unit = {},
    onPlayRecording: (String) -> Unit = {},
    onRenameRecording: (String, String) -> Unit = { _, _ -> },
    onExportRecording: (String) -> Boolean = { false },
    onApplyStyle: (String, Int, Int, Int, Int, Int, Int, Boolean) -> Unit = { _, _, _, _, _, _, _, _ -> }
) {
    val swipeThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var showStyleSheet by remember { mutableStateOf(false) }
    var showEditorMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var renameRecordingPath by remember { mutableStateOf<String?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var draftTitle by remember(note.id) { mutableStateOf(note.title) }
    var draftBody by remember(note.id) { mutableStateOf(note.body) }

    LaunchedEffect(note.id, note.title) {
        if (draftTitle != note.title) draftTitle = note.title
    }
    LaunchedEffect(note.id, note.body) {
        if (draftBody != note.body) draftBody = note.body
    }

    val runAction: (() -> Unit) -> Unit = { action ->
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        action()
    }

    val pageColor = Color(note.noteColor)
    val accentColor = Color(note.accentColor)
    val bodyTextColor = readableColor(pageColor, Color(note.textColor), MaterialTheme.colorScheme.onSurface)

    Column(
        modifier = modifier
            .fillMaxSize()
            .topFlowHorizontalSwipe(
                enabled = true,
                direction = SwipeDirection.Right,
                startZoneFraction = 0.4f,
                thresholdPx = swipeThresholdPx,
                onSwipe = { runAction { onBack() } }
            )
            .background(materialEditorSurface(note.noteColor))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .imePadding()
            .testTag("note_editor")
            .semantics { contentDescription = "Note editor" }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelLarge,
                        color = readableMetadataColor(pageColor, bodyTextColor),
                        maxLines = 1
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { runAction(onBack) }, modifier = Modifier.testTag("close_editor")) {
                        Text("Notes")
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = { runAction { showStyleSheet = true } }, modifier = Modifier.testTag("style_editor")) {
                        Text("Style")
                    }
                    AnimatedVisibility(visible = !isCreating) {
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { runAction(onDeleteNote) }, modifier = Modifier.testTag("delete_note")) {
                            Text("Delete")
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box {
                        OutlinedButton(onClick = { showEditorMenu = true }) {
                            Text("Menu")
                        }
                        DropdownMenu(
                            expanded = showEditorMenu,
                            onDismissRequest = { showEditorMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New note") },
                                onClick = { showEditorMenu = false; runAction(onCreateNote) }
                            )
                            DropdownMenuItem(
                                text = { Text("Open latest note") },
                                onClick = { showEditorMenu = false; runAction(onOpenRecentNote) }
                            )
                            DropdownMenuItem(
                                text = { Text("Check for app update") },
                                onClick = {
                                    showEditorMenu = false
                                    runAction { launchTopFlowUpdate(context) }
                                }
                            )
                        }
                    }
                }
            }
            item {
                BasicTextField(
                    value = draftTitle,
                    onValueChange = {
                        draftTitle = it
                        onTitleChange(it)
                    },
                    textStyle = editorTitleStyle(note, bodyTextColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_title")
                        .semantics { contentDescription = "Note title" },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = false,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (draftTitle.isBlank()) {
                                Text(
                                    text = "Untitled",
                                    style = editorTitleStyle(note, bodyTextColor.copy(alpha = 0.5f))
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${note.wordCount} words · ${shortDate(note.updatedAt)}",
                        style = MaterialTheme.typography.labelLarge.copy(color = readableMetadataColor(pageColor, bodyTextColor)),
                        maxLines = 1
                    )
                }
            }
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item {
                BasicTextField(
                    value = draftBody,
                    onValueChange = {
                        draftBody = it
                        onBodyChange(it)
                    },
                    textStyle = editorBodyStyle(note, bodyTextColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 420.dp)
                        .testTag("editor_body")
                        .semantics { contentDescription = "Note body" },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    decorationBox = { innerTextField ->
                        Box(Modifier.fillMaxWidth()) {
                            if (draftBody.isBlank()) {
                                Text(
                                    text = "Start writing...",
                                    style = editorBodyStyle(note, bodyTextColor.copy(alpha = 0.45f)),
                                    color = bodyTextColor.copy(alpha = 0.45f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
            item {
                RhymeSuggestionRow(
                    suggestions = rhymeSuggestions,
                    loading = rhymeLoading,
                    modifier = Modifier.fillMaxWidth(),
                    onInsertWord = { insertion ->
                        val next = draftBody + insertion
                        draftBody = next
                        onBodyChange(next)
                    }
                )
            }
            item {
                MediaPanel(
                    note = note,
                    media = media,
                    onAttach = { runAction(onAttachSong) },
                    onToggleSong = { runAction(onToggleSong) },
                    onSeekSong = onSeekSong
                )
            }
            item {
                VoicePanel(
                    note = note,
                    media = media,
                    onStartRecording = { runAction(onStartRecording) },
                    onStopRecording = { shouldSave -> runAction { onStopRecording(shouldSave) } },
                    onPlayRecording = { path -> runAction { onPlayRecording(path) } },
                    onRename = { path, tag -> runAction { renameRecordingPath = path; renameValue = tag } },
                    onExport = { path ->
                        var exported = false
                        runAction { exported = onExportRecording(path) }
                        exported
                    }
                )
            }
        }
    }

    if (showStyleSheet) {
        StyleEditorSheet(
            note = note,
            onDismiss = { runAction { showStyleSheet = false } },
            defaults = styleDefaults,
            onApply = { font, size, page, text, accent, menu, menuAccent, saveAsDefaults ->
                runAction {
                    onApplyStyle(font, size, page, text, accent, menu, menuAccent, saveAsDefaults)
                    showStyleSheet = false
                }
            },
            onCancel = { runAction { showStyleSheet = false } }
        )
    }

    if (renameRecordingPath != null) {
        RenameRecordingDialog(
            open = true,
            initialTag = renameValue,
            onConfirm = { newTag ->
                renameRecordingPath?.let { onRenameRecording(it, newTag) }
                renameRecordingPath = null
                renameValue = ""
            },
            onDismiss = {
                renameRecordingPath = null
                renameValue = ""
            },
            onTagChange = { renameValue = it }
        )
    }
}

@Composable
private fun MediaPanel(
    note: NoteUi,
    media: MediaUiState,
    onAttach: () -> Unit,
    onToggleSong: () -> Unit,
    onSeekSong: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = Color(note.noteColor).copy(alpha = 0.08f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, Color(note.accentColor).copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Song", style = MaterialTheme.typography.titleMedium, color = Color(note.accentColor))
                Text(
                    text = if (note.songUri.isBlank()) "No song attached" else "Attached",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Open")
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onAttach) { Text("Attach") }
                        OutlinedButton(onClick = onToggleSong, enabled = note.songUri.isNotBlank()) {
                            Text(if (media.songPlaying) "Pause" else "Play")
                        }
                    }
                    val sliderRange = if (media.songDurationMs <= 0) {
                        0f
                    } else {
                        (media.songPositionMs.toFloat() / media.songDurationMs.toFloat()).coerceIn(0f, 1f)
                    }
                    var pendingSeek by remember { mutableFloatStateOf(sliderRange) }
                    LaunchedEffect(sliderRange) {
                        pendingSeek = sliderRange
                    }
                    Slider(
                        value = pendingSeek,
                        onValueChange = { pendingSeek = it },
                        onValueChangeFinished = {
                            val targetMs = (pendingSeek * media.songDurationMs).roundToInt()
                            onSeekSong(targetMs)
                        },
                        enabled = media.songDurationMs > 0 && note.songUri.isNotBlank(),
                        colors = SliderDefaults.colors(),
                        valueRange = 0f..1f
                    )
                    Text(
                        text = "${formatDuration(media.songPositionMs)} / ${formatDuration(media.songDurationMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VoicePanel(
    note: NoteUi,
    media: MediaUiState,
    onStartRecording: () -> Unit,
    onStopRecording: (Boolean) -> Unit,
    onPlayRecording: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onExport: (String) -> Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = Color(note.noteColor).copy(alpha = 0.08f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, Color(note.accentColor).copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Voice", style = MaterialTheme.typography.titleMedium, color = Color(note.accentColor))
                Text(
                    text = if (media.recordingActive) "Recording" else "${note.recordings.size} saved",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Open")
                }
            }
            AnimatedVisibility(visible = expanded || media.recordingActive) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (media.recordingActive) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Recording", style = MaterialTheme.typography.labelLarge)
                            OutlinedButton(onClick = { onStopRecording(true) }) { Text("Save") }
                            OutlinedButton(onClick = { onStopRecording(false) }) { Text("Cancel") }
                        }
                        Text("elapsed ${formatDuration(media.recordingElapsedMs)}", style = MaterialTheme.typography.labelMedium)
                    } else {
                        OutlinedButton(onClick = onStartRecording) { Text("Record") }
                    }
                    if (note.recordings.isEmpty()) {
                        Text(
                            text = "No recordings yet",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    note.recordings.forEach { recording ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                recording.tag.ifBlank { "Recording" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge
                            )
                            val isPlaying = media.recordingPlaybackActive && media.recordingPlaybackPath == recording.path
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onPlayRecording(recording.path) }) {
                                    Text(if (isPlaying) "Stop" else "Play")
                                }
                                OutlinedButton(onClick = { onRename(recording.path, recording.tag) }) { Text("Rename") }
                                OutlinedButton(onClick = { onExport(recording.path) }) { Text("Export") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameRecordingDialog(
    open: Boolean,
    initialTag: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onTagChange: (String) -> Unit
) {
    if (!open) return
    var text by remember(initialTag) { mutableStateOf(initialTag) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val confirmWithDismiss = { tag: String ->
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        onConfirm(tag.trim())
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Rename recording", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onTagChange(it)
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    OutlinedButton(onClick = { confirmWithDismiss(text) }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun StyleEditorSheet(
    note: NoteUi,
    defaults: StyleDefaults,
    onDismiss: () -> Unit,
    onApply: (String, Int, Int, Int, Int, Int, Int, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftFont by remember(note.id) { mutableStateOf(note.font) }
    var draftFontSize by remember(note.id) { mutableIntStateOf(note.fontSizeSp) }
    var draftNoteColor by remember(note.id) { mutableIntStateOf(note.noteColor) }
    var draftTextColor by remember(note.id) { mutableIntStateOf(note.textColor) }
    var draftAccentColor by remember(note.id) { mutableIntStateOf(note.accentColor) }
    var draftMenuColor by remember(defaults) { mutableIntStateOf(defaults.menuColor) }
    var draftMenuAccentColor by remember(defaults) { mutableIntStateOf(defaults.menuAccentColor) }
    var draftColorTarget by remember { mutableStateOf("note") }
    var section by remember { mutableStateOf("preview") }
    var saveAsDefaults by remember { mutableStateOf(false) }

    fun colorForTarget() = when (draftColorTarget) {
        "note" -> draftNoteColor
        "text" -> draftTextColor
        "accent" -> draftAccentColor
        "menu" -> draftMenuColor
        else -> draftMenuAccentColor
    }

    fun setColorForTarget(nextColor: Int) {
        when (draftColorTarget) {
            "note" -> draftNoteColor = nextColor
            "text" -> draftTextColor = nextColor
            "accent" -> draftAccentColor = nextColor
            "menu" -> draftMenuColor = nextColor
            else -> draftMenuAccentColor = nextColor
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                onDismiss()
            }
        },
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Note Style", style = MaterialTheme.typography.titleLarge)
            StyleSectionTabs(selected = section, onSelect = { section = it })
            Surface(
                color = Color(draftNoteColor).copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, Color(draftAccentColor))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "The Top Flow",
                        style = editorTitleStyle(
                            note.copy(font = draftFont, fontSizeSp = draftFontSize, noteColor = draftNoteColor, textColor = draftTextColor, accentColor = draftAccentColor),
                            Color(draftTextColor)
                        ),
                        color = Color(draftTextColor)
                    )
                    Text(
                        "The Top Flow",
                        style = editorBodyStyle(
                            note.copy(font = draftFont, fontSizeSp = draftFontSize, noteColor = draftNoteColor, textColor = draftTextColor, accentColor = draftAccentColor),
                            Color(draftTextColor)
                        ),
                        color = Color(draftTextColor)
                    )
                }
            }
            when (section) {
                "font" -> {
                    Text("Font", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(noteFontOptions) { spec ->
                            val selected = spec.id == draftFont
                            OutlinedButton(
                                onClick = { draftFont = spec.id },
                                colors = if (selected) {
                                    ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                }
                            ) {
                                Text(spec.label, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Size ${draftFontSize}sp", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = draftFontSize.toFloat(),
                            onValueChange = { draftFontSize = it.toInt().coerceIn(14, 28) },
                            valueRange = 14f..28f,
                            steps = 14
                        )
                    }
                }
                "colors" -> {
                    Text("Color target", style = MaterialTheme.typography.labelLarge)
                    ColorTargetTabs(
                        selected = draftColorTarget,
                        onSelect = { draftColorTarget = it }
                    )
                    ColorWheelPicker(
                        color = colorForTarget(),
                        accent = draftAccentColor,
                        onChange = { setColorForTarget(it) }
                    )
                }
                "defaults" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = saveAsDefaults, onCheckedChange = { saveAsDefaults = it })
                        Text("Use this note style for new notes", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "Menu colors always apply to the app menu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = MaterialTheme.shapes.small
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${noteFontLabel(draftFont)} • ${draftFontSize}sp",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "#${colorToHex(colorForTarget())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                FilledTonalButton(
                    onClick = {
                        onApply(
                            draftFont,
                            draftFontSize,
                            draftNoteColor,
                            draftTextColor,
                            draftAccentColor,
                            draftMenuColor,
                            draftMenuAccentColor,
                            saveAsDefaults
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
private fun ColorTargetTabs(
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(
            listOf(
                "note" to "Note",
                "text" to "Text",
                "accent" to "Accent",
                "menu" to "Menu",
                "menuAccent" to "Menu Accent"
            )
        ) { (target, label) ->
            val isSelected = selected == target
            OutlinedButton(
                onClick = { onSelect(target) },
                colors = if (isSelected) {
                    ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun StyleSectionTabs(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("preview" to "Preview", "font" to "Font", "colors" to "Colors", "defaults" to "Defaults").forEach { (id, label) ->
            val isSelected = selected == id
            OutlinedButton(
                onClick = { onSelect(id) },
                colors = if (isSelected) {
                    ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ColorWheelPicker(
    color: Int,
    accent: Int,
    onChange: (Int) -> Unit
) {
    val hsv = remember(color) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(color, it) }
    }
    var hue by remember(color) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(color) { mutableFloatStateOf(hsv[1].coerceIn(0f, 1f)) }
    var value by remember(color) { mutableFloatStateOf(hsv[2].coerceIn(0f, 1f)) }

    fun commit(nextHue: Float = hue, nextSaturation: Float = saturation, nextValue: Float = value) {
        hue = ((nextHue % 360f) + 360f) % 360f
        saturation = nextSaturation.coerceIn(0f, 1f)
        value = nextValue.coerceIn(0f, 1f)
        onChange(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .size(196.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val degrees = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
                            val radius = min(size.width, size.height) / 2f
                            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                            commit(nextHue = degrees + 180f, nextSaturation = (distance / radius).coerceIn(0f, 1f))
                        }
                    }
            ) {
                val radius = min(size.width, size.height) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                for (angle in 0 until 360 step 6) {
                    drawArc(
                        color = Color(android.graphics.Color.HSVToColor(floatArrayOf(angle.toFloat(), 1f, value))),
                        startAngle = angle.toFloat(),
                        sweepAngle = 7f,
                        useCenter = true
                    )
                }
                drawCircle(color = Color.Black.copy(alpha = 0.18f), radius = radius * (1f - saturation), center = center)
                drawCircle(color = Color(accent), radius = radius, center = center, style = Stroke(width = 3.dp.toPx()))
            }
        }
        Text("Brightness ${(value * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
        Slider(value = value, onValueChange = { commit(nextValue = it) }, valueRange = 0.18f..1f)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(
                listOf(
                    0xFF0ECDBE.toInt(),
                    0xFF84FFEE.toInt(),
                    0xFF6C63FF.toInt(),
                    0xFFFFC875.toInt(),
                    0xFFFF8A80.toInt(),
                    0xFFFFFFFF.toInt(),
                    0xFF05070D.toInt(),
                    0xFF000000.toInt()
                )
            ) { swatch ->
                val selected = swatch == color
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(swatch))
                        .border(BorderStroke(if (selected) 2.dp else 1.dp, Color(accent)), CircleShape)
                        .clickable { onChange(swatch) }
                )
            }
        }
    }
}

@Composable
private fun ColorPickerInputs(
    color: Int,
    accent: Int,
    onChange: (Int) -> Unit
) {
    val r = (color shr 16) and 0xff
    val g = (color shr 8) and 0xff
    val b = color and 0xff
    fun opaqueColor(red: Int = r, green: Int = g, blue: Int = b): Int {
        return (0xFF shl 24) or
            ((red.coerceIn(0, 255) and 0xFF) shl 16) or
            ((green.coerceIn(0, 255) and 0xFF) shl 8) or
            (blue.coerceIn(0, 255) and 0xFF)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Red ${r}", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = r.toFloat(),
            onValueChange = { onChange(opaqueColor(red = it.toInt())) },
            valueRange = 0f..255f
        )
        Text("Green ${g}", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = g.toFloat(),
            onValueChange = { onChange(opaqueColor(green = it.toInt())) },
            valueRange = 0f..255f
        )
        Text("Blue ${b}", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = b.toFloat(),
            onValueChange = { onChange(opaqueColor(blue = it.toInt())) },
            valueRange = 0f..255f
        )

        val swatches = listOf(
            0xFF0ECDBE.toInt(),
            0xFF080C0E.toInt(),
            0xFF84FFEE.toInt(),
            0xFF6C63FF.toInt(),
            0xFFFFC875.toInt(),
            0xFFFF8A80.toInt(),
            0xFFFFFFFF.toInt(),
            0xFF000000.toInt()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            swatches.forEach { swatch ->
                val selected = swatch == color
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(swatch))
                        .border(
                            BorderStroke(if (selected) 2.dp else 1.dp, Color(accent)),
                            CircleShape
                        )
                        .clickable { onChange(swatch) }
                )
            }
        }
    }
}

@Composable
private fun RhymeSuggestionRow(
    suggestions: List<String>,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onInsertWord: (String) -> Unit = {}
) {
    Surface(
        modifier = modifier.testTag("rhyme_suggestions"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = if (loading) "Rhyme engine loading" else "Rhyme Suggestions",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (!loading && suggestions.isEmpty()) {
                Text(
                    text = "Type a word to see rhymes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { word ->
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.clickable { onInsertWord("$word ") }
                        ) {
                            Text(
                                text = word,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotesState(
    isSearching: Boolean,
    onCreateNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val runAction: (() -> Unit) -> Unit = { action ->
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        action()
    }
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isSearching) "No matching notes" else "No notes yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isSearching) "Try a different search." else "Create a note to start the flow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isSearching) {
                Spacer(Modifier.height(18.dp))
                FilledTonalButton(onClick = { runAction(onCreateNote) }) {
                    Text("New note")
                }
            }
        }
    }
}

@Composable
private fun JankStateLabels(screenLabel: String, isScrolling: Boolean) {
    val view = LocalView.current
    val holder = remember(view) { PerformanceMetricsState.getHolderForHierarchy(view) }
    LaunchedEffect(holder, screenLabel, isScrolling) {
        holder.state?.putState("TopFlowScreen", screenLabel)
        holder.state?.putState("TopFlowScrolling", isScrolling.toString())
    }
    DisposableEffect(holder) {
        onDispose {
            holder.state?.removeState("TopFlowScreen")
            holder.state?.removeState("TopFlowScrolling")
        }
    }
}

@Composable
private fun editorTitleStyle(note: NoteUi, color: Color): androidx.compose.ui.text.TextStyle {
    val textSize = note.fontSizeSp.coerceIn(14, 28).sp
    return androidx.compose.ui.text.TextStyle(
        fontFamily = noteFontFamily(note.font),
        fontSize = (note.fontSizeSp + 8).sp,
        lineHeight = (note.fontSizeSp + 12).sp,
        color = color
    )
}

@Composable
private fun editorBodyStyle(note: NoteUi, color: Color): androidx.compose.ui.text.TextStyle {
    val textSize = note.fontSizeSp.coerceIn(14, 28).sp
    return androidx.compose.ui.text.TextStyle(
        fontFamily = noteFontFamily(note.font),
        color = color,
        fontSize = textSize,
        lineHeight = (note.fontSizeSp.coerceIn(14, 28) + 10).sp
    )
}

private fun materialEditorSurface(noteColor: Int): Color = Color(noteColor).copy(alpha = 0.06f)

private fun readableColor(pageColor: Color, textColor: Color, fallback: Color): Color {
    return if (colorContrastRatio(pageColor, textColor) >= 2f) textColor else fallback
}

private fun readableMetadataColor(pageColor: Color, textColor: Color): Color {
    return if (colorContrastRatio(pageColor, textColor) >= 1.4f) textColor else Color.White
}

private fun colorContrastRatio(a: Color, b: Color): Float {
    val l1 = relativeLuminance(a) + 0.05f
    val l2 = relativeLuminance(b) + 0.05f
    val max = max(l1, l2)
    val min = min(l1, l2)
    return max / min
}

private fun relativeLuminance(color: Color): Float {
    fun toLinear(channel: Float): Float {
        return if (channel <= 0.03928f) {
            channel / 12.92f
        } else {
            ((channel + 0.055f) / 1.055f).pow(2.4f)
        }
    }

    return 0.2126f * toLinear(color.red) +
        0.7152f * toLinear(color.green) +
        0.0722f * toLinear(color.blue)
}

private fun formatDuration(ms: Int): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    val minutes = sec / 60
    val seconds = sec % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun shortDate(timestamp: Long): String {
    return DateFormat.getDateInstance(DateFormat.SHORT).format(Date(timestamp))
}

private fun colorToHex(color: Int): String {
    val noAlpha = color and 0x00FFFFFF
    return noAlpha.toString(16).uppercase().padStart(6, '0')
}

private fun launchTopFlowUpdate(context: Context) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.UPDATE_MANIFEST_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

enum class SwipeDirection { Left, Right }

@Composable
private fun Modifier.topFlowHorizontalSwipe(
    enabled: Boolean,
    direction: SwipeDirection,
    startZoneFraction: Float,
    thresholdPx: Float,
    onSwipe: () -> Unit
): Modifier {
    if (!enabled) return this
    val isRight = direction == SwipeDirection.Right
    val dragOffset = remember { mutableFloatStateOf(0f) }
    val isTracking = remember { mutableStateOf(false) }
    val totalDx = remember { mutableFloatStateOf(0f) }
    val totalDy = remember { mutableFloatStateOf(0f) }

    return this
        .offset { IntOffset(dragOffset.floatValue.roundToInt(), 0) }
        .pointerInput(enabled, direction, startZoneFraction, thresholdPx) {
            if (!enabled) return@pointerInput
            detectDragGestures(
                onDragStart = { start ->
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    totalDx.floatValue = 0f
                    totalDy.floatValue = 0f
                    isTracking.value = false
                    if (isRight && start.x > width * startZoneFraction) return@detectDragGestures
                    if (!isRight && width - start.x > width * startZoneFraction) return@detectDragGestures
                    isTracking.value = true
                },
                onDrag = { change, dragAmount ->
                    if (!isTracking.value) return@detectDragGestures
                    totalDx.floatValue += dragAmount.x
                    totalDy.floatValue += dragAmount.y
                    val absDx = abs(totalDx.floatValue)
                    val absDy = abs(totalDy.floatValue)
                    if (absDy * 1.25f > absDx) {
                        isTracking.value = false
                        dragOffset.floatValue = 0f
                        return@detectDragGestures
                    }
                    val directionalDrag = if (isRight) dragAmount.x else -dragAmount.x
                    if (absDx > absDy * 1.25f && directionalDrag > 0f) {
                        val nextMagnitude = (abs(dragOffset.floatValue) + directionalDrag)
                            .coerceIn(0f, thresholdPx * 1.25f)
                        dragOffset.floatValue = if (isRight) nextMagnitude else -nextMagnitude
                        change.consume()
                    }
                },
                onDragEnd = {
                    if (abs(dragOffset.floatValue) >= thresholdPx) onSwipe()
                    dragOffset.floatValue = 0f
                    isTracking.value = false
                },
                onDragCancel = {
                    dragOffset.floatValue = 0f
                    isTracking.value = false
                }
            )
        }
}
