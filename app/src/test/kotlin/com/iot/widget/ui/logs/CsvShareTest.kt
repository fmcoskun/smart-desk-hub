package com.iot.widget.ui.logs

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iot.widget.testutil.fixedClock
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CsvShareTest {

    private val share = CsvShare(ApplicationProvider.getApplicationContext(), fixedClock())

    @Test
    fun writesBomUtf8FileWithTimestampName() {
        val file = share.write(Char(0xFEFF) + "time,message\n1,ş\n")
        // fixedClock = 2026-10-27T14:21:34Z, shown in Istanbul time (+03:00)
        assertEquals("smartdesk-logs-20261027-1721.csv", file.name)
        assertEquals("exports", file.parentFile?.name)
        val bytes = file.readBytes()
        assertArrayEquals(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()), bytes.copyOfRange(0, 3))
        assertTrue(String(bytes, Charsets.UTF_8).endsWith("1,ş\n"))
    }

    @Test
    fun shareIntentHasCsvTypeAndReadGrant() {
        // uriFor() is checked on the emulator: FileProvider compares paths with '/', which
        // never matches Robolectric's Windows paths
        val link = Uri.parse("content://com.iot.widget.files/exports/a.csv")
        val intent = share.shareIntent(link, "a.csv")
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/csv", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        assertEquals(link, uri)
        assertEquals("a.csv", intent.getStringExtra(Intent.EXTRA_SUBJECT))
    }
}
