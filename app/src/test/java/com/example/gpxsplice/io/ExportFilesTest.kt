package com.example.gpxsplice.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class ExportFilesTest {
    @Test
    fun createsZipWithAllEntries() {
        val bytes = ZipExporter.zipBytes(
            listOf(
                ExportFile("part-1.gpx", "one".toByteArray()),
                ExportFile("part-2.gpx", "two".toByteArray()),
            ),
        )

        val names = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                names.add(entry.name)
                entry = zip.nextEntry
            }
        }

        assertEquals(listOf("part-1.gpx", "part-2.gpx"), names)
        assertTrue(bytes.isNotEmpty())
    }
}
