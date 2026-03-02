package com.ananta.towers.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntryScreen(
    loading: Boolean,
    onMenuClick: () -> Unit,
    onSubmit: (type: String, name: String, flat: String, host: String?, vehicle: String?, sticker: Boolean, purpose: String?, idProof: String?, photo: Uri?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("visitor") }
    var name by remember { mutableStateOf("") }
    var flat by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var vehicle by remember { mutableStateOf("") }
    var sticker by remember { mutableStateOf(false) }
    var purpose by remember { mutableStateOf("") }
    var idProof by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) photoUri = tempUri
    }

    fun launchCamera() {
        val file = File(context.cacheDir, "photos").also { it.mkdirs() }.let { File(it, "photo_${System.currentTimeMillis()}.jpg") }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        tempUri = uri
        cameraLauncher.launch(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Entry") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Type selector
            Text("Entry Type", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("visitor", "delivery", "vehicle").forEach { t ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.replaceFirstChar { it.uppercase() }) })
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(if (type == "delivery") "Company Name" else "Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = flat, onValueChange = { flat = it }, label = { Text("Flat / Unit Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            if (type != "vehicle") {
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            if (type == "visitor") {
                OutlinedTextField(value = purpose, onValueChange = { purpose = it }, label = { Text("Purpose of Visit") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = idProof, onValueChange = { idProof = it }, label = { Text("ID Proof (type & number)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            if (type == "vehicle" || type == "visitor") {
                OutlinedTextField(value = vehicle, onValueChange = { vehicle = it }, label = { Text("Vehicle Number (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            if (type == "vehicle") {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = sticker, onCheckedChange = { sticker = it })
                    Text("Has Parking Sticker")
                }
            }

            // Photo capture
            Text("Photo", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { launchCamera() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text(if (photoUri != null) "Retake Photo" else "Capture Photo")
            }
            photoUri?.let {
                Image(painter = rememberAsyncImagePainter(it), contentDescription = "Captured", modifier = Modifier.fillMaxWidth().height(200.dp), contentScale = ContentScale.Crop)
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onSubmit(type, name, flat, host.ifBlank { null }, vehicle.ifBlank { null }, sticker, purpose.ifBlank { null }, idProof.ifBlank { null }, photoUri)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && flat.isNotBlank() && !loading
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Submit Entry")
            }
        }
    }
}
