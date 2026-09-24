package com.zaksecurity.dvrplayer.diagnostics

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns

object MediaInfoCollector {
    fun collect(context: Context, uri: Uri): Map<String, String> {
        val result = linkedMapOf<String, String>()
        result["uriScheme"] = uri.scheme.orEmpty()
        result["uriAuthority"] = uri.authority.orEmpty()
        result["contentType"] = context.contentResolver.getType(uri).orEmpty()

        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) {
                    result["displayName"] = cursor.getString(nameIndex).orEmpty()
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    result["sizeBytes"] = cursor.getLong(sizeIndex).toString()
                }
            }
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            addMetadata(retriever, result, "durationMs", MediaMetadataRetriever.METADATA_KEY_DURATION)
            addMetadata(retriever, result, "width", MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            addMetadata(retriever, result, "height", MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            addMetadata(retriever, result, "rotationDegrees", MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            addMetadata(retriever, result, "captureFrameRate", MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            addMetadata(retriever, result, "bitrate", MediaMetadataRetriever.METADATA_KEY_BITRATE)
            addMetadata(retriever, result, "metadataMimeType", MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            addMetadata(retriever, result, "hasAudio", MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            addMetadata(retriever, result, "hasVideo", MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
        } finally {
            retriever.release()
        }

        return result
    }

    private fun addMetadata(
        retriever: MediaMetadataRetriever,
        result: MutableMap<String, String>,
        name: String,
        key: Int
    ) {
        retriever.extractMetadata(key)?.let { result[name] = it }
    }

    fun asText(info: Map<String, String>): String =
        if (info.isEmpty()) {
            "No media selected.\n"
        } else {
            info.entries.joinToString("\n") { (key, value) -> "$key=$value" } + "\n"
        }
}
