package com.ananta.towers.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ananta.towers.data.VisitorEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    activeEntries: List<VisitorEntry>,
    onMenuClick: () -> Unit,
    onScanClick: () -> Unit,
    onManualLookup: (String) -> Unit,
    onEntryClick: (VisitorEntry) -> Unit,
    onExit: (VisitorEntry) -> Unit,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) { onRefresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ananta Towers") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menu") }
                }
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onScanClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan Vehicle",
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Enter Vehicle Number") },
                trailingIcon = {
                    IconButton(onClick = { onManualLookup(searchQuery) }) {
                        Icon(Icons.Default.Search, null)
                    }
                },
                singleLine = true
            )

            Text(
                "Active Visitors (${activeEntries.size})",
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
                        ActiveEntryCard(
                            entry = entry, 
                            onClick = { onEntryClick(entry) },
                            onExit = { onExit(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveEntryCard(entry: VisitorEntry, onClick: () -> Unit, onExit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.visitor?.visitorName ?: "Unknown", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("Flat ${entry.flatNumber}", style = MaterialTheme.typography.labelLarge)
            }
            Text("Vehicle: ${entry.vehicleNumber ?: "None"}", style = MaterialTheme.typography.bodySmall)
            Text("In: ${entry.entryTime.take(16).replace("T", " ")}", style = MaterialTheme.typography.bodySmall)
            
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onExit, 
                modifier = Modifier.fillMaxWidth(), 
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Mark Exit")
            }
        }
    }
}
