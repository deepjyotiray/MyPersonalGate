package com.ananta.towers

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.ananta.towers.data.*
import com.ananta.towers.ui.screens.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AnantaApp(viewModel)
            }
        }
    }
}

@Composable
fun AnantaApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val token by viewModel.token.collectAsStateWithLifecycle()
    val role by viewModel.role.collectAsStateWithLifecycle(initialValue = null)
    val userName by viewModel.userName.collectAsStateWithLifecycle(initialValue = null)
    val users by viewModel.users.collectAsStateWithLifecycle()
    val activeEntries by viewModel.activeEntries.collectAsStateWithLifecycle()
    val registeredVehicles by viewModel.registeredVehicles.collectAsStateWithLifecycle()
    val vehicleHistory by viewModel.vehicleHistory.collectAsStateWithLifecycle()
    val cachedVisitor by viewModel.cachedVisitor.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val scannedVehicle by viewModel.scannedVehicle.collectAsStateWithLifecycle()
    val searchedNumber by viewModel.searchedNumber.collectAsStateWithLifecycle()
    val selectedEntry by viewModel.selectedEntry.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isAdmin = role == "admin" || role == "superadmin"

    // Start destination based on role
    val startDest = when {
        token == null -> "login"
        isAdmin -> "admin_dashboard"
        else -> "dashboard"
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) navController.navigate("scan_vehicle")
    }

    fun navigateToDashboard() {
        val dest = if (isAdmin) "admin_dashboard" else "dashboard"
        navController.navigate(dest) { popUpTo(dest) { inclusive = true }; launchSingleTop = true }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = token != null,
        drawerContent = {
            if (token != null) {
                ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.72f)) {
                    Box(Modifier.padding(24.dp)) {
                        Column {
                            Text("Ananta Towers", style = MaterialTheme.typography.headlineSmall)
                            userName?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                            role?.let { r -> Text(r.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) }
                        }
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    val dashRoute = if (isAdmin) "admin_dashboard" else "dashboard"
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Dashboard, null) },
                        label = { Text("Dashboard") },
                        selected = currentDestination?.hierarchy?.any { it.route == dashRoute } == true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigateToDashboard()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    if (!isAdmin) {
                        // Guard-only: Scan Vehicle
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.QrCodeScanner, null) },
                            label = { Text("Scan Vehicle") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    navController.navigate("scan_vehicle")
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    if (isAdmin) {
                        // Admin-only items
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.History, null) },
                            label = { Text("History") },
                            selected = currentDestination?.hierarchy?.any { it.route == "history" } == true,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("history")
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.DirectionsCar, null) },
                            label = { Text("Registered Vehicles") },
                            selected = currentDestination?.hierarchy?.any { it.route == "registered_vehicles" } == true,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("registered_vehicles")
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.People, null) },
                            label = { Text("Manage Users") },
                            selected = currentDestination?.hierarchy?.any { it.route == "manage_users" } == true,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("manage_users")
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    Spacer(Modifier.weight(1f))
                    HorizontalDivider()
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Logout, null) },
                        label = { Text("Logout") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.logout()
                            navController.navigate("login") { popUpTo(0) }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    ) {
        NavHost(navController, startDestination = startDest) {
            composable("login") {
                LoginScreen(
                    onLogin = { username, pin, onError ->
                        viewModel.login(username, pin, onSuccess = { role ->
                            val dest = if (role == "admin" || role == "superadmin") "admin_dashboard" else "dashboard"
                            navController.navigate(dest) { popUpTo("login") { inclusive = true } }
                        }, onError = onError)
                    }
                )
            }

            // ── GUARD FLOW ──────────────────────────────────────────────
            composable("dashboard") {
                DashboardScreen(
                    activeEntries = activeEntries,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onScanClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            navController.navigate("scan_vehicle")
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onManualLookup = { number ->
                        viewModel.lookupVehicle(number) { navController.navigate("vehicle_details/false") }
                    },
                    onEntryClick = { entry ->
                        viewModel.selectEntry(entry)
                        navController.navigate("visitor_details")
                    },
                    onExit = { entry -> viewModel.markExit(entry.id) },
                    onRefresh = { viewModel.loadActive() }
                )
            }
            composable("scan_vehicle") {
                ScannerScreen(
                    onVehicleDetected = { plate ->
                        viewModel.lookupVehicle(plate) {
                            navController.navigate("vehicle_details/false") {
                                popUpTo("scan_vehicle") { inclusive = true }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                "vehicle_details/{isNewlyRegistered}",
                arguments = listOf(navArgument("isNewlyRegistered") { type = NavType.BoolType })
            ) { backStackEntry ->
                val isNewlyRegistered = backStackEntry.arguments?.getBoolean("isNewlyRegistered") ?: false
                VehicleDetailsScreen(
                    vehicle = scannedVehicle,
                    searchedNumber = searchedNumber,
                    isNewlyRegistered = isNewlyRegistered,
                    entryHistory = vehicleHistory,
                    onLoadHistory = { scannedVehicle?.let { viewModel.loadVehicleHistory(it.vehicleNumber) } },
                    onRegisterEntry = {
                        scannedVehicle?.let { viewModel.loadCachedVisitor(it.vehicleNumber) }
                        navController.navigate("visitor_entry_form")
                    },
                    onContinueAsVisitor = { navController.navigate("visitor_entry_form") },
                    onRegisterNewVehicle = { navController.navigate("register_vehicle") },
                    onEditVehicle = { navController.navigate("edit_vehicle") },
                    onBack = {
                        viewModel.clearScannedVehicle()
                        navController.popBackStack()
                    }
                )
            }
            composable("register_vehicle") {
                RegisterVehicleScreen(
                    prefilledNumber = searchedNumber,
                    existingVehicle = null,
                    loading = loading,
                    error = error,
                    onSubmit = { num, name, mob, bld, flat, type, tag, tenant, rc, rent ->
                        viewModel.registerVehicle(num, name, mob, bld, flat, type, tag, tenant, rc, rent) {
                            navController.navigate("vehicle_details/true") {
                                popUpTo("vehicle_details/false") { inclusive = true }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("edit_vehicle") {
                RegisterVehicleScreen(
                    prefilledNumber = scannedVehicle?.vehicleNumber,
                    existingVehicle = scannedVehicle,
                    loading = loading,
                    error = error,
                    onSubmit = { num, name, mob, bld, flat, type, tag, tenant, rc, rent ->
                        scannedVehicle?.let { v ->
                            viewModel.updateVehicle(v.id, num, name, mob, bld, flat, type, tag, tenant, rc, rent) {
                                navController.popBackStack()
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("visitor_entry_form") {
                VisitorEntryForm(
                    loading = loading,
                    error = error,
                    prefilledVehicle = scannedVehicle?.vehicleNumber ?: searchedNumber,
                    cachedVisitor = cachedVisitor,
                    onSubmit = { name, mobile, building, flat, vehicle, notes, idType, idPhoto, photo ->
                        viewModel.createVisitorEntry(name, mobile, building, flat, vehicle, notes, idType, idPhoto, photo) {
                            navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = true } }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("visitor_details") {
                VisitorDetailsScreen(
                    entry = selectedEntry,
                    onBack = { navController.popBackStack() }
                )
            }

            // ── ADMIN FLOW ───────────────────────────────────────────────
            composable("admin_dashboard") {
                AdminDashboardScreen(
                    activeEntries = activeEntries,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onEntryClick = { entry ->
                        viewModel.selectEntry(entry)
                        navController.navigate("visitor_details")
                    },
                    onRefresh = { viewModel.loadActive() }
                )
            }
            composable("history") {
                HistoryScreen(
                    entries = history,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onRefresh = { viewModel.loadHistory() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("registered_vehicles") {
                RegisteredVehiclesScreen(
                    vehicles = registeredVehicles,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onRefresh = { viewModel.loadVehicles() },
                    onVehicleClick = { vehicle ->
                        viewModel.setScannedVehicle(vehicle)
                        navController.navigate("registered_vehicle_details")
                    },
                    onAddVehicle = { navController.navigate("admin_register_vehicle") }
                )
            }
            composable("admin_register_vehicle") {
                RegisterVehicleScreen(
                    prefilledNumber = null,
                    existingVehicle = null,
                    loading = loading,
                    error = error,
                    onSubmit = { num, name, mob, bld, flat, type, tag, tenant, rc, rent ->
                        viewModel.registerVehicle(num, name, mob, bld, flat, type, tag, tenant, rc, rent) {
                            navController.popBackStack()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("registered_vehicle_details") {
                VehicleDetailsScreen(
                    vehicle = scannedVehicle,
                    searchedNumber = null,
                    entryHistory = vehicleHistory,
                    onLoadHistory = { scannedVehicle?.let { viewModel.loadVehicleHistory(it.vehicleNumber) } },
                    onRegisterEntry = {
                        scannedVehicle?.let { viewModel.loadCachedVisitor(it.vehicleNumber) }
                        navController.navigate("visitor_entry_form")
                    },
                    onContinueAsVisitor = { navController.navigate("visitor_entry_form") },
                    onRegisterNewVehicle = {},
                    onEditVehicle = { navController.navigate("edit_vehicle") },
                    onDeleteVehicle = {
                        scannedVehicle?.let { v ->
                            viewModel.deleteVehicle(v.id) {
                                navController.popBackStack()
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("manage_users") {
                ManageUsersScreen(
                    users = users,
                    currentRole = role,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onRefresh = { viewModel.loadUsers() },
                    onCreateUser = { username, pin, r, name, onError -> viewModel.createUser(username, pin, r, name, {}, onError) },
                    onDeleteUser = { userId -> viewModel.deleteUser(userId) }
                )
            }
        }
    }
}
