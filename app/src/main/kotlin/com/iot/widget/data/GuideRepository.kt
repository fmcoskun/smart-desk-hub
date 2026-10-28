package com.iot.widget.data

import android.content.Context
import com.iot.widget.R
import com.iot.widget.domain.guide.GuideTopic
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Data layer · the User Guide topics (static content shipped with the app). */
interface GuideRepository {
    suspend fun topics(): List<GuideTopic>
}

/**
 * Reads `res/raw/guide.json` once, on a background thread, and keeps the result. Keeping
 * the text in a JSON resource (not in Kotlin) means writers can change the guide without
 * touching code; a unit test parses the real file so a typo fails the build, not the app.
 */
@Singleton
class RawGuideRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : GuideRepository {
    private val lock = Mutex()
    private var cache: List<GuideTopic>? = null

    override suspend fun topics(): List<GuideTopic> = lock.withLock {
        cache ?: withContext(Dispatchers.IO) {
            val text = context.resources.openRawResource(R.raw.guide).bufferedReader().use { it.readText() }
            Json.decodeFromString<List<GuideTopic>>(text)
        }.also { cache = it }
    }
}
