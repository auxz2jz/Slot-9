package com.zaksecurity.dvrplayer.diagnostics

import android.content.Context
import android.net.Uri
import com.zaksecurity.dvrplayer.testing.GuidedTestController
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DiagnosticExporter {
    fun export(
        context: Context,
        destination: Uri,
        logger: DiagnosticLogger,
        testController: GuidedTestController,
        deviceInfo: Map<String, String>,
        mediaInfo: Map<String, String>
    ) {
        val output =
            requireNotNull(
                context.contentResolver.openOutputStream(destination)
            ) {
                "Unable to open diagnostic export destination."
            }

        output.use { raw ->
            ZipOutputStream(raw).use { zip ->
                val crashText =
                    logger.lastCrashText()

                putText(
                    zip,
                    "README.txt",
                    """
                    DVR Video Player diagnostic package

                    This package was created locally after the user explicitly chose an export destination.
                    It does not contain the source video.
                    Read summary.txt first, then events.jsonl for the detailed current-session event timeline.
                    If previous_crash.json exists, previous_crashed_session_events.jsonl contains the event trail that led to that crash.

                    sessionId=${logger.sessionId}
                    sessionLabel=${logger.sessionLabel}
                    currentEventFile=${logger.currentEventFileName()}
                    exportedUtc=${Instant.now()}
                    """.trimIndent() + "\n"
                )

                putText(
                    zip,
                    "summary.txt",
                    buildString {
                        appendLine("sessionId=${logger.sessionId}")
                        appendLine("sessionLabel=${logger.sessionLabel}")
                        appendLine(
                            "currentEventFile=${logger.currentEventFileName()}"
                        )
                        appendLine("exportedUtc=${Instant.now()}")
                        append(testController.summaryText())
                        appendLine(
                            "media=${mediaInfo["displayName"] ?: "none"}"
                        )
                        appendLine(
                            "device=${deviceInfo["manufacturer"].orEmpty()} " +
                                deviceInfo["model"].orEmpty()
                        )
                        appendLine(
                            "previousCrashPresent=${crashText.isNotBlank()}"
                        )
                    }
                )

                putText(
                    zip,
                    "device_app_info.txt",
                    DeviceInfoCollector.asText(deviceInfo)
                )
                putText(
                    zip,
                    "media_info.txt",
                    MediaInfoCollector.asText(mediaInfo)
                )
                putText(
                    zip,
                    "events.jsonl",
                    logger.eventsText()
                )
                putText(
                    zip,
                    "guided_test_results.json",
                    testController.resultsJson()
                )

                val errors =
                    logger.errorLines()

                if (errors.isNotBlank()) {
                    putText(
                        zip,
                        "errors.txt",
                        errors + "\n"
                    )
                }

                if (crashText.isNotBlank()) {
                    putText(
                        zip,
                        "previous_crash.json",
                        crashText + "\n"
                    )

                    val crashedSessionEvents =
                        logger.crashedSessionEventsText()

                    if (
                        crashedSessionEvents.isNotBlank()
                    ) {
                        putText(
                            zip,
                            "previous_crashed_session_events.jsonl",
                            crashedSessionEvents
                        )
                    }
                }
            }
        }
    }

    private fun putText(
        zip: ZipOutputStream,
        name: String,
        text: String
    ) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(
            text.toByteArray(Charsets.UTF_8)
        )
        zip.closeEntry()
    }
}
