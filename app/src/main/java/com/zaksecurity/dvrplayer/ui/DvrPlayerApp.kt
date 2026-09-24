package com.zaksecurity.dvrplayer.ui

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.zaksecurity.dvrplayer.diagnostics.DeviceInfoCollector
import com.zaksecurity.dvrplayer.diagnostics.DiagnosticExporter
import com.zaksecurity.dvrplayer.diagnostics.DiagnosticLogger
import com.zaksecurity.dvrplayer.diagnostics.MediaInfoCollector
import com.zaksecurity.dvrplayer.testing.GuidedTestController
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToLong

@OptIn(UnstableApi::class)
@Composable
fun DvrPlayerApp() {
    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context).build()
    }
    val logger = remember {
        DiagnosticLogger(context.applicationContext)
    }
    val guidedTest = remember {
        GuidedTestController()
    }

    var deviceInfo by remember {
        mutableStateOf(DeviceInfoCollector.collect(context))
    }
    var mediaInfo by remember {
        mutableStateOf<Map<String, String>>(emptyMap())
    }
    var selectedUri by remember {
        mutableStateOf<Uri?>(null)
    }
    var currentPositionMs by remember {
        mutableLongStateOf(0L)
    }
    var durationMs by remember {
        mutableLongStateOf(0L)
    }
    var isPlaying by remember {
        mutableStateOf(false)
    }
    var isScrubbing by remember {
        mutableStateOf(false)
    }
    var scrubPositionMs by remember {
        mutableLongStateOf(0L)
    }
    var seekStartPositionMs by remember {
        mutableLongStateOf(0L)
    }
    var exportStatus by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {
        logger.log(
            category = "APP",
            event = "APP_UI_STARTED",
            details = mapOf("appVersion" to "0.1.0")
        )
        logger.log(
            category = "DEVICE",
            event = "DEVICE_SNAPSHOT",
            details = deviceInfo
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) {
            logger.log(
                category = "DIAGNOSTIC",
                event = "EXPORT_CANCELLED"
            )
            exportStatus = "Export cancelled."
        } else {
            try {
                logger.log(
                    category = "DIAGNOSTIC",
                    event = "EXPORT_STARTED",
                    details = mapOf(
                        "destinationScheme" to uri.scheme.orEmpty()
                    )
                )

                DiagnosticExporter.export(
                    context = context,
                    destination = uri,
                    logger = logger,
                    testController = guidedTest,
                    deviceInfo = deviceInfo,
                    mediaInfo = mediaInfo
                )

                exportStatus = "Diagnostic ZIP saved."
            } catch (t: Throwable) {
                logger.logError(
                    event = "DIAGNOSTIC_EXPORT_FAILED",
                    throwable = t,
                    mediaPositionMs = player.currentPosition
                )
                exportStatus =
                    "Export failed: ${t.message.orEmpty()}"
            }
        }
    }

    val openVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            logger.log(
                category = "UI_ACTION",
                event = "OPEN_VIDEO_CANCELLED"
            )
            return@rememberLauncherForActivityResult
        }

        logger.log(
            category = "UI_ACTION",
            event = "OPEN_VIDEO_RESULT",
            details = mapOf(
                "uriScheme" to uri.scheme.orEmpty(),
                "uriAuthority" to uri.authority.orEmpty()
            )
        )

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }.onFailure { error ->
            logger.logError(
                event = "PERSISTABLE_READ_PERMISSION_FAILED",
                throwable = error
            )
        }

        selectedUri = uri

        mediaInfo =
            try {
                MediaInfoCollector.collect(context, uri).also { info ->
                    logger.log(
                        category = "MEDIA",
                        event = "MEDIA_METADATA_COLLECTED",
                        details = info
                    )
                }
            } catch (t: Throwable) {
                logger.logError(
                    event = "MEDIA_METADATA_COLLECTION_FAILED",
                    throwable = t
                )
                emptyMap()
            }

        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = false

        logger.log(
            category = "MEDIA",
            event = "MEDIA_ITEM_PREPARE_REQUESTED",
            details = mapOf(
                "displayName" to mediaInfo["displayName"].orEmpty()
            )
        )
    }

    DisposableEffect(player) {
        val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {
                    logger.log(
                        category = "PLAYER",
                        event = "PLAYBACK_STATE_CHANGED",
                        mediaPositionMs = player.currentPosition,
                        details = mapOf(
                            "state" to playbackStateName(playbackState),
                            "playWhenReady" to player.playWhenReady
                        )
                    )

                    if (
                        playbackState == Player.STATE_READY &&
                        player.currentMediaItem != null
                    ) {
                        guidedTest.onMediaReady(logger)
                    }
                }

                override fun onIsPlayingChanged(value: Boolean) {
                    isPlaying = value
                    logger.log(
                        category = "PLAYER",
                        event = "IS_PLAYING_CHANGED",
                        mediaPositionMs = player.currentPosition,
                        details = mapOf("isPlaying" to value)
                    )
                }

                override fun onPlayWhenReadyChanged(
                    playWhenReady: Boolean,
                    reason: Int
                ) {
                    logger.log(
                        category = "PLAYER",
                        event = "PLAY_WHEN_READY_CHANGED",
                        mediaPositionMs = player.currentPosition,
                        details = mapOf(
                            "playWhenReady" to playWhenReady,
                            "reason" to reason
                        )
                    )
                }

                override fun onPlaybackParametersChanged(
                    playbackParameters:
                        androidx.media3.common.PlaybackParameters
                ) {
                    logger.log(
                        category = "SPEED",
                        event = "PLAYBACK_PARAMETERS_CHANGED",
                        mediaPositionMs = player.currentPosition,
                        details = mapOf(
                            "speed" to playbackParameters.speed,
                            "pitch" to playbackParameters.pitch
                        )
                    )
                }

                override fun onVideoSizeChanged(
                    videoSize: VideoSize
                ) {
                    logger.log(
                        category = "PLAYER",
                        event = "VIDEO_SIZE_CHANGED",
                        mediaPositionMs = player.currentPosition,
                        details = mapOf(
                            "width" to videoSize.width,
                            "height" to videoSize.height,
                            "pixelWidthHeightRatio" to
                                videoSize.pixelWidthHeightRatio
                        )
                    )
                }

                override fun onRenderedFirstFrame() {
                    logger.log(
                        category = "PLAYER",
                        event = "FIRST_FRAME_RENDERED",
                        mediaPositionMs = player.currentPosition
                    )
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    logger.log(
                        category = "SEEK",
                        event = "POSITION_DISCONTINUITY",
                        mediaPositionMs = newPosition.positionMs,
                        details = mapOf(
                            "oldPositionMs" to oldPosition.positionMs,
                            "newPositionMs" to newPosition.positionMs,
                            "reason" to reason
                        )
                    )
                }

                override fun onPlayerError(
                    error: PlaybackException
                ) {
                    logger.logError(
                        event = "PLAYER_ERROR",
                        throwable = error,
                        mediaPositionMs = player.currentPosition,
                        details = mapOf(
                            "errorCode" to error.errorCode,
                            "errorCodeName" to error.errorCodeName
                        )
                    )

                    if (guidedTest.active) {
                        guidedTest.stopAsFailure(
                            logger,
                            "Media3 playback error: " +
                                error.errorCodeName
                        )
                    }
                }
            }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        var lastDiagnosticSampleAt = 0L

        while (true) {
            val rawDuration = player.duration

            currentPositionMs =
                player.currentPosition.coerceAtLeast(0L)

            durationMs =
                if (rawDuration > 0L) {
                    rawDuration
                } else {
                    0L
                }

            isPlaying = player.isPlaying

            guidedTest.onPlayerSample(
                isPlaying = player.isPlaying,
                positionMs =
                    player.currentPosition.coerceAtLeast(0L),
                logger = logger
            )

            val now = SystemClock.elapsedRealtime()
            if (
                guidedTest.active &&
                now - lastDiagnosticSampleAt >= 1_000L
            ) {
                lastDiagnosticSampleAt = now
                logger.log(
                    category = "PLAYER",
                    event = "PLAYER_SAMPLE",
                    mediaPositionMs =
                        player.currentPosition.coerceAtLeast(0L),
                    details = mapOf(
                        "durationMs" to durationMs,
                        "bufferedPositionMs" to
                            player.bufferedPosition.coerceAtLeast(0L),
                        "isPlaying" to player.isPlaying,
                        "playWhenReady" to player.playWhenReady,
                        "speed" to player.playbackParameters.speed
                    )
                )
            }

            delay(250L)
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DVR Video Player",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "v0.1.0 — playback + guided diagnostics",
                    style = MaterialTheme.typography.labelMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            logger.log(
                                category = "UI_ACTION",
                                event = "OPEN_VIDEO_PRESSED",
                                mediaPositionMs =
                                    player.currentPosition
                            )
                            openVideoLauncher.launch(
                                arrayOf("video/*")
                            )
                        }
                    ) {
                        Text("Open Video")
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            player.pause()
                            player.stop()
                            player.clearMediaItems()

                            selectedUri = null
                            mediaInfo = emptyMap()
                            currentPositionMs = 0L
                            durationMs = 0L

                            logger.startSession(
                                "baseline_playback_seek_v1"
                            )
                            deviceInfo =
                                DeviceInfoCollector.collect(context)
                            logger.log(
                                category = "DEVICE",
                                event = "DEVICE_SNAPSHOT",
                                details = deviceInfo
                            )
                            guidedTest.start(logger)
                            exportStatus = ""
                        }
                    ) {
                        Text("Guided Test")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { viewContext ->
                            PlayerView(viewContext).apply {
                                useController = false
                                resizeMode =
                                    AspectRatioFrameLayout
                                        .RESIZE_MODE_FIT
                                this.player = player
                            }
                        },
                        update = { view ->
                            view.player = player
                        }
                    )

                    if (selectedUri == null) {
                        Text(
                            text = "Open a video to begin",
                            color = Color.White
                        )
                    }
                }

                Text(
                    text =
                        "${formatTime(if (isScrubbing) scrubPositionMs else currentPositionMs)}" +
                            " / ${formatTime(durationMs)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = normalizedPosition(
                        if (isScrubbing) {
                            scrubPositionMs
                        } else {
                            currentPositionMs
                        },
                        durationMs
                    ),
                    onValueChange = { fraction ->
                        if (!isScrubbing) {
                            isScrubbing = true
                            seekStartPositionMs =
                                currentPositionMs
                            logger.log(
                                category = "SEEK",
                                event = "SEEK_DRAG_STARTED",
                                mediaPositionMs =
                                    currentPositionMs
                            )
                        }

                        scrubPositionMs =
                            (fraction * durationMs.toDouble())
                                .roundToLong()
                                .coerceIn(0L, durationMs)
                    },
                    onValueChangeFinished = {
                        val target =
                            scrubPositionMs
                                .coerceIn(0L, durationMs)

                        logger.log(
                            category = "SEEK",
                            event = "SEEK_RELEASED",
                            mediaPositionMs =
                                seekStartPositionMs,
                            details = mapOf(
                                "targetPositionMs" to target,
                                "differenceMs" to
                                    abs(
                                        target -
                                            seekStartPositionMs
                                    )
                            )
                        )

                        guidedTest.onSeekRequested(
                            startPositionMs =
                                seekStartPositionMs,
                            targetPositionMs = target,
                            logger = logger
                        )

                        player.seekTo(target)

                        logger.log(
                            category = "SEEK",
                            event = "SEEK_REQUEST_SENT",
                            mediaPositionMs =
                                seekStartPositionMs,
                            details = mapOf(
                                "targetPositionMs" to target
                            )
                        )

                        isScrubbing = false
                    },
                    valueRange = 0f..1f,
                    enabled = durationMs > 0L
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = selectedUri != null,
                        onClick = {
                            if (player.isPlaying) {
                                logger.log(
                                    category = "UI_ACTION",
                                    event =
                                        "PAUSE_BUTTON_PRESSED",
                                    mediaPositionMs =
                                        player.currentPosition
                                )
                                guidedTest.onPauseRequested(
                                    logger
                                )
                                player.pause()
                            } else {
                                logger.log(
                                    category = "UI_ACTION",
                                    event =
                                        "PLAY_BUTTON_PRESSED",
                                    mediaPositionMs =
                                        player.currentPosition
                                )
                                guidedTest.onPlayRequested(
                                    logger
                                )
                                player.play()
                            }
                        }
                    ) {
                        Text(
                            if (isPlaying) {
                                "Pause"
                            } else {
                                "Play"
                            }
                        )
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val fileName =
                                diagnosticFileName()
                            logger.log(
                                category = "UI_ACTION",
                                event =
                                    "EXPORT_DIAGNOSTICS_PRESSED",
                                mediaPositionMs =
                                    player.currentPosition,
                                details = mapOf(
                                    "suggestedFileName" to
                                        fileName
                                )
                            )
                            exportLauncher.launch(fileName)
                        }
                    ) {
                        Text("Export Diagnostics")
                    }
                }

                if (
                    guidedTest.active ||
                    guidedTest.completed
                ) {
                    GuidedTestCard(
                        controller = guidedTest,
                        onStopFailure = {
                            guidedTest.stopAsFailure(
                                logger,
                                "Tester stopped the guided test because the expected behavior did not occur."
                            )
                        },
                        onExport = {
                            val fileName =
                                diagnosticFileName()
                            logger.log(
                                category = "UI_ACTION",
                                event =
                                    "GUIDED_TEST_EXPORT_PRESSED",
                                mediaPositionMs =
                                    player.currentPosition,
                                details = mapOf(
                                    "suggestedFileName" to
                                        fileName
                                )
                            )
                            exportLauncher.launch(fileName)
                        }
                    )
                }

                if (exportStatus.isNotBlank()) {
                    Text(
                        text = exportStatus,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun GuidedTestCard(
    controller: GuidedTestController,
    onStopFailure: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Baseline Guided Test",
                style = MaterialTheme.typography.titleMedium
            )

            if (controller.completed) {
                Text(
                    text =
                        "Result: ${controller.overallStatus}"
                )

                controller.results.forEachIndexed {
                    index,
                    result ->

                    Text(
                        text =
                            "${index + 1}. ${result.id}: " +
                                "${result.status} — " +
                                result.message,
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }

                HorizontalDivider()

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExport
                ) {
                    Text(
                        "Export Test + Diagnostics ZIP"
                    )
                }
            } else {
                val step = controller.currentStep

                Text(
                    text =
                        "Step ${controller.currentIndex + 1} " +
                            "of ${controller.steps.size}: " +
                            step?.title.orEmpty(),
                    style =
                        MaterialTheme.typography.labelLarge
                )

                Text(
                    text = step?.instruction.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = controller.statusMessage,
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStopFailure
                ) {
                    Text(
                        "Stop — Expected Behavior Failed"
                    )
                }
            }
        }
    }
}

private fun normalizedPosition(
    positionMs: Long,
    durationMs: Long
): Float {
    if (durationMs <= 0L) return 0f

    return (
        positionMs
            .coerceIn(0L, durationMs)
            .toDouble() /
            durationMs.toDouble()
        )
        .toFloat()
        .coerceIn(0f, 1f)
}

private fun playbackStateName(
    state: Int
): String =
    when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN_$state"
    }

private fun formatTime(
    milliseconds: Long
): String {
    val totalSeconds =
        milliseconds.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes =
        (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0L) {
        "%d:%02d:%02d".format(
            hours,
            minutes,
            seconds
        )
    } else {
        "%02d:%02d".format(
            minutes,
            seconds
        )
    }
}

private fun diagnosticFileName(): String {
    val safeTimestamp =
        Instant.now()
            .toString()
            .replace(":", "-")
            .replace(".", "-")

    return "DVR_Player_Diagnostics_v0.1.0_$safeTimestamp.zip"
}
