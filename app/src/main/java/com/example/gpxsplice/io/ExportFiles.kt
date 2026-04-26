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
    fun gpxFiles(results: List<SplitResult>): List<ExportFile> = results.map { result ->
        ExportFile(
            fileName = "split-${result.index}.gpx",
            bytes = GpxWriter.write(result.document).toByteArray(Charsets.UTF_8),
        )
    }
}

object ZipExporter {
    fun zipBytes(files: List<ExportFile>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            files.forEach { file ->
                zip.putNextEntry(ZipEntry(file.fileName))
                zip.write(file.bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}

fun writeExportFile(directory: File, file: ExportFile): File {
    directory.mkdirs()
    return File(directory, file.fileName).also { it.writeBytes(file.bytes) }
}
