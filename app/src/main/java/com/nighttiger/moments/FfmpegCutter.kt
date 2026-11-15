package com.nighttiger.moments

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

/**
 * Cuts a selected moment into a vertical 9:16 Short.
 * Original game audio only — NO voiceover, NO captions in v1 (user's order).
 * Video is scaled + padded to 720x1280, so landscape and portrait
 * recordings both come out as proper vertical Shorts.
 * All rendering runs off the UI thread via FFmpegKit.executeAsync.
 */
object FfmpegCutter {

    fun copyUriToFile(context: Context, uri: Uri, name: String): File? {
        return try {
            val out = File(context.filesDir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            if (out.exists() && out.length() > 0) out else null
        } catch (e: Exception) {
            null
        }
    }

    fun cutShort(
        input: File,
        startSec: Double,
        durationSec: Double,
        outFile: File,
        onProgress: (Int) -> Unit,
        onDone: (Boolean, String) -> Unit
    ) {
        try {
            val totalMs = (durationSec * 1000).toLong()
            val filter = "scale=720:1280:force_original_aspect_ratio=decrease," +
                "pad=720:1280:(ow-iw)/2:(oh-ih)/2,format=yuv420p"
            val cmd = "ffmpeg -y -ss $startSec -t $durationSec " +
                "-i \"${input.absolutePath}\" " +
                "-vf \"$filter\" " +
                "-c:v libx264 -preset veryfast -crf 23 " +
                "-c:a aac -b:a 128k -movflags +faststart " +
                "\"${outFile.absolutePath}\""
            FFmpegKit.executeAsync(
                cmd,
                { session ->
                    val ok = ReturnCode.isSuccess(session.returnCode)
                    onDone(
                        ok,
                        if (ok) "" else "Cut fail ho gaya. Dobara koshish karein."
                    )
                },
                { },
                { stats ->
                    if (totalMs > 0) {
                        val p = ((stats.time * 100) / totalMs).toInt().coerceIn(0, 100)
                        onProgress(p)
                    }
                }
            )
        } catch (e: Exception) {
            onDone(false, "Cut start nahi hua: ${e.message}")
        }
    }

    /** Save a finished Short to the gallery: Movies / Night Tiger. */
    fun saveToGallery(context: Context, file: File, displayName: String): Uri? {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Night Tiger")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
            }
            uri
        } catch (e: Exception) {
            null
        }
    }

    /** Share a Short — TikTok (or any app) via the system share sheet. */
    fun shareVideo(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "TikTok par share karein")
        )
    }
}
