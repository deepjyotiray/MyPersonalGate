package com.ananta.towers.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ananta.towers.data.AppUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(
    users: List<AppUser>,
    currentRole: String?,
    onMenuClick: () -> Unit,
    onRefresh: () -> Unit,
    onCreateUser: (username: String, pin: String, role: String, name: String, onError: (String) -> Unit) -> Unit,
    onDeleteUser: (String) -> Unit
) {
    LaunchedEffect(Unit) { onRefresh() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    val tabs = listOf("All", "Admin", "Guard")

    val filtered = users.filter { u ->
        if (u.role == "superadmin") return@filter false
        when (selectedTab) {
            1 -> u.role == "admin"
            2 -> u.role == "guard"
            else -> true
        }
    }

    if (showDialog) {
        AddUserDialog(
            isSuperadmin = currentRole == "superadmin",
            onDismiss = { showDialog = false },
            onConfirm = { username, pin, role, name, onError ->
                onCreateUser(username, pin, role, name, onError)
                showDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Users") },
                navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menu") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, "Add User")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) })
                }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { user ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(user.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "@${user.username} · ${user.role}${if (user.isActive == 0) " · inactive" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            IconButton(onClick = { onDeleteUser(user.id.toString()) }) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddUserDialog(
    isSuperadmin: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, (String) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("guard") }
    var error by remember { mutableStateOf<String?>(null) }

    val roleOptions = if (isSuperadmin) listOf("guard", "admin") else listOf("guard")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add User") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("PIN") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (isSuperadmin) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Role:")
                        roleOptions.forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = role == r, onClick = { role = r })
                                Text(r.replaceFirstChar { it.uppercase() })
                            }
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(username, pin, role, name) { error = it } },
                enabled = name.isNotBlank() && username.isNotBlank() && pin.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
