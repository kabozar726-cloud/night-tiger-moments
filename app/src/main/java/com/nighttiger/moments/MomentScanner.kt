package com.nighttiger.moments

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Best-moment scanner — on-device, signal-based, NO human judgment.
 *
 * Primary signal: loud audio energy (gunfire / excitement) in 5-second
 * windows, measured with ffmpeg volumedetect (mean_volume in dB).
 * Secondary signal: scene-change count per window, from one ffmpeg
 * select/showinfo pass (best effort — if it fails, loudness alone decides).
 *
 * HONEST LIMIT (ye app ke rules hain, bug nahi): a loud window is only a
 * *candidate*. A good moment can be quiet (a silent clutch), and a loud
 * window can be unimportant (lobby noise, a plane). That is why the app
 * always shows the candidates for preview and the user picks — the app
 * never cuts without the user's tap.
 *
 * All work here runs on a background thread (called from Dispatchers.IO
 * in the ViewModel). Never call these functions on the UI thread.
 */
data class MomentCandidate(
    val id: Int,
    val startSec: Double,
    val durationSec: Double,
    val peakDb: Double,
    val score: Int, // 0-100, loudness + scene changes
    val sceneChanges: Int
)

data class ScanResult(
    val durationSec: Double,
    val candidates: List<MomentCandidate>
)

object MomentScanner {

    private val meanDbRegex = Regex("mean_volume:\\s*(-?[0-9.]+)")
    private val ptsRegex = Regex("pts_time:([0-9.]+)")

    fun getDurationSec(file: File): Double {
        return try {
            val info = FFprobeKit.getMediaInformation(file.absolutePath)
            info.mediaInformation?.duration?.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    fun hasAudio(file: File): Boolean {
        return try {
            val info = FFprobeKit.getMediaInformation(file.absolutePath)
            info.mediaInformation?.streams?.any { it.type == "audio" } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /** One 5-second window's mean loudness in dB, or null if it could not be read. */
    private fun windowMeanDb(file: File, startSec: Double, windowSec: Double): Double? {
        return try {
            val session = FFmpegKit.execute(
                "-ss $startSec -t $windowSec -i \"${file.absolutePath}\" " +
                    "-af volumedetect -f null -"
            )
            val logs = session.allLogsAsString ?: ""
            meanDbRegex.find(logs)?.groupValues?.get(1)?.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /** Scene-change times (seconds) for the whole file — best effort, may be empty. */
    private fun sceneChangeTimes(file: File, durationSec: Double): List<Double> {
        // A full scene pass on a very long video is slow, so skip it there.
        if (durationSec > 1800.0) return emptyList()
        return try {
            val session = FFmpegKit.execute(
                "-i \"${file.absolutePath}\" " +
                    "-vf \"select='gt(scene,0.4)',showinfo\" -f null -"
            )
            val logs = session.allLogsAsString ?: ""
            ptsRegex.findAll(logs)
                .mapNotNull { it.groupValues.get(1).toDoubleOrNull() }
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Scan the video and return up to 10 candidate moments.
     * onProgress(0..100, note) is called from the background thread.
     */
    fun scan(file: File, onProgress: (Int, String) -> Unit): ScanResult {
        val durationSec = getDurationSec(file)
        if (durationSec <= 0.0) return ScanResult(0.0, emptyList())

        // Long videos: wider step so the scan finishes in reasonable time.
        val windowSec = 5.0
        val stepSec = if (durationSec > 1200.0) 10.0 else 5.0
        val windowCount = ((durationSec - windowSec) / stepSec).toInt().coerceAtLeast(1)

        val windows = mutableListOf<Pair<Double, Double>>() // (startSec, meanDb)
        for (i in 0 until windowCount) {
            val start = i * stepSec
            val db = windowMeanDb(file, start, windowSec)
            if (db != null) windows.add(start to db)
            val p = ((i + 1) * 55 / windowCount).coerceIn(0, 55)
            onProgress(p, "Awaaz check ho rahi hai... (${i + 1}/$windowCount)")
        }
        if (windows.isEmpty()) return ScanResult(durationSec, emptyList())

        onProgress(60, "Scene check ho raha hai...")
        val sceneTimes = sceneChangeTimes(file, durationSec)

        // Score each window: loudness (main) + scene changes in that window.
        data class ScoredWindow(val start: Double, val db: Double, val score: Int)
        val scored = windows.map { (start, db) ->
            val scenes = sceneTimes.count { it >= start && it < start + windowSec }
            // mean_volume -45 dB (quiet) -> 0, -5 dB (very loud) -> 100.
            val loudScore = ((db + 45.0) / 40.0 * 100.0).toInt().coerceIn(0, 100)
            val total = (loudScore + scenes * 4).coerceIn(0, 100)
            ScoredWindow(start, db, total)
        }

        // Loud clusters: consecutive windows with score >= 45.
        val loud = scored.filter { it.score >= 45 }
        val pool = if (loud.isNotEmpty()) loud else scored.sortedByDescending { it.score }.take(5)

        // Each cluster becomes one moment: 45 seconds centred on its loudest window,
        // clamped to 30-60 seconds and inside the video.
        val candidates = mutableListOf<MomentCandidate>()
        val sortedPool = pool.sortedByDescending { it.score }
        var id = 1
        for (peak in sortedPool) {
            if (candidates.size >= 10) break
            // Skip if this peak is already covered by an earlier candidate.
            val covered = candidates.any { c ->
                peak.start >= c.startSec - 5.0 && peak.start < c.startSec + c.durationSec + 5.0
            }
            if (covered) continue
            val clipLen = 45.0
            var start = (peak.start + windowSec / 2.0 - clipLen / 2.0)
                .coerceIn(0.0, max(0.0, durationSec - 30.0))
            var len = min(clipLen, durationSec - start)
            if (len < 30.0) {
                len = min(30.0, durationSec)
                start = max(0.0, durationSec - len)
            }
            val scenes = sceneTimes.count { it >= start && it < start + len }
            candidates.add(
                MomentCandidate(
                    id = id++,
                    startSec = start,
                    durationSec = len,
                    peakDb = peak.db,
                    score = peak.score,
                    sceneChanges = scenes
                )
            )
        }
        onProgress(100, "Scan mukammal.")
        return ScanResult(durationSec, candidates.sortedByDescending { it.score })
    }
}
