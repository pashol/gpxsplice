package com.example.gpxsplice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gpxsplice.domain.GpxDocument
import com.example.gpxsplice.domain.GpxSplitter
import com.example.gpxsplice.domain.SplitMode
import com.example.gpxsplice.domain.SplitOptions
import com.example.gpxsplice.domain.SplitResult
import com.example.gpxsplice.domain.orderedPoints

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

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("GPX Splice", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onPickFile) { Text("Choose GPX file") }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        localError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (document != null) {
            Text("Loaded ${document.orderedPoints().size} track points")
            SplitModeSelector(mode, onModeChange = { mode = it; results = emptyList() })
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(inputLabel(mode)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    runCatching {
                        GpxSplitter.split(document, optionsFor(mode, input))
                    }.onSuccess {
                        localError = if (it.size > 100) "Warning: this creates more than 100 files." else null
                        results = it
                    }.onFailure {
                        localError = it.message ?: "Could not split GPX file"
                    }
                },
            ) { Text("Split") }
        }

        if (results.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${results.size} output files")
                    Text("${results.sumOf { it.pointCount }} total points")
                }
            }
            TrackPreviewCanvas(results = results, modifier = Modifier.fillMaxWidth().height(220.dp))
            Button(onClick = { onShareFiles(results) }) { Text("Share GPX files") }
            Button(onClick = { onShareZip(results) }) { Text("Share ZIP") }
        }
    }
}

@Composable
private fun SplitModeSelector(mode: SplitMode, onModeChange: (SplitMode) -> Unit) {
    Column {
        SplitMode.entries.forEach { candidate ->
            Row {
                RadioButton(selected = mode == candidate, onClick = { onModeChange(candidate) })
                Text(candidate.label(), modifier = Modifier.padding(top = 12.dp))
            }
        }
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

private fun optionsFor(mode: SplitMode, input: String): SplitOptions = when (mode) {
    SplitMode.DISTANCE -> SplitOptions(mode = mode, distanceMeters = input.toDouble() * 1000.0)
    SplitMode.MAX_POINTS -> SplitOptions(mode = mode, maxPoints = input.toInt())
    SplitMode.EQUAL_STAGES -> SplitOptions(mode = mode, stages = input.toInt())
}
