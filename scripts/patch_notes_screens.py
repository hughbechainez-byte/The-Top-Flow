#!/usr/bin/env python3
"""Apply 30.5 UI polish patches to NotesScreens.kt (idempotent)."""
from pathlib import Path

PATH = Path("app/src/main/kotlin/com/davehq/thetopflow/ui/NotesScreens.kt")

def main() -> None:
    text = PATH.read_text(encoding="utf-8")
    original = text

    # 1) Contrast against the real editor paper, not the raw noteColor int
    text = text.replace(
        "val pageColor = Color(note.noteColor)\n"
        "    val accentColor = Color(note.accentColor)\n"
        "    val bodyTextColor = readableColor(pageColor, Color(note.textColor), MaterialTheme.colorScheme.onSurface)\n",
        "val pageColor = materialEditorSurface(note.noteColor)\n"
        "    val accentColor = Color(note.accentColor)\n"
        "    val bodyTextColor = readableColor(pageColor, Color(note.textColor), MaterialTheme.colorScheme.onSurface)\n",
    )
    text = text.replace(
        ".background(materialEditorSurface(note.noteColor))",
        ".background(pageColor)",
    )

    # 2) Delegate surface + contrast helpers to NoteVisualUtils
    text = text.replace(
        "private fun materialEditorSurface(noteColor: Int): Color = Color(noteColor).copy(alpha = 0.06f)",
        "private fun materialEditorSurface(noteColor: Int): Color = topFlowEditorSurface(noteColor)",
    )
    old_readable = (
        "private fun readableColor(pageColor: Color, textColor: Color, fallback: Color): Color {\n"
        "    return if (colorContrastRatio(pageColor, textColor) >= 2f) textColor else fallback\n"
        "}"
    )
    new_readable = (
        "private fun readableColor(pageColor: Color, textColor: Color, fallback: Color): Color =\n"
        "    topFlowReadableText(pageColor, textColor, fallback)"
    )
    text = text.replace(old_readable, new_readable)

    old_meta = (
        "private fun readableMetadataColor(pageColor: Color, textColor: Color): Color {\n"
        "    return if (colorContrastRatio(pageColor, textColor) >= 1.4f) textColor else Color.White\n"
        "}"
    )
    new_meta = (
        "private fun readableMetadataColor(pageColor: Color, textColor: Color): Color =\n"
        "    topFlowReadableMeta(pageColor, textColor)"
    )
    text = text.replace(old_meta, new_meta)

    # 3) Replace broken ColorWheelPicker body with accurate TopFlowColorWheel
    start = text.find("@Composable\nprivate fun ColorWheelPicker(")
    end = text.find("@Composable\nprivate fun ColorPickerInputs(")
    if start != -1 and end != -1 and start < end:
        replacement = (
            "@Composable\n"
            "private fun ColorWheelPicker(\n"
            "    color: Int,\n"
            "    accent: Int,\n"
            "    onChange: (Int) -> Unit\n"
            ") {\n"
            "    TopFlowColorWheel(color = color, accent = accent, onChange = onChange)\n"
            "}\n\n"
        )
        text = text[:start] + replacement + text[end:]

    # 4) Declutter editor chrome: drop version chip from toolbar row, keep Notes/Style/Menu
    old_toolbar = (
        "                Row(\n"
        "                    modifier = Modifier.fillMaxWidth(),\n"
        "                    verticalAlignment = Alignment.CenterVertically\n"
        "                ) {\n"
        "                    Text(\n"
        "                        text = \"v${BuildConfig.VERSION_NAME}\",\n"
        "                        style = MaterialTheme.typography.labelLarge,\n"
        "                        color = readableMetadataColor(pageColor, bodyTextColor),\n"
        "                        maxLines = 1\n"
        "                    )\n"
        "                    Spacer(Modifier.width(8.dp))\n"
        "                    OutlinedButton(onClick = { runAction(onBack) }, modifier = Modifier.testTag(\"close_editor\")) {\n"
        "                        Text(\"Notes\")\n"
        "                    )\n"
        "                    Spacer(Modifier.weight(1f))\n"
        "                    OutlinedButton(onClick = { runAction { showStyleSheet = true } }, modifier = Modifier.testTag(\"style_editor\")) {\n"
        "                        Text(\"Style\")\n"
        "                    )\n"
        "                    AnimatedVisibility(visible = !isCreating) {\n"
        "                        Spacer(Modifier.width(8.dp))\n"
        "                        OutlinedButton(onClick = { runAction(onDeleteNote) }, modifier = Modifier.testTag(\"delete_note\")) {\n"
        "                            Text(\"Delete\")\n"
        "                        }\n"
        "                    }\n"
        "                    Spacer(modifier.width(8.dp))\n"
        "                    Box {\n"
        "                        OutlinedButton(onClick = { showEditorMenu = true }) {\n"
        "                            Text(\"Menu\")\n"
        "                        }\n"
    )
    new_toolbar = (
        "                Row(\n"
        "                    modifier = Modifier.fillMaxWidth(),\n"
        "                    verticalAlignment = Alignment.CenterVertically\n"
        "                ) {\n"
        "                    OutlinedButton(onClick = { runAction(onBack) }, modifier = Modifier.testTag(\"close_editor\")) {\n"
        "                        Text(\"Notes\")\n"
        "                    )\n"
        "                    Spacer(Modifier.weight(1f))\n"
        "                    OutlinedButton(onClick = { runAction { showStyleSheet = true } }, modifier = Modifier.testTag(\"style_editor\")) {\n"
        "                        Text(\"Style\")\n"
        "                    )\n"
        "                    Spacer(modifier.width(8.dp))\n"
        "                    Box {\n"
        "                        OutlinedButton(onClick = { showEditorMenu = true }) {\n"
        "                            Text(\"Menu\")\n"
        "                        }\n"
    )
    if old_toolbar in text:
        text = text.replace(old_toolbar, new_toolbar, 1)
        # Move Delete into the overflow menu
        text = text.replace(
            "                            DropdownMenuItem(\n"
            "                                text = { Text(\"New note\") },\n"
            "                                onClick = { showEditorMenu = false; runAction(onCreateNote) }\n"
            "                            )\n"
            "                            DropdownMenuItem(\n"
            "                                text = { Text(\"Open latest note\") },\n"
            "                                onClick = { showEditorMenu = false; runAction(onOpenRecentNote) }\n"
            "                            )\n"
            "                            DropdownMenuItem(\n"
            "                                text = { Text(\"Check for app update\") },\n"
            "                                onClick = {\n"
            "                                    showEditorMenu = false\n"
            "                                    runAction(onCheckForUpdate)\n"
            "                                }\n"
            "                            )\n",
            "                            DropdownMenuItem(\n"
            "                                text = { Text(\"New note\") },\n"
            "                                onClick = { showEditorMenu = false; runAction(onCreateNote) }\n"
            "                            )\n"
            "                            DropdownMenuItem(\n"
            "                                text = { Text(\"Open latest note\") },\n"
            "                                onClick = { showEditorMenu = false; runAction(onOpenRecentNote) }\n"
            "                            )\n"
            "                            if (!isCreating) {\n"
            "                                DropdownMenuItem(\n"
            "                                    text = { Text(\"Delete note\") },\n"
            "                                    onClick = { showEditorMenu = false; runAction(onDeleteNote) }\n"
            "                                )\n"
            "                            }\n"
            "                            DropdownMenuItem(\n"
            "                                text = { Text(\"Check for app update\") },\n"
            "                                onClick = {\n"
            "                                    showEditorMenu = false\n"
            "                                    runAction(onCheckForUpdate)\n"
            "                                }\n"
            "                            )\n",
            1,
        )

    # 5) Theme copy no longer promises a scrolling ambient effect
    text = text.replace(
        'if (neonTheme) "OLED Neon uses crisp mint rails and a low-cost ambient sweep." else "Flat uses the clean base surfaces without animated rails."',
        'if (neonTheme) "OLED Neon uses a quiet static vignette and soft studio bloom." else "Flat uses the clean base surfaces without neon accents."',
    )

    if text == original:
        print("NotesScreens.kt already patched (or patterns not found)")
        return

    PATH.write_text(text, encoding="utf-8")
    print("Patched NotesScreens.kt")

if __name__ == "__main__":
    main()
