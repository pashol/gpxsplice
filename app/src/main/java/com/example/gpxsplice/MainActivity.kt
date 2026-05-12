package com.example.gpxsplice

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.gpxsplice.io.exportZipFileName
import com.example.gpxsplice.io.writeExportFile
import com.example.gpxsplice.ui.GpxSplitApp
import com.example.gpxsplice.ui.MergeInput
import com.example.gpxsplice.ui.theme.GpxSpliceTheme
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent {
            GpxSpliceTheme {
                var document by remember { mutableStateOf<GpxDocument?>(null) }
                var inputFileName by remember { mutableStateOf<String?>(null) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                var mergeInputs by remember { mutableStateOf<List<MergeInput>>(emptyList()) }
                var isImportingMergeFiles by remember { mutableStateOf(false) }
                var mergeImportCount by remember { mutableStateOf(0) }
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    lifecycleScope.launch {
                        val selectedFileName = uri.displayName()
                        runCatchingPreservingCancellation {
                            withContext(Dispatchers.IO) {
                                contentResolver.openInputStream(uri).use { input ->
                                    requireNotNull(input) { "Could not open selected file" }
                                    GpxReader.read(input)
                                }
                            }
                        }.onSuccess {
                            document = it
                            inputFileName = selectedFileName
                            errorMessage = null
                        }.onFailure {
                            document = null
                            inputFileName = null
                            errorMessage = formatImportErrorMessage(it)
                        }
                    }
                }
                val mergePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
                    if (uris.isEmpty()) return@rememberLauncherForActivityResult
                    lifecycleScope.launch {
                        isImportingMergeFiles = true
                        mergeImportCount = uris.size
                        errorMessage = null
                        try {
                            runCatchingPreservingCancellation {
                                withContext(Dispatchers.IO) {
                                    uris.mapIndexed { index, uri ->
                                        val fileName = try {
                                            uri.displayName()
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (_: Throwable) {
                                            null
                                        } ?: "GPX file ${index + 1}"
                                        val document = try {
                                            contentResolver.openInputStream(uri).use { input ->
                                                if (input == null) {
                                                    throw IllegalStateException("Could not open $fileName")
                                                }
                                                try {
                                                    GpxReader.read(input)
                                                } catch (error: CancellationException) {
                                                    throw error
                                                } catch (error: Throwable) {
                                                    val detail = error.message?.trim()?.takeIf(String::isNotEmpty) ?: "invalid GPX file"
                                                    throw IllegalArgumentException("Could not read $fileName: $detail", error)
                                                }
                                            }
                                        } catch (error: IllegalArgumentException) {
                                            throw error
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (error: Throwable) {
                                            throw IllegalStateException("Could not open $fileName", error)
                                        }
                                        MergeInput(fileName, document)
                                    }
                                }
                            }.onSuccess {
                                mergeInputs = it
                                errorMessage = null
                            }.onFailure {
                                mergeInputs = emptyList()
                                errorMessage = it.message ?: "Could not read GPX files"
                            }
                        } finally {
                            isImportingMergeFiles = false
                            mergeImportCount = 0
                        }
                    }
                }

                GpxSplitApp(
                    document = document,
                    onPickFile = { picker.launch(arrayOf("application/gpx+xml", "application/xml", "text/xml", "*/*")) },
                    onShareFiles = { results ->
                        lifecycleScope.launch {
                            runCatchingPreservingCancellation {
                                shareFiles(ExportBuilder.gpxFiles(results, inputFileName))
                            }.onSuccess {
                                errorMessage = null
                            }.onFailure {
                                errorMessage = "Could not share GPX files"
                            }
                        }
                    },
                    onShareZip = { results ->
                        lifecycleScope.launch {
                            runCatchingPreservingCancellation {
                                shareZip(results, inputFileName)
                            }.onSuccess {
                                errorMessage = null
                            }.onFailure {
                                errorMessage = "Could not share ZIP"
                            }
                        }
                    },
                    errorMessage = errorMessage,
                    mergeInputs = mergeInputs,
                    isImportingMergeFiles = isImportingMergeFiles,
                    mergeImportStatusMessage = if (isImportingMergeFiles) {
                        formatMergeImportProgressMessage(mergeImportCount)
                    } else {
                        null
                    },
                    onPickMergeFiles = {
                        if (!isImportingMergeFiles) {
                            mergePicker.launch(arrayOf("application/gpx+xml", "application/xml", "text/xml", "*/*"))
                        }
                    },
                    onShareMergedFile = { mergedDocument, firstInputFileName ->
                        lifecycleScope.launch {
                            runCatchingPreservingCancellation {
                                shareMergedFile(mergedDocument, firstInputFileName)
                            }.onSuccess {
                                errorMessage = null
                            }.onFailure {
                                errorMessage = "Could not share merged GPX"
                            }
                        }
                    },
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

    private suspend fun shareZip(results: List<SplitResult>, inputFileName: String?) {
        val uri = withContext(Dispatchers.IO) {
            val exportDir = createExportDir()
            val zipFile = writeExportFile(
                exportDir,
                ExportFile(exportZipFileName(inputFileName), ZipExporter.zipBytes(ExportBuilder.gpxFiles(results, inputFileName))),
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

    private suspend fun shareMergedFile(document: GpxDocument, firstInputFileName: String?) {
        val uri = withContext(Dispatchers.IO) {
            val exportDir = createExportDir()
            val output = writeExportFile(exportDir, ExportBuilder.mergedGpxFile(document, firstInputFileName))
            FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", output)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(contentResolver, "Merged GPX", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share merged GPX"))
    }

    private fun createExportDir(): File {
        val exportsRoot = File(cacheDir, "exports")
        exportsRoot.mkdirs()
        return File(exportsRoot, UUID.randomUUID().toString()).apply {
            mkdirs()
            cleanupOldExportDirs(exportsRoot, this)
        }
    }

    private fun cleanupOldExportDirs(exportsRoot: File, activeDir: File) {
        exportsRoot.listFiles()
            ?.filter { directory ->
                directory.isDirectory &&
                    directory != activeDir &&
                    directory.lastModified() < System.currentTimeMillis() - EXPORT_DIR_MAX_AGE_MS
            }
            ?.forEach { it.deleteRecursively() }
    }

    private inline fun <T> runCatchingPreservingCancellation(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
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

    private fun Uri.displayName(): String? {
        contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
        return null
    }

    companion object {
        private const val EXPORT_DIR_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    }
}

internal fun formatImportErrorMessage(error: Throwable): String {
    val detail = error.message?.trim()?.takeIf(String::isNotEmpty) ?: return "Could not read GPX file"
    return "Could not read GPX file: $detail"
}

internal fun formatMergeImportProgressMessage(fileCount: Int): String =
    if (fileCount > 0) "Opening $fileCount GPX files..." else "Opening GPX files..."
