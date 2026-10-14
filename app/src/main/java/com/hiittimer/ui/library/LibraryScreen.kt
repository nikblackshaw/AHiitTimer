package com.hiittimer.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hiittimer.domain.TimeFormat
import com.hiittimer.domain.Workout

/** Browse and choose between the built-in presets and saved workouts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    presets: List<Workout>,
    custom: List<Workout>,
    selectedId: String,
    onSelect: (Workout) -> Unit,
    onNew: () -> Unit,
    onEdit: (Workout) -> Unit,
    onDelete: (Workout) -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workouts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Text("Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    IconButton(onClick = onNew) {
                        Icon(Icons.Default.Add, contentDescription = "New workout")
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item {
                Text("Presets", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp))
            }
            items(presets, key = { it.id }) { w ->
                WorkoutRow(w, w.id == selectedId, onSelect, null, null)
            }
            item {
                Text("My Workouts", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            }
            if (custom.isEmpty()) {
                item {
                    Text("No saved workouts yet — tap + to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp))
                }
            } else {
                items(custom, key = { it.id }) { w ->
                    WorkoutRow(w, w.id == selectedId, onSelect, onEdit, onDelete)
                }
            }
        }
    }
}

@Composable
private fun WorkoutRow(
    workout: Workout,
    isSelected: Boolean,
    onSelect: (Workout) -> Unit,
    onEdit: ((Workout) -> Unit)?,
    onDelete: ((Workout) -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onSelect(workout) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(workout.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(workout.detailedSummary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Total ${TimeFormat.duration(workout.totalDuration.toDouble())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary)
            }
            if (onEdit != null) {
                IconButton(onClick = { onEdit(workout) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${workout.name}")
                }
            }
            if (onDelete != null) {
                IconButton(onClick = { onDelete(workout) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete ${workout.name}",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}
