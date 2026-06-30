package com.davehq.thetopflow.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.metrics.performance.PerformanceMetricsState
import com.davehq.thetopflow.NotesUiState
import com.davehq.thetopflow.data.NoteUi
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NotesRoute(
    state: NotesUiState,
    onCreateNote: () -> Unit,
    onOpenNote: (String) -> Unit,
    onCloseEditor: () -> Unit,
    onSearch: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onDeleteNote: () -> Unit
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
                onSearch = onSearch
            )
        } else {
                NoteEditorScreen(
                note = state.selectedNote,
                isCreating = state.isCreating,
                rhymeSuggestions = state.rhymeSuggestions,
                rhymeLoading = state.rhymeLoading,
                onBack = onCloseEditor,
                onTitleChange = onTitleChange,
                onBodyChange = onBodyChange,
                onDeleteNote = onDeleteNote
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesGridScreen(
    state: NotesUiState,
    gridState: LazyStaggeredGridState,
    onCreateNote: () -> Unit,
    onOpenNote: (String) -> Unit,
    onSearch: (String) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "The Top Flow",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    text = "${state.notes.size} notes",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(16.dp))
                SearchField(value = state.query, onValueChange = onSearch)
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    modifier = Modifier.testTag("create_note"),
                    onClick = onCreateNote,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("New note", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("search_notes")
            .semantics { contentDescription = "Search notes" },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                .background(MaterialTheme.colorScheme.surface)
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
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AnimatedVisibility(visible = note.preview.isNotBlank()) {
                Text(
                    text = note.preview,
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "${note.wordCount} words · ${shortDate(note.updatedAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NoteEditorScreen(
    note: NoteUi,
    isCreating: Boolean,
    rhymeSuggestions: List<String>,
    rhymeLoading: Boolean,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onDeleteNote: () -> Unit
) {
    BackHandler(onBack = onBack)
    NoteEditor(
        note = note,
        isCreating = isCreating,
        rhymeSuggestions = rhymeSuggestions,
        rhymeLoading = rhymeLoading,
        onBack = onBack,
        onTitleChange = onTitleChange,
        onBodyChange = onBodyChange,
        onDeleteNote = onDeleteNote
    )
}

@Composable
fun NoteEditor(
    note: NoteUi,
    isCreating: Boolean,
    rhymeSuggestions: List<String> = emptyList(),
    rhymeLoading: Boolean = false,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onTitleChange: (String) -> Unit = {},
    onBodyChange: (String) -> Unit = {},
    onDeleteNote: () -> Unit = {}
) {
    val accent = Color(note.accentColor)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("note_editor")
            .semantics { contentDescription = "Note editor" }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.testTag("close_editor")) {
                Text("Notes", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(visible = !isCreating) {
                OutlinedButton(onClick = onDeleteNote, modifier = Modifier.testTag("delete_note")) {
                    Text("Delete", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        BasicTextField(
            value = note.title,
            onValueChange = onTitleChange,
            textStyle = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("editor_title")
                .semantics { contentDescription = "Note title" },
            singleLine = false,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (note.title.isBlank()) {
                        Text(
                            text = "Untitled",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${note.wordCount} words · ${shortDate(note.updatedAt)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(18.dp))
        RhymeSuggestionRow(
            suggestions = rhymeSuggestions,
            loading = rhymeLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))
        BasicTextField(
            value = note.body,
            onValueChange = onBodyChange,
            textStyle = editorBodyStyle(note),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("editor_body")
                .semantics { contentDescription = "Note body" },
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxSize()) {
                    if (note.body.isBlank()) {
                        Text(
                            text = "Start writing...",
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
private fun editorBodyStyle(note: NoteUi): TextStyle {
    return MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = note.fontSizeSp.coerceIn(14, 28).sp,
        lineHeight = (note.fontSizeSp.coerceIn(14, 28) + 10).sp
    )
}

@Composable
private fun RhymeSuggestionRow(
    suggestions: List<String>,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("rhyme_suggestions"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = if (loading) "Rhyme engine loading" else "Rhymes",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (!loading && suggestions.isEmpty()) {
                Text(
                    text = "Type a word to see offline rhymes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions, key = { it }) { word ->
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
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
                FilledTonalButton(onClick = onCreateNote) {
                    Text("New note", style = MaterialTheme.typography.labelLarge)
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

private fun shortDate(timestamp: Long): String {
    return DateFormat.getDateInstance(DateFormat.SHORT).format(Date(timestamp))
}
