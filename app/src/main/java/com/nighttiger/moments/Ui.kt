package com.nighttiger.moments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nighttiger.moments.MomentsViewModel.Companion.formatTime

val Gold = Color(0xFFF5B301)
val DarkBg = Color(0xFF0D0D0F)
val CardBg = Color(0xFF1B1B1F)
val LightText = Color(0xFFF5F5F5)
val MutedText = Color(0xFF9A9AA3)

@Composable
fun NightTigerScreen(
    viewModel: MomentsViewModel,
    onPickVideo: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Branding — Night Tiger, PUBG Mobile in-game name
        Text(
            text = "꧁ Night Tiger ꧂",
            color = Gold,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "PUBG Moments • Best moments se Shorts banayein",
            color = MutedText,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "No voiceover • No captions • Sirf best moments",
            color = MutedText,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Step 1 — pick video
        SectionTitle("1. Video chunein / Pick a video")
        Button(
            onClick = onPickVideo,
            enabled = !viewModel.isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.VideoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Gallery se lambi video chunein", fontWeight = FontWeight.Bold)
        }
        if (viewModel.pickedName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Video: ${viewModel.pickedName} • " +
                    "${String.format("%.1f", viewModel.pickedSizeMb)} MB • " +
                    formatTime(viewModel.durationSec),
                color = LightText,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Step 2 — scan
        SectionTitle("2. Best moments dhoondein / Scan")
        Text(
            text = "App awaaz (goli / excitement) aur scene change se best moments nikalti hai. " +
                "Ye signal hain, insani faisla nahi — is liye aap khud check karke chunte hain.",
            color = MutedText,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.startScan() },
            enabled = !viewModel.isBusy && viewModel.pickedName.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan karein", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Status + progress
        Text(text = viewModel.status, color = LightText, fontSize = 14.sp)
        if (viewModel.isBusy) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Gold)
                Spacer(modifier = Modifier.width(12.dp))
                LinearProgressIndicator(
                    progress = viewModel.progress / 100f,
                    color = Gold,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = "${viewModel.progress}%",
                color = MutedText,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Step 3 — candidates
        if (viewModel.candidates.isNotEmpty()) {
            SectionTitle("3. Moments chunein / Pick moments")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${viewModel.selectedIds.size} / ${viewModel.candidates.size} chune gaye",
                    color = MutedText,
                    fontSize = 13.sp
                )
                Text(
                    text = "Sab chunein",
                    color = Gold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.selectAll() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            for (cand in viewModel.candidates) {
                CandidateRow(
                    candidate = cand,
                    checked = viewModel.selectedIds.contains(cand.id),
                    onToggle = { viewModel.toggleSelect(cand.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.cutSelected(context) },
                enabled = !viewModel.isBusy && viewModel.selectedIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shorts Banao (${viewModel.selectedIds.size})", fontWeight = FontWeight.Bold)
            }
        }

        // Step 4 — finished shorts
        if (viewModel.finishedShorts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("4. Tayyar Shorts / Ready Shorts")
            Text(
                text = "Gallery mein save ho gayi hain: Movies / Night Tiger",
                color = MutedText,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            for (file in viewModel.finishedShorts) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = file.name,
                        color = LightText,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { FfmpegCutter.shareVideo(context, file) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Imaandaar baat: app signal se moments dhoondti hai — kuch ache moments " +
                "chhoot sakte hain, kuch kamzor bhi aa sakte hain. Is liye preview aur aap ki " +
                "pasand zaroori hai. Lambi video scan / cut karne mein waqt lagta hai.",
            color = MutedText,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Gold,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun CandidateRow(
    candidate: MomentCandidate,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(10.dp))
            .border(1.dp, if (checked) Gold else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = Color.Black)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Moment ${candidate.id} • ${formatTime(candidate.startSec)} — " +
                    formatTime(candidate.startSec + candidate.durationSec),
                color = LightText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Loudness score: ${candidate.score}/100 • " +
                    "Peak ${String.format("%.1f", candidate.peakDb)} dB • " +
                    "Scene changes: ${candidate.sceneChanges}",
                color = MutedText,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFF333338), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(candidate.score / 100f)
                        .height(6.dp)
                        .background(Gold, RoundedCornerShape(3.dp))
                )
            }
        }
        if (checked) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Gold)
        }
    }
}
