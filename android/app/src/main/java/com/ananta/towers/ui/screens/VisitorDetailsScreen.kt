package com.ananta.towers.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ananta.towers.data.VisitorEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorDetailsScreen(
    entry: VisitorEntry?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visitor Details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (entry == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Entry not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Visitor Information", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        DetailRow("Name", entry.visitor?.visitorName ?: "Unknown")
                        DetailRow("Mobile", entry.visitor?.mobileNumber ?: "N/A")
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Entry Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        DetailRow("Tower", entry.building)
                        DetailRow("Flat", entry.flatNumber)
                        DetailRow("Vehicle", entry.vehicleNumber ?: "None")
                        DetailRow("In Time", entry.entryTime.replace("T", " "))
                        entry.exitTime?.let { DetailRow("Out Time", it.replace("T", " ")) }
                        DetailRow("Status", entry.status.name)
                    }
                }

                if (!entry.notes.isNullOrBlank()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Notes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(entry.notes, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                val photoUrl = entry.visitor?.photoUrl
                val idUrl = entry.visitor?.idPhotoUrl

                // Visitor Photo Section
                Text("Visitor Photo", style = MaterialTheme.typography.titleMedium)
                Card(Modifier.fillMaxWidth()) {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Visitor Photo",
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                            Text("Visitor photo not available", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Visitor ID Section
                Text("Visitor ID", style = MaterialTheme.typography.titleMedium)
                Card(Modifier.fillMaxWidth()) {
                    if (!idUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = idUrl,
                            contentDescription = "Visitor ID",
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                            Text("ID photo not available", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}


