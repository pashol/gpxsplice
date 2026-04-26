package com.example.gpxsplice

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.gpxsplice.domain.GpxDocument
import com.example.gpxsplice.domain.SplitResult
import com.example.gpxsplice.io.ExportBuilder
import com.example.gpxsplice.io.ExportFile
import com.example.gpxsplice.io.GpxReader
import com.example.gpxsplice.io.ZipExporter
import com.example.gpxsplice.io.writeExportFile
import com.example.gpxsplice.ui.GpxSplitApp
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var document by remember { mutableStateOf<GpxDocument?>(null) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    lifecycleScope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                contentResolver.openInputStream(uri).use { input ->
                                    requireNotNull(input) { "Could not open selected file" }
                                    GpxReader.read(input)
                                }
                            }
                        }.onSuccess {
                            document = it
                            errorMessage = null
                        }.onFailure {
                            errorMessage = "Could not read GPX file"
                        }
                    }
                }

                GpxSplitApp(
                    document = document,
                    onPickFile = { picker.launch(arrayOf("application/gpx+xml", "application/xml", "text/xml", "*/*")) },
                    onShareFiles = { results ->
                        lifecycleScope.launch {
                            shareFiles(ExportBuilder.gpxFiles(results))
                        }
                    },
                    onShareZip = { results ->
                        lifecycleScope.launch {
                            shareZip(results)
                        }
                    },
                    errorMessage = errorMessage,
                )
            }
        }
    }

    private suspend fun shareFiles(files: List<ExportFile>) {
        val uris = withContext(Dispatchers.IO) {
            val exportDir = createExportDir()
            files.map { file ->
                val output = writeExportFile(exportDir, file)
                FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", output)
            }
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/gpx+xml"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            clipData = uris.toClipData("GPX files")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share GPX files"))
    }

    private suspend fun shareZip(results: List<SplitResult>) {
        val uri = withContext(Dispatchers.IO) {
            val exportDir = createExportDir()
            val zipFile = writeExportFile(
                exportDir,
                ExportFile("gpx-splits.zip", ZipExporter.zipBytes(ExportBuilder.gpxFiles(results))),
            )
            FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", zipFile)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(contentResolver, "ZIP", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share ZIP"))
    }

    private fun createExportDir(): File {
        val exportsRoot = File(cacheDir, "exports")
        exportsRoot.mkdirs()
        return File(exportsRoot, UUID.randomUUID().toString()).apply {
            mkdirs()
        }
    }

    private fun List<Uri>.toClipData(label: String): ClipData {
        val firstUri = first()
        return ClipData.newUri(contentResolver, label, firstUri).apply {
            drop(1).forEach { uri ->
                addItem(ClipData.Item(uri))
            }
        }
    }
}
