package com.zaksecurity.dvrplayer.diagnostics

import android.content.Context
import android.os.SystemClock
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class DiagnosticLogger(context: Context) {
    private val directory = File(context.cacheDir, "dvr_diagnostics").apply { mkdirs() }
    private val eventFile = File(directory, "events.jsonl")
    private val sequence = AtomicLong(0L)

    var sessionId: String = ""
        private set

    var sessionLabel: String = ""
        private set

    private var sessionStartElapsedMs: Long = 0L

    init {
        startSession("app_session")
    }

    @Synchronized
    fun startSession(label: String) {
        sessionId = UUID.randomUUID().toString()
        sessionLabel = label
        sessionStartElapsedMs = SystemClock.elapsedRealtime()
        sequence.set(0L)
        eventFile.writeText("")
        log(
            category = "SESSION",
            event = "SESSION_STARTED",
            details = mapOf("label" to label)
        )
    }

    @Synchronized
    fun log(
        category: String,
        event: String,
        mediaPositionMs: Long? = null,
        details: Map<String, Any?> = emptyMap()
    ) {
        val json = JSONObject()
        json.put("sessionId", sessionId)
        json.put("sequence", sequence.incrementAndGet())
        json.put("timestampUtc", Instant.now().toString())
        json.put("elapsedMs", elapsedMs())
        json.put("category", category)
        json.put("event", event)
        if (mediaPositionMs != null) {
            json.put("mediaPositionMs", mediaPositionMs)
        }

        val detailsJson = JSONObject()
        details.forEach { (key, value) ->
            detailsJson.put(key, value ?: JSONObject.NULL)
        }
        json.put("details", detailsJson)

        eventFile.appendText(json.toString() + "\n")
    }

    fun logError(
        event: String,
        throwable: Throwable,
        mediaPositionMs: Long? = null,
        details: Map<String, Any?> = emptyMap()
    ) {
        val merged = LinkedHashMap<String, Any?>()
        merged.putAll(details)
        merged["exceptionType"] = throwable::class.java.name
        merged["message"] = throwable.message.orEmpty()
        merged["stackTrace"] = throwable.stackTraceToString()
        log(
            category = "ERROR",
            event = event,
            mediaPositionMs = mediaPositionMs,
            details = merged
        )
    }

    fun elapsedMs(): Long =
        (SystemClock.elapsedRealtime() - sessionStartElapsedMs).coerceAtLeast(0L)

    fun eventsText(): String =
        if (eventFile.exists()) eventFile.readText() else ""

    fun errorLines(): String {
        if (!eventFile.exists()) return ""
        return eventFile.useLines { lines ->
            lines.mapNotNull { line ->
                runCatching {
                    val obj = JSONObject(line)
                    if (obj.optString("category") == "ERROR") line else null
                }.getOrNull()
            }.joinToString("\n")
        }
    }
}
