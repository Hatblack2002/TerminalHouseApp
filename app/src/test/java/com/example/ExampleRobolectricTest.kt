package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.SystemMonitor
import com.example.service.TerminalEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TerminalHouse", appName)
    }

    @Test
    fun `test terminal initial session output`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lines = TerminalEngine.createInitialSessionLines(context)
        assertTrue(lines.isNotEmpty())
        assertTrue(lines.any { it.text.contains("neofetch") })
        assertTrue(lines.any { it.text.contains("Ubuntu 24.04.5") })
    }

    @Test
    fun `test terminal command execution`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = TerminalEngine.executeCommand("uname -a", "~", context)
        assertNotNull(result)
        assertTrue(result.lines.any { it.text.contains("Linux") })
    }

    @Test
    fun `test system monitor real stats`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val stats = SystemMonitor.getRealStats(context)
        assertNotNull(stats)
        assertTrue(stats.osVersion.contains("24.04.5"))
        assertTrue(stats.ramUsedPercent > 0)
        assertTrue(stats.storageUsedPercent > 0)
    }
}
