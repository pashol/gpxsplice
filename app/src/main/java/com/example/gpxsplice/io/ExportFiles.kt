package com.example.gpxsplice.io

import com.example.gpxsplice.domain.SplitResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExportFile(
    val fileName: String,
    val bytes: ByteArray,
)

object ExportBuilder {
    fun gpxFiles(results: List<SplitResult>, inputFileName: String? = null): List<ExportFile> = results.map { result ->
        val splitIndex = result.index.toString().padStart(3, '0')
        ExportFile(
            fileName = exportFileName(
                inputFileName = inputFileName,
                suffix = "-split-$splitIndex",
                defaultName = "split-$splitIndex.gpx",
                outputExtension = ".gpx",
            ),
            bytes = GpxWriter.write(result.document).toByteArray(Charsets.UTF_8),
        )
    }
}

fun exportZipFileName(inputFileName: String?): String =
    exportFileName(inputFileName, suffix = "-splits", defaultName = "gpx-splits.zip", outputExtension = ".zip")

object ZipExporter {
    fun zipBytes(files: List<ExportFile>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            files.forEach { file ->
                file.requireSafeFileName()
                zip.putNextEntry(ZipEntry(file.fileName))
                zip.write(file.bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}

fun writeExportFile(directory: File, file: ExportFile): File {
    file.requireSafeFileName()
    directory.mkdirs()
    return File(directory, file.fileName).also { it.writeBytes(file.bytes) }
}

private fun ExportFile.requireSafeFileName() {
    require(fileName.isNotBlank())
    require(!File(fileName).isAbsolute)
    require(!fileName.contains('/'))
    require(!fileName.contains('\\'))
    require(!fileName.contains(".."))
}

private fun exportFileName(
    inputFileName: String?,
    suffix: String,
    defaultName: String,
    outputExtension: String,
): String {
    val trimmedName = inputFileName?.trim()?.takeIf(String::isNotEmpty) ?: return defaultName
    val extensionSeparator = trimmedName.lastIndexOf('.')
    if (extensionSeparator <= 0 || extensionSeparator == trimmedName.lastIndex) {
        return "$trimmedName$suffix$outputExtension"
    }

    val baseName = trimmedName.substring(0, extensionSeparator)
    return "$baseName$suffix$outputExtension"
}
