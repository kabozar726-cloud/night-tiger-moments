package com.nighttiger.moments

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * App state. Every heavy step (copy, scan, cut) runs on Dispatchers.IO —
 * standing rule: no heavy work on the UI thread.
 */
class MomentsViewModel : ViewModel() {

    var pickedName by mutableStateOf("")
        private set
    var pickedSizeMb by mutableStateOf(0.0)
        private set
    var durationSec by mutableStateOf(0.0)
        private set

    var status by mutableStateOf("Video chunein — lambi PUBG recording.")
        private set
    var isBusy by mutableStateOf(false)
        private set
    var progress by mutableStateOf(0)
        private set

    var candidates by mutableStateOf<List<MomentCandidate>>(emptyList())
        private set
    var selectedIds by mutableStateOf<Set<Int>>(emptySet())
        private set

    var finishedShorts by mutableStateOf<List<File>>(emptyList())
        private set

    private var inputFile: File? = null

    fun toggleSelect(id: Int) {
        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
    }

    fun selectAll() {
        selectedIds = candidates.map { it.id }.toSet()
    }

    fun onVideoPicked(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isBusy = true
                progress = 0
                status = "Video copy ho rahi hai... (lambi video mein waqt lag sakta hai)"
                candidates = emptyList()
                selectedIds = emptySet()
                finishedShorts = emptyList()
            }
            val file = FfmpegCutter.copyUriToFile(context, uri, "input.mp4")
            withContext(Dispatchers.Main) {
                if (file == null) {
                    isBusy = false
                    status = "Video copy nahi hui. Dobara chunein."
                    return@withContext
                }
                inputFile = file
                pickedName = uri.lastPathSegment ?: "video.mp4"
                pickedSizeMb = file.length() / 1024.0 / 1024.0
                durationSec = MomentScanner.getDurationSec(file)
                isBusy = false
                status = "Video mil gayi: ${formatTime(durationSec)} lambi. Ab Scan dabayein."
            }
        }
    }

    fun startScan() {
        val file = inputFile ?: return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isBusy = true
                progress = 0
                status = "Scan shuru..."
                candidates = emptyList()
                selectedIds = emptySet()
            }
            val result = MomentScanner.scan(file) { p, note ->
                // Called from the background thread — post to main via state is
                // safe here because Compose state writes are thread-safe snapshots.
                progress = p
                status = note
            }
            withContext(Dispatchers.Main) {
                isBusy = false
                durationSec = result.durationSec
                if (result.candidates.isEmpty()) {
                    status = "Koi loud moment nahi mila. Video mein shayad awaaz nahi hai — phir bhi aap poori video se haath se Short bana sakte hain (agle version mein)."
                } else {
                    candidates = result.candidates
                    selectedIds = result.candidates.map { it.id }.toSet()
                    status = "${result.candidates.size} best moments mile. Jin ko rakhna hai, un par tick karein, phir Shorts Banao dabayein."
                }
            }
        }
    }

    fun cutSelected(context: Context) {
        val file = inputFile ?: return
        val chosen = candidates.filter { selectedIds.contains(it.id) }
        if (chosen.isEmpty()) {
            status = "Pehle kam az kam aik moment chunein."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val outDir = File(context.filesDir, "shorts").apply { mkdirs() }
            val done = mutableListOf<File>()
            for ((index, cand) in chosen.withIndex()) {
                withContext(Dispatchers.Main) {
                    isBusy = true
                    progress = 0
                    status = "Short ${index + 1}/${chosen.size} ban rahi hai..."
                }
                val outFile = File(outDir, "night-tiger-short-${cand.id}.mp4")
                val ok = cutOne(file, cand, outFile)
                if (ok) {
                    FfmpegCutter.saveToGallery(
                        context, outFile,
                        "NightTiger_Short_${cand.id}.mp4"
                    )
                    done.add(outFile)
                }
            }
            withContext(Dispatchers.Main) {
                isBusy = false
                progress = 100
                finishedShorts = done
                status = if (done.size == chosen.size) {
                    "${done.size} Shorts tayyar hain — Gallery (Movies / Night Tiger) mein save ho gayi hain. Share dabakar TikTok par bhejein."
                } else {
                    "${done.size}/${chosen.size} Shorts bani. Jo nahi bani, un ko dobara koshish karein."
                }
            }
        }
    }

    /** Suspends until one ffmpeg cut finishes. */
    private suspend fun cutOne(input: File, cand: MomentCandidate, out: File): Boolean {
        return withContext(Dispatchers.IO) {
            var result: Boolean? = null
            FfmpegCutter.cutShort(
                input, cand.startSec, cand.durationSec, out,
                onProgress = { p -> progress = p },
                onDone = { ok, _ -> result = ok }
            )
            // Wait for the async ffmpeg callback (polling is cheap here;
            // the work itself is async inside ffmpeg-kit).
            var waited = 0
            while (result == null && waited < 600_000) {
                Thread.sleep(200)
                waited += 200
            }
            result == true
        }
    }

    companion object {
        fun formatTime(totalSec: Double): String {
            val s = totalSec.toInt()
            val m = s / 60
            val r = s % 60
            return if (m > 0) "$m min $r sec" else "$r sec"
        }
    }
}
