package com.ananta.towers.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorEntryForm(
    loading: Boolean,
    error: String?,
    prefilledVehicle: String?,
    cachedVisitor: com.ananta.towers.data.CachedVisitor? = null,
    onSubmit: (name: String, mobile: String, building: String, flat: String, vehicle: String?, notes: String?, idType: String, idPhoto: Uri?, photo: Uri) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(cachedVisitor?.name ?: "") }
    var mobile by remember { mutableStateOf(cachedVisitor?.mobile ?: "") }
    var building by remember { mutableStateOf(cachedVisitor?.building ?: "") }
    var flat by remember { mutableStateOf(cachedVisitor?.flat ?: "") }
    var vehicle by remember { mutableStateOf(prefilledVehicle ?: "") }
    var notes by remember { mutableStateOf("") }
    
    var idType by remember { mutableStateOf("Aadhaar Card") }
    var idTypeExpanded by remember { mutableStateOf(false) }
    val idTypes = listOf("Aadhaar Card", "Driving License", "PAN Card", "Voter ID", "Company ID")
    
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var idPhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var isCapturingId by remember { mutableStateOf(false) }

    var buildingExpanded by remember { mutableStateOf(false) }
    var flatExpanded by remember { mutableStateOf(false) }

    val towers = (1..21).map { it.toString() }
    val flats = (1..13).flatMap { floor ->
        (1..4).map { flatNum -> "${floor}${flatNum.toString().padStart(2, '0')}" }
    }

    val occupantNumber = remember(building, flat) {
        if (building.isNotBlank() && flat.isNotBlank()) {
            "+91 98765${building.padStart(2, '0')}${flat.padStart(3, '0')}"
        } else null
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            if (isCapturingId) idPhotoUri = tempUri
            else photoUri = tempUri
        }
    }

    val idUploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) idPhotoUri = uri
    }

    fun launchCamera(forId: Boolean) {
        isCapturingId = forId
        val file = File(context.cacheDir, "photos").also { it.mkdirs() }.let { 
            File(it, "${if(forId) "id" else "visitor"}_${System.currentTimeMillis()}.jpg") 
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        tempUri = uri
        cameraLauncher.launch(uri)
    }

    fun makeCall(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Visitor Entry") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Visitor Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = buildingExpanded,
                    onExpandedChange = { buildingExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = building,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tower") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = buildingExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = buildingExpanded,
                        onDismissRequest = { buildingExpanded = false }
                    ) {
                        towers.forEach { tower ->
                            DropdownMenuItem(text = { Text(tower) }, onClick = { building = tower; buildingExpanded = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = flatExpanded,
                    onExpandedChange = { flatExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = flat,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Flat No") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = flatExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = flatExpanded,
                        onDismissRequest = { flatExpanded = false }
                    ) {
                        flats.forEach { flatNo ->
                            DropdownMenuItem(text = { Text(flatNo) }, onClick = { flat = flatNo; flatExpanded = false })
                        }
                    }
                }
            }

            AnimatedVisibility(visible = occupantNumber != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Occupant Number", style = MaterialTheme.typography.labelMedium)
                            Text(occupantNumber ?: "", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { occupantNumber?.let { makeCall(it) } }, contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp)); Text("Call")
                        }
                    }
                }
            }

            // ID Details Section
            Text("ID Proof Details", style = MaterialTheme.typography.titleMedium)
            ExposedDropdownMenuBox(
                expanded = idTypeExpanded,
                onExpandedChange = { idTypeExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = idType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select ID Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = idTypeExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = idTypeExpanded,
                    onDismissRequest = { idTypeExpanded = false }
                ) {
                    idTypes.forEach { type ->
                        DropdownMenuItem(text = { Text(type) }, onClick = { idType = type; idTypeExpanded = false })
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { launchCamera(true) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(4.dp)); Text("Capture ID")
                }
                OutlinedButton(onClick = { idUploadLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.FileUpload, null)
                    Spacer(Modifier.width(4.dp)); Text("Upload ID")
                }
            }
            
            if (idPhotoUri != null) {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreditCard, null, tint = Color(0xFF4CAF50))
                        Spacer(Modifier.width(8.dp))
                        Text("ID Photo Attached", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                }
            }

            OutlinedTextField(value = vehicle, onValueChange = { vehicle = it }, label = { Text("Vehicle Number (Optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            Text("Visitor Photo (Required)", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { launchCamera(false) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text(if (photoUri != null) "Change Photo" else "Capture Photo")
            }

            photoUri?.let {
                Image(painter = rememberAsyncImagePainter(it), contentDescription = null, modifier = Modifier.fillMaxWidth().height(200.dp), contentScale = ContentScale.Crop)
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = { photoUri?.let { onSubmit(name, mobile, building, flat, vehicle.ifBlank { null }, notes.ifBlank { null }, idType, idPhotoUri, it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && mobile.isNotBlank() && building.isNotBlank() && flat.isNotBlank() && photoUri != null && !loading
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(24.dp))
                else Text("Create Entry")
            }
        }
    }
}
