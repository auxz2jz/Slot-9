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
                putText(
                    zip,
                    "README.txt",
                    """
                    DVR Video Player diagnostic package

                    This package was created locally after the user explicitly chose an export destination.
                    It does not contain the source video.
                    Read summary.txt first, then events.jsonl for the detailed event timeline.

                    sessionId=${logger.sessionId}
                    sessionLabel=${logger.sessionLabel}
                    exportedUtc=${Instant.now()}
                    """.trimIndent() + "\n"
                )

                putText(
                    zip,
                    "summary.txt",
                    buildString {
                        appendLine("sessionId=${logger.sessionId}")
                        appendLine("sessionLabel=${logger.sessionLabel}")
                        appendLine("exportedUtc=${Instant.now()}")
                        append(testController.summaryText())
                        appendLine(
                            "media=${mediaInfo["displayName"] ?: "none"}"
                        )
                        appendLine(
                            "device=${deviceInfo["manufacturer"].orEmpty()} " +
                                deviceInfo["model"].orEmpty()
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

                val errors = logger.errorLines()
                if (errors.isNotBlank()) {
                    putText(
                        zip,
                        "errors.txt",
                        errors + "\n"
                    )
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
        zip.write(text.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
}
