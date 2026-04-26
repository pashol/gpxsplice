package com.example.gpxsplice

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
import com.example.gpxsplice.domain.GpxDocument
import com.example.gpxsplice.domain.SplitResult
import com.example.gpxsplice.io.ExportBuilder
import com.example.gpxsplice.io.ExportFile
import com.example.gpxsplice.io.GpxReader
import com.example.gpxsplice.io.ZipExporter
import com.example.gpxsplice.io.writeExportFile
import com.example.gpxsplice.ui.GpxSplitApp
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var document by remember { mutableStateOf<GpxDocument?>(null) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    runCatching {
                        contentResolver.openInputStream(uri).use { input ->
                            requireNotNull(input) { "Could not open selected file" }
                            GpxReader.read(input)
                        }
                    }.onSuccess {
                        document = it
                        errorMessage = null
                    }.onFailure {
                        errorMessage = "Could not read GPX file"
                    }
                }

                GpxSplitApp(
                    document = document,
                    onPickFile = { picker.launch(arrayOf("application/gpx+xml", "application/xml", "text/xml", "*/*")) },
                    onShareFiles = { shareFiles(ExportBuilder.gpxFiles(it)) },
                    onShareZip = { shareZip(it) },
                    errorMessage = errorMessage,
                )
            }
        }
    }

    private fun shareFiles(files: List<ExportFile>) {
        val exportDir = File(cacheDir, "exports")
        exportDir.deleteRecursively()
        exportDir.mkdirs()
        val uris = files.map { file ->
            val output = writeExportFile(exportDir, file)
            FileProvider.getUriForFile(this, "$packageName.fileprovider", output)
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/gpx+xml"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share GPX files"))
    }

    private fun shareZip(results: List<SplitResult>) {
        val exportDir = File(cacheDir, "exports")
        exportDir.deleteRecursively()
        exportDir.mkdirs()
        val zipFile = writeExportFile(
            exportDir,
            ExportFile("gpx-splits.zip", ZipExporter.zipBytes(ExportBuilder.gpxFiles(results))),
        )
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", zipFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share ZIP"))
    }
}
