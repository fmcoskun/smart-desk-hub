package com.iot.widget.ui.logs

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * UI layer · hands a CSV file to another app (Gmail, Drive, Sheets…) through the share sheet.
 *
 * Other apps cannot read our private files, so the file is written to the cache and shared as
 * a `content://` link from a [FileProvider] (declared in the manifest, paths in
 * `res/xml/file_paths.xml`). The receiving app gets a one-off read grant, nothing more.
 */
class CsvShare @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    /** Writes [csv] as UTF-8 to `cache/exports/`, replacing earlier exports (no pile-up). */
    fun write(csv: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val name = "smartdesk-logs-" + STAMP.format(clock.instant().atZone(clock.zone)) + ".csv"
        return File(dir, name).apply { writeText(csv, Charsets.UTF_8) }
    }

    fun shareIntent(file: File): Intent = shareIntent(uriFor(file), file.name)

    /** `content://com.iot.widget.files/exports/<name>` */
    fun uriFor(file: File): Uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)

    /** Kept apart from [uriFor] so it can be unit-tested: FileProvider's root check fails on Windows hosts. */
    internal fun shareIntent(uri: Uri, name: String): Intent =
        Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, name)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    private companion object {
        val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm", Locale.US)
    }
}
