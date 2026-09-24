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
    fun `initial session lines do not fake ubuntu identity`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lines = TerminalEngine.createInitialSessionLines(context)
        assertTrue(lines.isNotEmpty())
        // Fase 8: la UI inicial no fabrica datos de Ubuntu
        assertTrue(lines.none { it.text.contains("neofetch") })
        assertTrue(lines.none { it.text.contains("root@ubuntu") })
        assertTrue(lines.none { it.text.contains("24.04.5") })
    }

    @Test
    fun `engine without session reports real bootstrap state instead of fake output`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = TerminalEngine.executeCommand("uname -a", "~", context)
        assertNotNull(result)
        // Sin rootfs ni PRoot disponibles, el motor informa el estado real del bootstrap
        assertTrue(result.lines.any { it.text.contains("[motor]") })
    }

    @Test
    fun `system monitor reports only real values`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val stats = SystemMonitor.getRealStats(context)
        assertNotNull(stats)
        // Sin rootfs en Robolectric: N/D real, nunca un valor inventado
        assertEquals("N/D", stats.osVersion)
        assertEquals(0, stats.packagesCount)
        assertEquals(0.0, stats.rootfsSizeMb, 0.001)
        // RAM y almacenamiento consistentes con lo que reporta la plataforma (Robolectric: valores por defecto)
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val mi = android.app.ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        assertEquals(mi.totalMem / (1024 * 1024), stats.ramTotalMb)
        assertTrue(stats.storageUsedPercent >= 1)
    }
}
