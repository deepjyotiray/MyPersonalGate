package com.ananta.towers.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.ananta.towers.data.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailsScreen(
    vehicle: Vehicle?,
    searchedNumber: String?,
    isNewlyRegistered: Boolean = false,
    entryHistory: List<com.ananta.towers.data.VisitorEntry> = emptyList(),
    onLoadHistory: (() -> Unit)? = null,
    onRegisterEntry: () -> Unit,
    onContinueAsVisitor: () -> Unit,
    onRegisterNewVehicle: () -> Unit,
    onEditVehicle: () -> Unit,
    onDeleteVehicle: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(vehicle?.vehicleNumber) {
        if (vehicle != null) onLoadHistory?.invoke()
    }

    if (showDeleteDialog && vehicle != null && onDeleteVehicle != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Vehicle") },
            text = { Text("Remove ${vehicle.vehicleNumber} from registered vehicles?") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; onDeleteVehicle() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vehicle Details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (vehicle != null) {
                        if (onDeleteVehicle != null) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Vehicle", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        IconButton(onClick = onEditVehicle) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Vehicle")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isNewlyRegistered) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, "Success", tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Vehicle Registered Successfully!",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (vehicle != null) {
                if (!isNewlyRegistered) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(64.dp))
                    Text("Registered Vehicle", style = MaterialTheme.typography.headlineSmall)
                }
                
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow("Vehicle No", vehicle.vehicleNumber)
                        DetailRow("Owner", vehicle.ownerName)
                        DetailRow("Type", if (vehicle.isTenant) "Tenant" else "Owner")
                        DetailRow("Flat", "${vehicle.building} - ${vehicle.flatNumber}")
                        DetailRow("Sticker No", vehicle.societyStickerNumber ?: "N/A")
                        DetailRow("Tag Number", vehicle.tagNumber ?: "N/A")
                    }
                }
                
                vehicle.vehiclePhotoUrl?.let {
                    AsyncImage(model = it, contentDescription = "Vehicle Photo", modifier = Modifier.height(200.dp).fillMaxWidth())
                }

                if (entryHistory.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Visit History (${entryHistory.size} total)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            val lastEntry = entryHistory.first()
                            DetailRow("Last Visit", lastEntry.entryTime.take(16).replace("T", " "))
                            DetailRow("Last Visitor", lastEntry.visitor?.visitorName ?: "Unknown")
                            HorizontalDivider()
                            entryHistory.take(5).forEach { entry ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(entry.visitor?.visitorName ?: "Unknown", style = MaterialTheme.typography.bodySmall)
                                    Text(entry.entryTime.take(10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }

                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                var downloading by remember { mutableStateOf(false) }

                fun shareDocument(relativeUrl: String, label: String) {
                    scope.launch {
                        downloading = true
                        withContext(Dispatchers.IO) {
                            try {
                                val fullUrl = "https://ananta.healthymealspot.com$relativeUrl"
                                val ext = relativeUrl.substringAfterLast('.', "jpg")
                                val dir = File(context.cacheDir, "documents").also { it.mkdirs() }
                                val file = File(dir, "${label.replace(" ", "_")}_${System.currentTimeMillis()}.$ext")
                                URL(fullUrl).openStream().use { input -> file.outputStream().use { input.copyTo(it) } }
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val mime = context.contentResolver.getType(uri) ?: "application/*"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = mime
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share $label"))
                            } catch (_: Exception) {}
                        }
                        downloading = false
                    }
                }

                if (vehicle.rcBookUrl != null || vehicle.rentAgreementUrl != null) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Documents", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            vehicle.rcBookUrl?.let { url ->
                                OutlinedButton(
                                    onClick = { shareDocument(url, "RC Book") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !downloading
                                ) {
                                    Icon(Icons.Default.Share, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (downloading) "Downloading..." else "Share RC Book")
                                }
                            }
                            vehicle.rentAgreementUrl?.let { url ->
                                OutlinedButton(
                                    onClick = { shareDocument(url, "Rent Agreement") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !downloading
                                ) {
                                    Icon(Icons.Default.Share, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (downloading) "Downloading..." else "Share Rent Agreement")
                                }
                            }
                        }
                    }
                }

                Button(onClick = onRegisterEntry, modifier = Modifier.fillMaxWidth()) {
                    Text("Register Entry")
                }
                
                OutlinedButton(onClick = onEditVehicle, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Vehicle Details")
                }

                Text(
                    "This is a registered vehicle. Entry will be logged as Resident.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(64.dp))
                Text("Vehicle Not Registered", style = MaterialTheme.typography.headlineSmall, color = Color.Red)
                
                if (searchedNumber != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Searched Number", style = MaterialTheme.typography.labelMedium)
                            Text(searchedNumber, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                Button(
                    onClick = onRegisterNewVehicle,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.AppRegistration, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Register as Resident Vehicle")
                }
                
                OutlinedButton(onClick = onContinueAsVisitor, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue as Visitor Entry")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
