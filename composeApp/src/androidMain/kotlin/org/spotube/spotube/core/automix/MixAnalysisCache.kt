package org.spotube.spotube.core.automix

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

private const val MIX_ANALYSIS_CACHE_VERSION = 3

@Serializable
private data class MixAnalysisFile(
    val version: Int = MIX_ANALYSIS_CACHE_VERSION,
    val entries: Map<String, AudioFeatures> = emptyMap()
)

class MixAnalysisCache(context: Context) {
    private val file = File(context.filesDir, "automix_analysis.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val mutex = Mutex()
    private var entries: MutableMap<String, AudioFeatures>? = null

    suspend fun get(trackId: String): AudioFeatures? = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadLocked()[trackId]
        }
    }

    suspend fun put(trackId: String, features: AudioFeatures) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = loadLocked()
            current[trackId] = features
            file.writeText(
                json.encodeToString(
                    MixAnalysisFile.serializer(),
                    MixAnalysisFile(entries = current.toMap())
                )
            )
        }
    }

    private fun loadLocked(): MutableMap<String, AudioFeatures> {
        entries?.let { return it }
        val loaded = runCatching {
            if (!file.exists()) return@runCatching mutableMapOf<String, AudioFeatures>()
            val parsed = json.decodeFromString(MixAnalysisFile.serializer(), file.readText())
            if (parsed.version != MIX_ANALYSIS_CACHE_VERSION) {
                mutableMapOf()
            } else {
                parsed.entries.toMutableMap()
            }
        }.getOrDefault(mutableMapOf())
        entries = loaded
        return loaded
    }

}
