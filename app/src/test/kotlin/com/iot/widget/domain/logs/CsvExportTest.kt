package com.iot.widget.domain.logs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CsvExportTest {

    private val ist = ZoneId.of("Europe/Istanbul")
    private val t = ZonedDateTime.of(2026, 10, 28, 14, 31, 5, 0, ist).toInstant().toEpochMilli()
    private val bom = Char(0xFEFF)

    @Test
    fun headerOrder() {
        val lines = CsvExport.format(emptyList(), ist).removePrefix(bom.toString()).lines()
        assertEquals("time,source,level,actor,device,message,topic,payload", lines.first())
    }

    @Test
    fun csvEscapesAndStartsWithBom() {
        val entry = LogEntry(
            id = 1, timeMillis = t, source = LogSource.CLIENT, level = LogLevel.WARN,
            actor = "Ayşe Yılmaz", deviceId = null, deviceName = "Desk Lamp, left",
            message = "said \"hi\"\nthen left", topic = "office/desk/lights", payload = """{"state":"ON"}""",
        )
        val csv = CsvExport.format(listOf(entry), ist)
        assertTrue(csv.startsWith(bom.toString()))
        val body = csv.removePrefix(bom.toString()).substringAfter('\n')
        assertEquals(
            "2026-10-28T14:31:05+03:00,CLIENT,WARN,Ayşe Yılmaz,\"Desk Lamp, left\",\"said \"\"hi\"\"\nthen left\"," +
                "office/desk/lights,\"{\"\"state\"\":\"\"ON\"\"}\"\n",
            body,
        )
    }

    @Test
    fun emptyOptionalFieldsAreEmpty() {
        val entry = LogEntry(2, t, LogSource.DEVICE, LogLevel.INFO, null, "cam_desk", "Desk Camera", "telemetry ok", null, null)
        val row = CsvExport.format(listOf(entry), ist).lines()[1]
        assertEquals("2026-10-28T14:31:05+03:00,DEVICE,INFO,,Desk Camera,telemetry ok,,", row)
    }


    @Test
    fun csvNeutralisesFormulas() {
        // a device or user name like "=HYPERLINK(...)" must not become a live formula in Excel
        val e = LogEntry(3, t, LogSource.CLIENT, LogLevel.INFO, "@admin", null, "=HYPERLINK(\"http://x\")", "+cmd", null, "-1")
        val fields = CsvExport.format(listOf(e), ist).removePrefix(bom.toString()).lines()[1]
        assertTrue(fields, fields.contains(",'@admin,"))
        assertTrue(fields, fields.contains("\"'=HYPERLINK(\"\"http://x\"\")\""))
        assertTrue(fields, fields.contains(",'+cmd,"))
        assertTrue(fields, fields.endsWith(",'-1"))
    }
}
