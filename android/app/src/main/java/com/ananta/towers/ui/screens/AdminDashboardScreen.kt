package com.ananta.towers.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ananta.towers.data.VisitorEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    activeEntries: List<VisitorEntry>,
    onMenuClick: () -> Unit,
    onEntryClick: (VisitorEntry) -> Unit,
    onRefresh: () -> Unit
) {
    LaunchedEffect(Unit) { onRefresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guards on Duty") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menu") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Active Entries (${activeEntries.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (activeEntries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active entries", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeEntries, key = { it.id }) { entry ->
                        AdminEntryCard(entry = entry, onClick = { onEntryClick(entry) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminEntryCard(entry: VisitorEntry, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.visitor?.visitorName ?: "Unknown", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Inside", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Text("Tower ${entry.building} - Flat ${entry.flatNumber}", style = MaterialTheme.typography.bodySmall)
            entry.vehicleNumber?.let { Text("Vehicle: $it", style = MaterialTheme.typography.bodySmall) }
            Text("In: ${entry.entryTime.take(16).replace("T", " ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
