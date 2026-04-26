package com.example.gpxsplice.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gpxsplice.domain.GpxDocument
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

@Composable
fun GpxSplitApp(
    document: GpxDocument?,
    onPickFile: () -> Unit,
    onShareFiles: (List<SplitResult>) -> Unit,
    onShareZip: (List<SplitResult>) -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(SplitMode.DISTANCE) }
    var input by remember { mutableStateOf("5") }
    var results by remember { mutableStateOf<List<SplitResult>>(emptyList()) }
    var localError by remember { mutableStateOf<String?>(null) }
    var isSplitting by remember { mutableStateOf(false) }
    var splitJob by remember { mutableStateOf<Job?>(null) }
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(maxWidth = 840.dp)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("GPX Splice", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Pick a GPX file, choose a split method, then export the generated stages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onPickFile) { Text("Choose GPX file") }
                ErrorText(errorMessage)
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
                            SplitModeSelector(mode, onModeChange = { mode = it; results = emptyList() })
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                label = { Text(inputLabel(mode)) },
                                keyboardOptions = KeyboardOptions(keyboardType = keyboardTypeFor(mode)),
                                isError = inputError != null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                enabled = splitOptions != null && !isSplitting,
                                onClick = {
                                    val currentOptions = splitOptions
                                    if (currentOptions == null || isSplitting) return@Button

                                    results = emptyList()
                                    localError = null
                                    isSplitting = true
                                    splitJob = coroutineScope.launch {
                                        try {
                                            val splitResults = withContext(Dispatchers.Default) {
                                                GpxSplitter.split(document, currentOptions)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { onShareFiles(results) }, modifier = Modifier.weight(1f)) { Text("Share GPX files") }
                        Button(onClick = { onShareZip(results) }, modifier = Modifier.weight(1f)) { Text("Share ZIP") }
                    }
                }
            }
        }
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
                RadioButton(selected = mode == candidate, onClick = { onModeChange(candidate) })
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
