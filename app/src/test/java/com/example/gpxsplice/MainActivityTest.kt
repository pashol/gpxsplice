package com.example.gpxsplice

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityTest {
    @Test
    fun formatsMergeImportProgressMessage() {
        assertEquals("Opening 3 GPX files...", formatMergeImportProgressMessage(3))
        assertEquals("Opening GPX files...", formatMergeImportProgressMessage(0))
    }
}
