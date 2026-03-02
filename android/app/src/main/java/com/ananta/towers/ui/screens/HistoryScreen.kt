package com.ananta.towers.ui.screens

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
fun HistoryScreen(
    entries: List<VisitorEntry>,
    onMenuClick: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { onRefresh() }

    var towerFilter by remember { mutableStateOf("All") }
    var flatFilter by remember { mutableStateOf("All") }
    var vehicleFilter by remember { mutableStateOf("All") }

    val towers = listOf("All") + entries.mapNotNull { it.building }.filter { it.isNotBlank() }.distinct().sorted()
    val flats = listOf("All") + entries.mapNotNull { it.flatNumber }.filter { it.isNotBlank() }.distinct().sorted()
    val vehicles = listOf("All") + entries.mapNotNull { it.vehicleNumber }.filter { it.isNotBlank() }.distinct().sorted()

    val filtered = entries.filter { e ->
        (towerFilter == "All" || e.building == towerFilter) &&
        (flatFilter == "All" || e.flatNumber == flatFilter) &&
        (vehicleFilter == "All" || e.vehicleNumber == vehicleFilter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visitor History (${filtered.size})") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menu") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilterDropdown("Tower", towerFilter, towers, Modifier.weight(1f)) { towerFilter = it }
                HistoryFilterDropdown("Flat", flatFilter, flats, Modifier.weight(1f)) { flatFilter = it }
                HistoryFilterDropdown("Vehicle", vehicleFilter, vehicles, Modifier.weight(1f)) { vehicleFilter = it }
            }
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No entries found", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { entry ->
                        HistoryEntryCard(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(entry: VisitorEntry) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.visitor?.visitorName ?: entry.name ?: "Unknown", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (entry.status?.name == "INSIDE") "Inside" else "Exited",
                    color = if (entry.status?.name == "INSIDE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Text("Tower ${entry.building} - Flat ${entry.flatNumber}", style = MaterialTheme.typography.bodySmall)
            entry.vehicleNumber?.let { Text("Vehicle: $it", style = MaterialTheme.typography.bodySmall) }
            Text("In: ${entry.entryTime.take(16).replace("T", " ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            entry.exitTime?.let { Text("Out: ${it.take(16).replace("T", " ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryFilterDropdown(label: String, value: String, options: List<String>, modifier: Modifier, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
