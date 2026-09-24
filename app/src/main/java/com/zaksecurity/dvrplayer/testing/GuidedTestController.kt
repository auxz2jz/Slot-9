package com.zaksecurity.dvrplayer.testing

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zaksecurity.dvrplayer.diagnostics.DiagnosticLogger
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

enum class GuidedStepType {
    OPEN_MEDIA,
    PLAY,
    PAUSE,
    SEEK
}

data class GuidedStep(
    val id: String,
    val title: String,
    val instruction: String,
    val type: GuidedStepType
)

data class GuidedStepResult(
    val id: String,
    val status: String,
    val durationMs: Long,
    val message: String
)

class GuidedTestController {
    val steps = listOf(
        GuidedStep(
            id = "open_media",
            title = "Open a video",
            instruction = "Tap Open Video and choose a local video file.",
            type = GuidedStepType.OPEN_MEDIA
        ),
        GuidedStep(
            id = "play",
            title = "Play",
            instruction = "Tap Play and let the video run for at least 2 seconds.",
            type = GuidedStepType.PLAY
        ),
        GuidedStep(
            id = "pause",
            title = "Pause",
            instruction = "Tap Pause.",
            type = GuidedStepType.PAUSE
        ),
        GuidedStep(
            id = "seek",
            title = "Seek",
            instruction = "Drag the timeline to a noticeably different point and release it.",
            type = GuidedStepType.SEEK
        )
    )

    var active by mutableStateOf(false)
        private set

    var completed by mutableStateOf(false)
        private set

    var overallStatus by mutableStateOf("NOT_STARTED")
        private set

    var currentIndex by mutableIntStateOf(0)
        private set

    var statusMessage by mutableStateOf("Not started")
        private set

    val results = mutableStateListOf<GuidedStepResult>()

    private var stepStartedElapsedMs: Long = 0L
    private var playRequestedAtMs: Long? = null
    private var playStartPositionMs: Long? = null
    private var pauseRequestedAtMs: Long? = null
    private var seekRequestedAtMs: Long? = null
    private var seekTargetMs: Long? = null

    val currentStep: GuidedStep?
        get() = if (active && currentIndex in steps.indices) steps[currentIndex] else null

    fun start(logger: DiagnosticLogger) {
        active = true
        completed = false
        overallStatus = "IN_PROGRESS"
        currentIndex = 0
        results.clear()
        resetTransientState()
        stepStartedElapsedMs = SystemClock.elapsedRealtime()
        statusMessage = "Waiting for media to be opened"
        logger.log(
            category = "TEST",
            event = "GUIDED_TEST_STARTED",
            details = mapOf(
                "testId" to "baseline_playback_seek_v1",
                "stepCount" to steps.size
            )
        )
        logStepStarted(logger)
    }

    fun onMediaReady(logger: DiagnosticLogger) {
        if (currentStep?.type == GuidedStepType.OPEN_MEDIA) {
            passCurrent(logger, "Media opened and player reached a ready state.")
        }
    }

    fun onPlayRequested(logger: DiagnosticLogger) {
        if (currentStep?.type != GuidedStepType.PLAY) return
        playRequestedAtMs = SystemClock.elapsedRealtime()
        playStartPositionMs = null
        statusMessage =
            "Play was requested; waiting for confirmed playback and position advance."
        logger.log(category = "TEST", event = "PLAY_TEST_REQUEST_RECORDED")
    }

    fun onPauseRequested(logger: DiagnosticLogger) {
        if (currentStep?.type != GuidedStepType.PAUSE) return
        pauseRequestedAtMs = SystemClock.elapsedRealtime()
        statusMessage =
            "Pause was requested; waiting for confirmed paused state."
        logger.log(category = "TEST", event = "PAUSE_TEST_REQUEST_RECORDED")
    }

    fun onSeekRequested(
        startPositionMs: Long,
        targetPositionMs: Long,
        logger: DiagnosticLogger
    ) {
        if (currentStep?.type != GuidedStepType.SEEK) return

        if (abs(targetPositionMs - startPositionMs) < 1_500L) {
            statusMessage =
                "Move the timeline farther so the seek can be verified."
            logger.log(
                category = "TEST",
                event = "SEEK_TEST_MOVE_TOO_SMALL",
                mediaPositionMs = startPositionMs,
                details = mapOf(
                    "targetPositionMs" to targetPositionMs,
                    "differenceMs" to abs(targetPositionMs - startPositionMs)
                )
            )
            return
        }

        seekRequestedAtMs = SystemClock.elapsedRealtime()
        seekTargetMs = targetPositionMs
        statusMessage =
            "Seek requested; waiting for player position confirmation."
        logger.log(
            category = "TEST",
            event = "SEEK_TEST_REQUEST_RECORDED",
            mediaPositionMs = startPositionMs,
            details = mapOf("targetPositionMs" to targetPositionMs)
        )
    }

    fun onPlayerSample(
        isPlaying: Boolean,
        positionMs: Long,
        logger: DiagnosticLogger
    ) {
        when (currentStep?.type) {
            GuidedStepType.PLAY ->
                evaluatePlay(isPlaying, positionMs, logger)
            GuidedStepType.PAUSE ->
                evaluatePause(isPlaying, logger)
            GuidedStepType.SEEK ->
                evaluateSeek(positionMs, logger)
            else -> Unit
        }
    }

    fun stopAsFailure(logger: DiagnosticLogger, reason: String) {
        if (!active) return
        val step = currentStep
        if (step != null) {
            results += GuidedStepResult(
                id = step.id,
                status = "FAIL",
                durationMs = stepDurationMs(),
                message = reason
            )
            logger.log(
                category = "TEST",
                event = "STEP_FAILED",
                details = mapOf(
                    "stepId" to step.id,
                    "reason" to reason
                )
            )
        }
        active = false
        completed = true
        overallStatus = "FAIL"
        statusMessage = reason
        logger.log(
            category = "TEST",
            event = "GUIDED_TEST_FINISHED",
            details = mapOf("result" to overallStatus)
        )
    }

    private fun evaluatePlay(
        isPlaying: Boolean,
        positionMs: Long,
        logger: DiagnosticLogger
    ) {
        val requestedAt = playRequestedAtMs ?: return

        if (isPlaying) {
            if (playStartPositionMs == null) {
                playStartPositionMs = positionMs
                logger.log(
                    category = "TEST",
                    event = "PLAYBACK_CONFIRMED_STARTED",
                    mediaPositionMs = positionMs
                )
            }

            val start = playStartPositionMs ?: positionMs
            if (positionMs - start >= 1_500L) {
                passCurrent(
                    logger,
                    "Playback was confirmed and media position advanced."
                )
                return
            }
        }

        if (SystemClock.elapsedRealtime() - requestedAt > 8_000L) {
            stopAsFailure(
                logger,
                "Play was requested but confirmed playback/position advance did not occur within 8 seconds."
            )
        }
    }

    private fun evaluatePause(
        isPlaying: Boolean,
        logger: DiagnosticLogger
    ) {
        val requestedAt = pauseRequestedAtMs ?: return

        if (!isPlaying) {
            passCurrent(
                logger,
                "Player confirmed the paused state."
            )
            return
        }

        if (SystemClock.elapsedRealtime() - requestedAt > 5_000L) {
            stopAsFailure(
                logger,
                "Pause was requested but the player did not confirm a paused state within 5 seconds."
            )
        }
    }

    private fun evaluateSeek(
        positionMs: Long,
        logger: DiagnosticLogger
    ) {
        val requestedAt = seekRequestedAtMs ?: return
        val target = seekTargetMs ?: return

        if (abs(positionMs - target) <= 1_500L) {
            passCurrent(
                logger,
                "Player position reached the requested seek target within tolerance."
            )
            return
        }

        if (SystemClock.elapsedRealtime() - requestedAt > 8_000L) {
            stopAsFailure(
                logger,
                "Seek was requested but the player did not reach the requested target within 8 seconds."
            )
        }
    }

    private fun passCurrent(
        logger: DiagnosticLogger,
        message: String
    ) {
        val step = currentStep ?: return

        results += GuidedStepResult(
            id = step.id,
            status = "PASS",
            durationMs = stepDurationMs(),
            message = message
        )

        logger.log(
            category = "TEST",
            event = "STEP_PASSED",
            details = mapOf(
                "stepId" to step.id,
                "message" to message
            )
        )

        currentIndex += 1
        resetTransientState()

        if (currentIndex >= steps.size) {
            active = false
            completed = true
            overallStatus = "PASS"
            statusMessage =
                "All baseline guided-test steps passed."
            logger.log(
                category = "TEST",
                event = "GUIDED_TEST_FINISHED",
                details = mapOf("result" to overallStatus)
            )
        } else {
            stepStartedElapsedMs = SystemClock.elapsedRealtime()
            statusMessage =
                "Waiting for: ${steps[currentIndex].title}"
            logStepStarted(logger)
        }
    }

    private fun logStepStarted(logger: DiagnosticLogger) {
        val step = currentStep ?: return
        logger.log(
            category = "TEST",
            event = "STEP_STARTED",
            details = mapOf(
                "stepId" to step.id,
                "title" to step.title,
                "instruction" to step.instruction
            )
        )
    }

    private fun resetTransientState() {
        playRequestedAtMs = null
        playStartPositionMs = null
        pauseRequestedAtMs = null
        seekRequestedAtMs = null
        seekTargetMs = null
    }

    private fun stepDurationMs(): Long =
        (SystemClock.elapsedRealtime() - stepStartedElapsedMs)
            .coerceAtLeast(0L)

    fun resultsJson(): String {
        val root = JSONObject()
        root.put("testId", "baseline_playback_seek_v1")
        root.put("overallStatus", overallStatus)
        root.put("active", active)
        root.put("completed", completed)

        val array = JSONArray()
        results.forEach { result ->
            array.put(
                JSONObject()
                    .put("stepId", result.id)
                    .put("status", result.status)
                    .put("durationMs", result.durationMs)
                    .put("message", result.message)
            )
        }

        currentStep?.let { step ->
            root.put(
                "currentStep",
                JSONObject()
                    .put("stepId", step.id)
                    .put("title", step.title)
                    .put("instruction", step.instruction)
            )
        }

        root.put("results", array)
        return root.toString(2)
    }

    fun summaryText(): String =
        buildString {
            appendLine("testId=baseline_playback_seek_v1")
            appendLine("overallStatus=$overallStatus")
            appendLine("completed=$completed")
            results.forEach { result ->
                appendLine(
                    "step.${result.id}=${result.status}; " +
                        "durationMs=${result.durationMs}; ${result.message}"
                )
            }
            currentStep?.let {
                appendLine("currentStep=${it.id}")
            }
        }
}
