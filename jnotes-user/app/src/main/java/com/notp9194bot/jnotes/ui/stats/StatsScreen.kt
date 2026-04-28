package com.notp9194bot.jnotes.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val vm: StatsViewModel = viewModel(factory = StatsViewModel.Factory)
    val s by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Active", s.activeNotes.toString(), Modifier.weight(1f))
                StatCard("Pinned", s.pinned.toString(), Modifier.weight(1f))
                StatCard("Streak", "${s.streakDays}d", Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Archive", s.archived.toString(), Modifier.weight(1f))
                StatCard("Trash", s.trashed.toString(), Modifier.weight(1f))
                StatCard("Tags", s.tagsUsed.toString(), Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Words", s.totalWords.toString(), Modifier.weight(1f))
                StatCard("Chars", s.totalChars.toString(), Modifier.weight(1f))
                StatCard("Checklists", s.checklists.toString(), Modifier.weight(1f))
            }

            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Activity (last 14 days)", style = MaterialTheme.typography.titleMedium)
                    Sparkline(s.sparkline, modifier = Modifier.fillMaxWidth().height(120.dp))
                }
            }

            if (s.checklistItemsTotal > 0) {
                val pct = s.checklistItemsDone.toFloat() / s.checklistItemsTotal
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Checklist completion", style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth())
                        Text(
                            "${s.checklistItemsDone} of ${s.checklistItemsTotal} done (${(pct * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            if (s.topTags.isNotEmpty()) {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Top tags", style = MaterialTheme.typography.titleMedium)
                        for ((tag, count) in s.topTags) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("#$tag", modifier = Modifier.weight(1f))
                                Text("$count")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun Sparkline(values: List<Int>, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (values.isEmpty()) return@Canvas
            val max = (values.max().coerceAtLeast(1)).toFloat()
            val w = size.width
            val h = size.height
            val stepX = w / (values.size - 1).coerceAtLeast(1)

            // bars
            val barW = (stepX * 0.55f).coerceAtLeast(2f)
            values.forEachIndexed { i, v ->
                val x = i * stepX - barW / 2
                val barH = (v / max) * (h - 12f)
                drawRect(
                    color = accent.copy(alpha = 0.18f),
                    topLeft = Offset(x.coerceAtLeast(0f), h - barH),
                    size = Size(barW.coerceAtMost(w), barH),
                )
            }

            // line
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = i * stepX
                val y = h - (v / max) * (h - 12f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = accent, style = Stroke(width = 4f))
        }
    }
}
