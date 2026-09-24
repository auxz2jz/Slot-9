package com.zaksecurity.dvrplayer.diagnostics

import android.content.Context
import android.os.SystemClock
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.exitProcess

class DiagnosticLogger(context: Context) {
    private val directory =
        File(context.cacheDir, "dvr_diagnostics").apply {
            mkdirs()
        }

    private val lastCrashFile =
        File(directory, "last_crash.json")

    private val sequence = AtomicLong(0L)

    private var eventFile: File =
        File(directory, "events_bootstrap.jsonl")

    var sessionId: String = ""
        private set

    var sessionLabel: String = ""
        private set

    private var sessionStartElapsedMs: Long = 0L

    init {
        startSession("app_session")
        installCrashHandler()
    }

    @Synchronized
    fun startSession(label: String) {
        sessionId = UUID.randomUUID().toString()
        sessionLabel = label
        sessionStartElapsedMs = SystemClock.elapsedRealtime()
        sequence.set(0L)

        eventFile =
            File(
                directory,
                "events_$sessionId.jsonl"
            )

        eventFile.writeText("")

        log(
            category = "SESSION",
            event = "SESSION_STARTED",
            details = mapOf(
                "label" to label,
                "eventFile" to eventFile.name
            )
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
            json.put(
                "mediaPositionMs",
                mediaPositionMs
            )
        }

        val detailsJson = JSONObject()
        details.forEach { (key, value) ->
            detailsJson.put(
                key,
                value ?: JSONObject.NULL
            )
        }
        json.put("details", detailsJson)

        eventFile.appendText(
            json.toString() + "\n"
        )
    }

    fun logError(
        event: String,
        throwable: Throwable,
        mediaPositionMs: Long? = null,
        details: Map<String, Any?> = emptyMap()
    ) {
        val merged =
            LinkedHashMap<String, Any?>()

        merged.putAll(details)
        merged["exceptionType"] =
            throwable::class.java.name
        merged["message"] =
            throwable.message.orEmpty()
        merged["stackTrace"] =
            throwable.stackTraceToString()

        log(
            category = "ERROR",
            event = event,
            mediaPositionMs = mediaPositionMs,
            details = merged
        )
    }

    fun elapsedMs(): Long =
        (
            SystemClock.elapsedRealtime() -
                sessionStartElapsedMs
            ).coerceAtLeast(0L)

    fun eventsText(): String =
        if (eventFile.exists()) {
            eventFile.readText()
        } else {
            ""
        }

    fun currentEventFileName(): String =
        eventFile.name

    fun errorLines(): String {
        if (!eventFile.exists()) return ""

        return eventFile.useLines { lines ->
            lines.mapNotNull { line ->
                runCatching {
                    val obj = JSONObject(line)
                    if (
                        obj.optString("category") ==
                        "ERROR"
                    ) {
                        line
                    } else {
                        null
                    }
                }.getOrNull()
            }.joinToString("\n")
        }
    }

    fun lastCrashText(): String =
        if (lastCrashFile.exists()) {
            lastCrashFile.readText()
        } else {
            ""
        }

    fun crashedSessionEventsText(): String {
        if (!lastCrashFile.exists()) {
            return ""
        }

        val eventFileName =
            runCatching {
                JSONObject(
                    lastCrashFile.readText()
                ).optString("eventFile")
            }.getOrDefault("")

        if (eventFileName.isBlank()) {
            return ""
        }

        val crashedEvents =
            File(directory, eventFileName)

        return if (crashedEvents.exists()) {
            crashedEvents.readText()
        } else {
            ""
        }
    }

    private fun installCrashHandler() {
        val previousHandler =
            Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler {
            thread,
            throwable ->

            runCatching {
                logError(
                    event = "UNCAUGHT_EXCEPTION",
                    throwable = throwable,
                    details = mapOf(
                        "threadName" to thread.name
                    )
                )

                val crash = JSONObject()
                crash.put("timestampUtc", Instant.now().toString())
                crash.put("sessionId", sessionId)
                crash.put("sessionLabel", sessionLabel)
                crash.put("eventFile", eventFile.name)
                crash.put("threadName", thread.name)
                crash.put(
                    "exceptionType",
                    throwable::class.java.name
                )
                crash.put(
                    "message",
                    throwable.message.orEmpty()
                )
                crash.put(
                    "stackTrace",
                    throwable.stackTraceToString()
                )

                lastCrashFile.writeText(
                    crash.toString(2)
                )
            }

            if (previousHandler != null) {
                previousHandler.uncaughtException(
                    thread,
                    throwable
                )
            } else {
                exitProcess(10)
            }
        }
    }
}
