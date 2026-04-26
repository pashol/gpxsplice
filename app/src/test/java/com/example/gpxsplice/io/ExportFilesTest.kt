package com.example.gpxsplice.io

import com.example.gpxsplice.domain.GpxDocument
import com.example.gpxsplice.domain.SplitResult
import com.example.gpxsplice.domain.Track
import com.example.gpxsplice.domain.TrackPoint
import com.example.gpxsplice.domain.TrackSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipInputStream

class ExportFilesTest {
    @Test
    fun buildsGpxFilesWithSplitNamesAndWriterBytes() {
        val results = listOf(
            splitResult(index = 1, documentName = "first"),
            splitResult(index = 2, documentName = "second"),
        )

        val files = ExportBuilder.gpxFiles(results)

        assertEquals(listOf("split-1.gpx", "split-2.gpx"), files.map { it.fileName })
        assertEquals(
            results.map { GpxWriter.write(it.document).toByteArray(Charsets.UTF_8).toList() },
            files.map { it.bytes.toList() },
        )
    }

    @Test
    fun gpxFilesUseInputFileNameWithIndexedSuffix() {
        val files = ExportBuilder.gpxFiles(results = listOf(splitResult(index = 1)), inputFileName = "ride.gpx")

        assertEquals(listOf("ride-split-1.gpx"), files.map { it.fileName })
    }

    @Test
    fun zipFileNameUsesInputFileNameWithSuffix() {
        assertEquals("ride-splits.zip", exportZipFileName("ride.gpx"))
    }

    @Test
    fun exportNamesFallBackWhenInputFileNameMissing() {
        val files = ExportBuilder.gpxFiles(results = listOf(splitResult(index = 2)), inputFileName = null)

        assertEquals(listOf("split-2.gpx"), files.map { it.fileName })
        assertEquals("gpx-splits.zip", exportZipFileName(null))
    }

    @Test
    fun createsZipWithAllEntries() {
        val files = listOf(
            ExportFile("part-1.gpx", "one".toByteArray()),
            ExportFile("part-2.gpx", "two".toByteArray()),
        )
        val bytes = ZipExporter.zipBytes(files)

        val entries = unzipEntries(bytes)

        assertEquals(listOf("part-1.gpx", "part-2.gpx"), entries.keys.toList())
        assertEquals(files.associate { it.fileName to it.bytes.toList() }, entries.mapValues { it.value.toList() })
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun writesExportFileToCreatedDirectory() {
        val directory = Files.createTempDirectory("export-files-test").toFile().resolve("nested")
        val exportFile = ExportFile("part-3.gpx", "three".toByteArray())

        try {
            assertFalse(directory.exists())

            val writtenFile = writeExportFile(directory, exportFile)

            assertTrue(directory.isDirectory)
            assertEquals(directory.resolve(exportFile.fileName), writtenFile)
            assertEquals(exportFile.bytes.toList(), writtenFile.readBytes().toList())
        } finally {
            directory.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun zipExporterRejectsUnsafeNames() {
        unsafeExportFiles().forEach { file ->
            assertRejectsIllegalArgument(file.fileName) {
                ZipExporter.zipBytes(listOf(file))
            }
        }
    }

    @Test
    fun writeExportFileRejectsUnsafeNames() {
        val root = Files.createTempDirectory("export-files-test").toFile()

        try {
            unsafeExportFiles().forEach { file ->
                assertRejectsIllegalArgument(file.fileName) {
                    writeExportFile(File(root, "nested"), file)
                }
            }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun splitResult(index: Int, documentName: String = "Track"): SplitResult =
        SplitResult(
            index = index,
            document = GpxDocument(
                name = documentName,
                tracks = listOf(
                    Track(
                        name = "track-$index",
                        segments = listOf(
                            TrackSegment(
                                points = listOf(
                                    TrackPoint(
                                        latitude = 10.0 + index,
                                        longitude = 20.0 + index,
                                        elevationMeters = 100.0 + index,
                                        time = "2024-01-0${index}T00:00:00Z",
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            pointCount = 1,
            distanceMeters = 123.0,
        )

    private fun unzipEntries(bytes: ByteArray): LinkedHashMap<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read = zip.read(buffer)
                while (read >= 0) {
                    output.write(buffer, 0, read)
                    read = zip.read(buffer)
                }
                entries[entry.name] = output.toByteArray()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return LinkedHashMap(entries)
    }

    private fun unsafeExportFiles(): List<ExportFile> = listOf(
        ExportFile("", "x".toByteArray()),
        ExportFile("   ", "x".toByteArray()),
        ExportFile("../part.gpx", "x".toByteArray()),
        ExportFile("nested/part.gpx", "x".toByteArray()),
        ExportFile("nested\\part.gpx", "x".toByteArray()),
        ExportFile(File("/tmp/part.gpx").path, "x".toByteArray()),
    )

    private fun assertRejectsIllegalArgument(fileName: String, block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException for '$fileName'")
        } catch (_: IllegalArgumentException) {
        }
    }
}
