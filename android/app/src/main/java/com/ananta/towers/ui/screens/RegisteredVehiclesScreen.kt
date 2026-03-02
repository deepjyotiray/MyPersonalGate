package com.ananta.towers.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ananta.towers.data.Vehicle

private val TOWER_OPTIONS = listOf("All") + (1..21).map { it.toString() }
private val FLAT_OPTIONS = listOf("All") + (1..13).flatMap { floor -> (1..4).map { unit -> "${floor}0${unit}" } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisteredVehiclesScreen(
    vehicles: List<Vehicle>,
    onMenuClick: () -> Unit,
    onRefresh: () -> Unit,
    onVehicleClick: (Vehicle) -> Unit,
    onAddVehicle: (() -> Unit)? = null
) {
    LaunchedEffect(Unit) { onRefresh() }

    var query by remember { mutableStateOf("") }
    var towerFilter by remember { mutableStateOf("All") }
    var flatFilter by remember { mutableStateOf("All") }

    val filtered = vehicles.filter { v ->
        (query.isBlank() || v.vehicleNumber.contains(query, ignoreCase = true) || v.ownerName.contains(query, ignoreCase = true)) &&
        (towerFilter == "All" || v.building == towerFilter) &&
        (flatFilter == "All" || v.flatNumber == flatFilter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registered Vehicles (${filtered.size})") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menu") }
                }
            )
        },
        floatingActionButton = {
            onAddVehicle?.let {
                FloatingActionButton(onClick = it) {
                    Icon(Icons.Default.Add, "Add Vehicle")
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name or vehicle number") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDropdown("Tower", towerFilter, TOWER_OPTIONS, Modifier.weight(1f)) { towerFilter = it }
                FilterDropdown("Flat", flatFilter, FLAT_OPTIONS, Modifier.weight(1f)) { flatFilter = it }
            }
            Spacer(Modifier.height(4.dp))
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No vehicles found", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { vehicle ->
                        VehicleCard(vehicle, onClick = { onVehicleClick(vehicle) })
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleCard(vehicle: Vehicle, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(40.dp).padding(end = 12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(Modifier.weight(1f)) {
                Text(vehicle.vehicleNumber, style = MaterialTheme.typography.titleMedium)
                Text(vehicle.ownerName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${vehicle.building} - Flat ${vehicle.flatNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(vehicle.vehicleType, style = MaterialTheme.typography.bodySmall)
            }
            if (!vehicle.isActive) {
                Badge(containerColor = MaterialTheme.colorScheme.error) { Text("Inactive") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(label: String, value: String, options: List<String>, modifier: Modifier, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
