package com.example.gpxsplice.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.gpxsplice.domain.GpxDocument
import com.example.gpxsplice.domain.GpxMerger
import com.example.gpxsplice.domain.GpxSplitter
import com.example.gpxsplice.domain.SplitMode
import com.example.gpxsplice.domain.SplitOptions
import com.example.gpxsplice.domain.SplitResult
import com.example.gpxsplice.domain.orderedPoints
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpxSplitApp(
    document: GpxDocument?,
    isImporting: Boolean,
    importStatusMessage: String?,
    onPickFile: () -> Unit,
    onShareFiles: (List<SplitResult>, Boolean) -> Unit,
    onShareZip: (List<SplitResult>, Boolean) -> Unit,
    errorMessage: String?,
    mergeInputs: List<MergeInput> = emptyList(),
    isImportingMergeFiles: Boolean = false,
    mergeImportStatusMessage: String? = null,
    onPickMergeFiles: () -> Unit = {},
    onShareMergedFile: (GpxDocument, String?, Boolean) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    var workflow by remember { mutableStateOf(Workflow.SPLIT) }
    var mode by remember { mutableStateOf(SplitMode.DISTANCE) }
    var input by remember { mutableStateOf("5") }
    var results by remember { mutableStateOf<List<SplitResult>>(emptyList()) }
    var sanitizeSplitExport by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var isSplitting by remember { mutableStateOf(false) }
    var splitJob by remember { mutableStateOf<Job?>(null) }
    var orderedMergeInputs by remember { mutableStateOf<List<MergeInput>>(emptyList()) }
    var mergeOrderingMessage by remember { mutableStateOf<String?>(null) }
    var mergedDocument by remember { mutableStateOf<GpxDocument?>(null) }
    var sanitizeMergeExport by remember { mutableStateOf(false) }
    var mergeError by remember { mutableStateOf<String?>(null) }
    var isMerging by remember { mutableStateOf(false) }
    var mergeJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val optionsResult = remember(mode, input) { runCatching { optionsFor(mode, input) } }
    val splitOptions = optionsResult.getOrNull()
    val inputError = optionsResult.exceptionOrNull()?.message

    LaunchedEffect(document, mode, input) {
        splitJob?.cancel()
        splitJob = null
        isSplitting = false
        results = emptyList()
        localError = null
    }

    LaunchedEffect(mergeInputs) {
        mergeJob = cancelAndClearMergeJob(mergeJob)
        isMerging = false
        val orderingResult = orderMergeInputs(mergeInputs)
        orderedMergeInputs = orderingResult.items
        mergeOrderingMessage = orderingResult.message
        mergedDocument = null
        mergeError = null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GPX Splice") },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(maxWidth = 840.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "Pick GPX files to split one track into stages or merge multiple tracks into one file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WorkflowSelector(workflow, onWorkflowChange = { workflow = it })
                    ErrorText(errorMessage)

                    when (workflow) {
                        Workflow.SPLIT -> SplitWorkflow(
                            document = document,
                            isImporting = isImporting,
                            importStatusMessage = importStatusMessage,
                            onPickFile = onPickFile,
                            localError = localError,
                            inputError = inputError,
                            mode = mode,
                            onModeChange = { mode = it; results = emptyList() },
                            input = input,
                            onInputChange = { input = it },
                            splitOptions = splitOptions,
                            isSplitting = isSplitting,
                            results = results,
                            sanitizeExport = sanitizeSplitExport,
                            onSanitizeExportChange = { sanitizeSplitExport = it },
                            onSplit = {
                                val currentDocument = document ?: return@SplitWorkflow
                                val currentOptions = splitOptions ?: return@SplitWorkflow
                                if (isSplitting) return@SplitWorkflow

                                results = emptyList()
                                localError = null
                                isSplitting = true
                                splitJob = coroutineScope.launch {
                                    try {
                                        val splitResults = withContext(Dispatchers.Default) {
                                            GpxSplitter.split(currentDocument, currentOptions)
                                        }
                                        localError = if (splitResults.size > 100) "Warning: this creates more than 100 files." else null
                                        results = splitResults
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (error: Throwable) {
                                        results = emptyList()
                                        localError = error.message ?: "Could not split GPX file"
                                    } finally {
                                        isSplitting = false
                                        splitJob = null
                                    }
                                }
                            },
                            onShareFiles = { shareResults -> onShareFiles(shareResults, sanitizeSplitExport) },
                            onShareZip = { shareResults -> onShareZip(shareResults, sanitizeSplitExport) },
                        )

                        Workflow.MERGE -> MergeWorkflow(
                            orderedMergeInputs = orderedMergeInputs,
                            isImportingMergeFiles = isImportingMergeFiles,
                            mergeImportStatusMessage = mergeImportStatusMessage,
                            mergeOrderingMessage = mergeOrderingMessage,
                            mergeError = mergeError,
                            isMerging = isMerging,
                            mergedDocument = mergedDocument,
                            sanitizeExport = sanitizeMergeExport,
                            onSanitizeExportChange = { sanitizeMergeExport = it },
                            onPickMergeFiles = onPickMergeFiles,
                            onMoveMergeInput = { index, direction ->
                                mergeJob = cancelAndClearMergeJob(mergeJob)
                                isMerging = false
                                orderedMergeInputs = moveMergeInput(orderedMergeInputs, index, direction)
                                mergeOrderingMessage = null
                                mergedDocument = null
                                mergeError = null
                            },
                            onReorderMergeInput = { fromIndex, toIndex ->
                                if (fromIndex == toIndex) return@MergeWorkflow
                                mergeJob = cancelAndClearMergeJob(mergeJob)
                                isMerging = false
                                orderedMergeInputs = moveMergeInput(orderedMergeInputs, fromIndex, toIndex)
                                mergeOrderingMessage = null
                                mergedDocument = null
                                mergeError = null
                            },
                            onMerge = {
                                if (!canMerge(orderedMergeInputs) || isMerging) return@MergeWorkflow

                                val documents = orderedMergeInputs.map { it.document }
                                mergedDocument = null
                                mergeError = null
                                isMerging = true
                                mergeJob = coroutineScope.launch {
                                    try {
                                        mergedDocument = withContext(Dispatchers.Default) {
                                            GpxMerger.merge(documents)
                                        }
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (error: Throwable) {
                                        mergedDocument = null
                                        mergeError = error.message?.trim()?.takeIf(String::isNotEmpty) ?: "Could not merge GPX files"
                                    } finally {
                                        isMerging = false
                                        mergeJob = null
                                    }
                                }
                            },
                            onShareMergedFile = { merged ->
                                onShareMergedFile(merged, orderedMergeInputs.firstOrNull()?.fileName, sanitizeMergeExport)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkflowSelector(workflow: Workflow, onWorkflowChange: (Workflow) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Workflow.entries.forEach { candidate ->
            Row(
                modifier = Modifier
                    .selectable(
                        selected = workflow == candidate,
                        onClick = { onWorkflowChange(candidate) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = workflow == candidate, onClick = null)
                Text(candidate.label(), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SplitWorkflow(
    document: GpxDocument?,
    isImporting: Boolean,
    importStatusMessage: String?,
    onPickFile: () -> Unit,
    localError: String?,
    inputError: String?,
    mode: SplitMode,
    onModeChange: (SplitMode) -> Unit,
    input: String,
    onInputChange: (String) -> Unit,
    splitOptions: SplitOptions?,
    isSplitting: Boolean,
    results: List<SplitResult>,
    sanitizeExport: Boolean,
    onSanitizeExportChange: (Boolean) -> Unit,
    onSplit: () -> Unit,
    onShareFiles: (List<SplitResult>) -> Unit,
    onShareZip: (List<SplitResult>) -> Unit,
) {
    Text(
        "Pick a GPX file, choose a split method, then export the generated stages.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(onClick = onPickFile, enabled = !isImporting) {
        Text(if (isImporting) "Opening GPX file..." else "Choose GPX file")
    }
    AnimatedVisibility(visible = isImporting && importStatusMessage != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = importStatusMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    ErrorText(localError)
    ErrorText(inputError)

    if (document != null) {
        Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Loaded ${document.orderedPoints().size} track points",
                    style = MaterialTheme.typography.titleMedium,
                )
                SplitModeSelector(mode, onModeChange = onModeChange)
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text(inputLabel(mode)) },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardTypeFor(mode)),
                    isError = inputError != null,
                    supportingText = {
                        inputError?.let { Text(it) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = splitOptions != null && !isSplitting,
                    onClick = onSplit,
                ) { Text(if (isSplitting) "Splitting..." else "Split") }
            }
        }
    }

    if (results.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("${results.size} output files", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${results.sumOf { it.pointCount }} total points",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TrackPreviewCanvas(results = results, modifier = Modifier.fillMaxWidth().height(220.dp))
        SanitizeExportOption(
            checked = sanitizeExport,
            onCheckedChange = onSanitizeExportChange,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (shouldUseVerticalExportActions(maxWidth)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onShareFiles(results) }, modifier = Modifier.fillMaxWidth()) { Text("Share GPX files") }
                    Button(onClick = { onShareZip(results) }, modifier = Modifier.fillMaxWidth()) { Text("Share ZIP") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onShareFiles(results) }, modifier = Modifier.weight(1f)) { Text("Share GPX files") }
                    Button(onClick = { onShareZip(results) }, modifier = Modifier.weight(1f)) { Text("Share ZIP") }
                }
            }
        }
    }
}

@Composable
private fun MergeWorkflow(
    orderedMergeInputs: List<MergeInput>,
    isImportingMergeFiles: Boolean,
    mergeImportStatusMessage: String?,
    mergeOrderingMessage: String?,
    mergeError: String?,
    isMerging: Boolean,
    mergedDocument: GpxDocument?,
    sanitizeExport: Boolean,
    onSanitizeExportChange: (Boolean) -> Unit,
    onPickMergeFiles: () -> Unit,
    onMoveMergeInput: (Int, MergeMoveDirection) -> Unit,
    onReorderMergeInput: (Int, Int) -> Unit,
    onMerge: () -> Unit,
    onShareMergedFile: (GpxDocument) -> Unit,
) {
    val itemPositions = remember { mutableStateMapOf<Int, MergeItemPosition>() }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Text(
        "Choose 2 or more GPX files, review their order, then export one merged GPX.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(enabled = !isImportingMergeFiles, onClick = onPickMergeFiles) { Text("Choose GPX files") }
    if (isImportingMergeFiles) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        mergeImportStatusMessage?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    mergeOrderingMessage?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    ErrorText(mergeError)

    mergeReorderHint(orderedMergeInputs.size)?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    orderedMergeInputs.forEachIndexed { index, item ->
        val isDragged = draggedIndex == index
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(if (isDragged) 1f else 0f)
                .graphicsLayer { translationY = if (isDragged) dragOffsetY else 0f }
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    itemPositions[index] = MergeItemPosition(
                        top = position.y,
                        bottom = position.y + coordinates.size.height,
                    )
                }
                .pointerInput(index, orderedMergeInputs.size) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggedIndex = index
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            draggedIndex = null
                            dragOffsetY = 0f
                        },
                        onDragEnd = {
                            draggedIndex = null
                            dragOffsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val currentIndex = draggedIndex ?: return@detectDragGesturesAfterLongPress
                            dragOffsetY += dragAmount.y
                            val draggedPosition = itemPositions[currentIndex] ?: return@detectDragGesturesAfterLongPress
                            val draggedCenterY = draggedPosition.centerY + dragOffsetY
                            val targetIndex = itemPositions
                                .minByOrNull { (_, position) -> kotlin.math.abs(position.centerY - draggedCenterY) }
                                ?.key
                                ?: return@detectDragGesturesAfterLongPress

                            if (targetIndex != currentIndex) {
                                onReorderMergeInput(currentIndex, targetIndex)
                                draggedIndex = targetIndex
                                dragOffsetY = 0f
                            }
                        },
                    )
                },
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(item.fileName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${item.pointCount} track points",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        enabled = index > 0,
                        onClick = { onMoveMergeInput(index, MergeMoveDirection.UP) },
                    ) { Text("Up") }
                    Button(
                        enabled = index < orderedMergeInputs.lastIndex,
                        onClick = { onMoveMergeInput(index, MergeMoveDirection.DOWN) },
                    ) { Text("Down") }
                }
            }
        }
    }

    Button(
        enabled = canMerge(orderedMergeInputs) && !isMerging,
        onClick = onMerge,
    ) { Text(if (isMerging) "Merging..." else "Merge") }

    mergedDocument?.let { merged ->
        val previewResult = mergedPreviewResult(merged)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Merged GPX ready", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${previewResult.pointCount} total points",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TrackPreviewCanvas(results = listOf(previewResult), modifier = Modifier.fillMaxWidth().height(220.dp))
        SanitizeExportOption(
            checked = sanitizeExport,
            onCheckedChange = onSanitizeExportChange,
        )
        Button(onClick = { onShareMergedFile(merged) }, modifier = Modifier.fillMaxWidth()) { Text("Share merged GPX") }
    }
}

@Composable
private fun SplitModeSelector(mode: SplitMode, onModeChange: (SplitMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SplitMode.entries.forEach { candidate ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = mode == candidate,
                        onClick = { onModeChange(candidate) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = mode == candidate, onClick = null)
                Text(candidate.label(), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun ErrorText(message: String?) {
    message?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SanitizeExportOption(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = checked,
                    onClick = { onCheckedChange(!checked) },
                    role = Role.Checkbox,
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Text("Remove time tags from export", style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            text = sanitizeExportHelpText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private enum class Workflow {
    SPLIT,
    MERGE,
}

private fun Workflow.label(): String = when (this) {
    Workflow.SPLIT -> "Split"
    Workflow.MERGE -> "Merge"
}

private fun SplitMode.label(): String = when (this) {
    SplitMode.DISTANCE -> "Distance"
    SplitMode.MAX_POINTS -> "Max points"
    SplitMode.EQUAL_STAGES -> "Equal stages"
}

private fun inputLabel(mode: SplitMode): String = when (mode) {
    SplitMode.DISTANCE -> "Distance in kilometers"
    SplitMode.MAX_POINTS -> "Maximum points per file"
    SplitMode.EQUAL_STAGES -> "Number of stages"
}

private fun keyboardTypeFor(mode: SplitMode): KeyboardType = when (mode) {
    SplitMode.DISTANCE -> KeyboardType.Decimal
    SplitMode.MAX_POINTS,
    SplitMode.EQUAL_STAGES,
    -> KeyboardType.Number
}

private fun optionsFor(mode: SplitMode, input: String): SplitOptions = when (mode) {
    SplitMode.DISTANCE -> SplitOptions(mode = mode, distanceMeters = input.toDouble() * 1000.0)
    SplitMode.MAX_POINTS -> SplitOptions(mode = mode, maxPoints = input.toInt())
    SplitMode.EQUAL_STAGES -> SplitOptions(mode = mode, stages = input.toInt())
}

internal fun shouldUseVerticalExportActions(maxWidth: Dp): Boolean = maxWidth < 600.dp

internal fun cancelAndClearMergeJob(job: Job?): Job? {
    job?.cancel()
    return null
}

internal fun mergeReorderHint(fileCount: Int): String? =
    if (fileCount > 1) "Long-press and drag cards to rearrange files." else null

internal fun sanitizeExportHelpText(): String =
    "Optional: remove time tags before sharing. Point order, coordinates, and elevation stay unchanged."

private data class MergeItemPosition(
    val top: Float,
    val bottom: Float,
) {
    val centerY: Float = (top + bottom) / 2f
}

internal fun mergedPreviewResult(document: GpxDocument): SplitResult =
    SplitResult(
        index = 1,
        document = document,
        pointCount = document.orderedPoints().size,
        distanceMeters = 0.0,
    )
