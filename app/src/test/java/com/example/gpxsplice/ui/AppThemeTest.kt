package com.example.gpxsplice.ui

import com.example.gpxsplice.ui.theme.shouldUseDynamicColor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeTest {
    @Test
    fun usesDynamicColorOnlyWhenEnabledOnAndroid12AndAbove() {
        assertFalse(shouldUseDynamicColor(dynamicColorEnabled = false, sdkInt = 31))
        assertFalse(shouldUseDynamicColor(dynamicColorEnabled = true, sdkInt = 30))
        assertTrue(shouldUseDynamicColor(dynamicColorEnabled = true, sdkInt = 31))
    }
}
