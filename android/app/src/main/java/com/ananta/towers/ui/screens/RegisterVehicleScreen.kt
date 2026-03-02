package com.ananta.towers.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

private val TOWERS = (1..21).map { it.toString() }
private val FLATS = (1..13).flatMap { floor -> (1..4).map { unit -> "${floor}0${unit}" } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterVehicleScreen(
    prefilledNumber: String?,
    loading: Boolean,
    error: String?,
    existingVehicle: com.ananta.towers.data.Vehicle? = null,
    onSubmit: (String, String, String, String, String, String, String?, Boolean, Uri?, Uri?) -> Unit,
    onBack: () -> Unit
) {
    var vehicleNumber by remember { mutableStateOf(existingVehicle?.vehicleNumber ?: prefilledNumber ?: "") }
    var ownerName by remember { mutableStateOf(existingVehicle?.ownerName ?: "") }
    var mobileNumber by remember { mutableStateOf(existingVehicle?.mobileNumber ?: "") }
    var building by remember { mutableStateOf(existingVehicle?.building ?: "") }
    var flatNumber by remember { mutableStateOf(existingVehicle?.flatNumber ?: "") }
    var vehicleType by remember { mutableStateOf(existingVehicle?.vehicleType ?: "4 Wheeler") }
    var tagNumber by remember { mutableStateOf(existingVehicle?.tagNumber ?: "") }
    var isTenant by remember { mutableStateOf(existingVehicle?.isTenant ?: false) }
    
    val context = LocalContext.current
    var rcBookUri by remember { mutableStateOf<Uri?>(null) }
    var rentAgreementUri by remember { mutableStateOf<Uri?>(null) }
    var rcCameraUri by remember { mutableStateOf<Uri?>(null) }
    var rentCameraUri by remember { mutableStateOf<Uri?>(null) }

    val rcLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) rcBookUri = uri }
    val rentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) rentAgreementUri = uri }
    val rcCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> if (success) rcBookUri = rcCameraUri }
    val rentCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> if (success) rentAgreementUri = rentCameraUri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingVehicle != null) "Edit Vehicle" else "Register New Vehicle") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(visible = error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            OutlinedTextField(
                value = vehicleNumber,
                onValueChange = { vehicleNumber = it.uppercase() },
                label = { Text("Vehicle Number") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            )
            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("Owner Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            )
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { mobileNumber = it },
                label = { Text("Mobile Number") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField("Tower", building, TOWERS, Modifier.weight(1f), !loading) { building = it }
                DropdownField("Flat", flatNumber, FLATS, Modifier.weight(1f), !loading) { flatNumber = it }
            }
            
            Text("Vehicle Type", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RadioButton(selected = vehicleType == "4 Wheeler", onClick = { vehicleType = "4 Wheeler" }, enabled = !loading)
                Text("4 Wheeler", modifier = Modifier.padding(top = 12.dp))
                RadioButton(selected = vehicleType == "2 Wheeler", onClick = { vehicleType = "2 Wheeler" }, enabled = !loading)
                Text("2 Wheeler", modifier = Modifier.padding(top = 12.dp))
            }

            OutlinedTextField(
                value = tagNumber,
                onValueChange = { tagNumber = it },
                label = { Text("Tag/Sticker Number (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isTenant, onCheckedChange = { isTenant = it }, enabled = !loading)
                Text("Is Tenant?")
            }

            if (rcBookUri != null) {
                Text("RC Book Selected ✓", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { rcLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    enabled = !loading
                ) {
                    Icon(Icons.Default.FileUpload, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("RC Book")
                }
                OutlinedButton(
                    onClick = {
                        val uri = createTempImageUri(context, "rc")
                        rcCameraUri = uri
                        rcCameraLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !loading
                ) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Take Photo")
                }
            }

            if (isTenant) {
                if (rentAgreementUri != null) {
                    Text("Rent Agreement Selected ✓", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { rentLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        enabled = !loading
                    ) {
                        Icon(Icons.Default.FileUpload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Rent Agreement")
                    }
                    OutlinedButton(
                        onClick = {
                            val uri = createTempImageUri(context, "rent")
                            rentCameraUri = uri
                            rentCameraLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !loading
                    ) {
                        Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Take Photo")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    onSubmit(vehicleNumber, ownerName, mobileNumber, building, flatNumber, vehicleType, if(tagNumber.isEmpty()) null else tagNumber, isTenant, rcBookUri, rentAgreementUri)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading && vehicleNumber.isNotEmpty() && ownerName.isNotEmpty() && mobileNumber.isNotEmpty()
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (existingVehicle != null) "Update Vehicle" else "Register Vehicle")
                }
            }
        }
    }
}

private fun createTempImageUri(context: Context, prefix: String): Uri {
    val file = File(context.cacheDir.resolve("photos").also { it.mkdirs() }, "${prefix}_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            enabled = enabled
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}
