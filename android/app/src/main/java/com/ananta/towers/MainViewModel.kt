package com.ananta.towers

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ananta.towers.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val authStore = AuthStore(app)
    private val visitorCache = VisitorCache(app)
    val token: StateFlow<String?> = authStore.token.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val role: StateFlow<String?> = authStore.role.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val userName: StateFlow<String?> = authStore.name.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _users = MutableStateFlow<List<AppUser>>(emptyList())
    val users: StateFlow<List<AppUser>> = _users

    private val _activeEntries = MutableStateFlow<List<VisitorEntry>>(emptyList())
    val activeEntries: StateFlow<List<VisitorEntry>> = _activeEntries

    private val _vehicleHistory = MutableStateFlow<List<VisitorEntry>>(emptyList())
    val vehicleHistory: StateFlow<List<VisitorEntry>> = _vehicleHistory

    private val _cachedVisitor = MutableStateFlow<CachedVisitor?>(null)
    val cachedVisitor: StateFlow<CachedVisitor?> = _cachedVisitor

    private val _registeredVehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val registeredVehicles: StateFlow<List<Vehicle>> = _registeredVehicles

    private val _scannedVehicle = MutableStateFlow<Vehicle?>(null)
    val scannedVehicle: StateFlow<Vehicle?> = _scannedVehicle

    private val _selectedEntry = MutableStateFlow<VisitorEntry?>(null)
    val selectedEntry: StateFlow<VisitorEntry?> = _selectedEntry

    private val _searchedNumber = MutableStateFlow<String?>(null)
    val searchedNumber: StateFlow<String?> = _searchedNumber

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun register(username: String, pin: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) = viewModelScope.launch {
        _loading.value = true
        try {
            val res = ApiClient.service.register(RegisterRequest(username, pin, name))
            if (res.isSuccessful) {
                val body = res.body()!!
                authStore.saveSession("Bearer ${body.token}", body.role, body.name)
                onSuccess()
            } else onError(if (res.code() == 409) "Username already exists" else "Registration failed")
        } catch (e: Exception) {
            onError("Connection Error")
        } finally {
            _loading.value = false
        }
    }

    fun login(username: String, pin: String, onSuccess: (role: String) -> Unit, onError: (String) -> Unit) = viewModelScope.launch {
        _loading.value = true
        try {
            val res = ApiClient.service.login(LoginRequest(username, pin))
            if (res.isSuccessful) {
                val body = res.body()!!
                authStore.saveSession("Bearer ${body.token}", body.role, body.name)
                onSuccess(body.role)
            } else onError("Invalid username or PIN")
        } catch (e: Exception) {
            onError("Connection Error")
        } finally {
            _loading.value = false
        }
    }

    fun logout() = viewModelScope.launch { authStore.clearToken() }

    fun lookupVehicle(number: String, onFinished: () -> Unit) = viewModelScope.launch {
        val t = token.value ?: return@launch
        val upNumber = number.uppercase()
        _searchedNumber.value = upNumber
        _loading.value = true
        _error.value = null
        try {
            Log.d("MainViewModel", "Looking up vehicle: $upNumber")
            val res = ApiClient.service.lookupVehicle(t, upNumber)
            if (res.isSuccessful) {
                _scannedVehicle.value = res.body()
            } else {
                Log.e("MainViewModel", "Lookup failed: ${res.code()} ${res.message()}")
                _scannedVehicle.value = null
                _error.value = "Vehicle Not Registered"
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Lookup exception", e)
            _scannedVehicle.value = null
            _error.value = "Lookup Failed"
        } finally {
            _loading.value = false
            onFinished()
        }
    }

    private suspend fun processImage(uri: Uri, fileName: String): File? = withContext(Dispatchers.IO) {
        val ctx = getApplication<Application>()
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, options)
            }

            var width = options.outWidth
            var height = options.outHeight
            val maxSize = 1024
            var sampleSize = 1
            while (width / 2 >= maxSize || height / 2 >= maxSize) {
                width /= 2
                height /= 2
                sampleSize *= 2
            }

            val scaledOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, scaledOptions)
            } ?: return@withContext null

            val file = File(ctx.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            }
            file
        } catch (e: Exception) {
            Log.e("MainViewModel", "Image processing failed", e)
            null
        }
    }

    fun registerVehicle(
        vehicleNumber: String,
        ownerName: String,
        mobileNumber: String,
        building: String,
        flatNumber: String,
        vehicleType: String,
        tagNumber: String?,
        isTenant: Boolean,
        rcBookUri: Uri?,
        rentAgreementUri: Uri?,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        val t = token.value ?: return@launch
        _loading.value = true
        _error.value = null
        
        try {
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            builder.addFormDataPart("vehicle_number", vehicleNumber)
            builder.addFormDataPart("owner_name", ownerName)
            builder.addFormDataPart("mobile_number", mobileNumber)
            builder.addFormDataPart("building", building)
            builder.addFormDataPart("flat_number", flatNumber)
            builder.addFormDataPart("vehicle_type", vehicleType)
            builder.addFormDataPart("is_tenant", if (isTenant) "1" else "0")
            
            tagNumber?.let { 
                builder.addFormDataPart("tag_number", it)
                builder.addFormDataPart("society_sticker_number", it)
            }

            val safeVehicleNum = vehicleNumber.replace(" ", "_")
            val safeOwner = ownerName.replace(" ", "_")
            rcBookUri?.let { uri ->
                processImage(uri, "rc_${safeVehicleNum}_${safeOwner}.jpg")?.let { file ->
                    builder.addFormDataPart("rc_book", file.name, file.asRequestBody("image/jpeg".toMediaType()))
                }
            }
            
            rentAgreementUri?.let { uri ->
                processImage(uri, "rent_${safeVehicleNum}_${safeOwner}.jpg")?.let { file ->
                    builder.addFormDataPart("rent_agreement", file.name, file.asRequestBody("image/jpeg".toMediaType()))
                }
            }

            val body = builder.build()
            Log.d("MainViewModel", "Registering vehicle: $vehicleNumber")
            val res = ApiClient.service.registerVehicle(t, body)

            if (res.isSuccessful) {
                _scannedVehicle.value = res.body()
                onSuccess()
            } else {
                val errorMsg = "Server Error: ${res.code()}"
                Log.e("MainViewModel", "Registration failed: ${res.code()} ${res.errorBody()?.string()}")
                _error.value = errorMsg
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Registration exception", e)
            _error.value = e.message ?: "An unexpected error occurred"
        } finally {
            _loading.value = false
        }
    }

    fun updateVehicle(
        id: String,
        vehicleNumber: String,
        ownerName: String,
        mobileNumber: String,
        building: String,
        flatNumber: String,
        vehicleType: String,
        tagNumber: String?,
        isTenant: Boolean,
        rcBookUri: Uri?,
        rentAgreementUri: Uri?,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        val t = token.value ?: return@launch
        _loading.value = true
        _error.value = null
        try {
            val safeVehicleNum = vehicleNumber.replace(" ", "_")
            val safeOwner = ownerName.replace(" ", "_")
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            builder.addFormDataPart("vehicle_number", vehicleNumber)
            builder.addFormDataPart("owner_name", ownerName)
            builder.addFormDataPart("mobile_number", mobileNumber)
            builder.addFormDataPart("building", building)
            builder.addFormDataPart("flat_number", flatNumber)
            builder.addFormDataPart("vehicle_type", vehicleType)
            builder.addFormDataPart("is_tenant", if (isTenant) "1" else "0")
            tagNumber?.let {
                builder.addFormDataPart("tag_number", it)
                builder.addFormDataPart("society_sticker_number", it)
            }
            rcBookUri?.let { uri ->
                processImage(uri, "rc_${safeVehicleNum}_${safeOwner}.jpg")?.let { file ->
                    builder.addFormDataPart("rc_book", file.name, file.asRequestBody("image/jpeg".toMediaType()))
                }
            }
            rentAgreementUri?.let { uri ->
                processImage(uri, "rent_${safeVehicleNum}_${safeOwner}.jpg")?.let { file ->
                    builder.addFormDataPart("rent_agreement", file.name, file.asRequestBody("image/jpeg".toMediaType()))
                }
            }
            val res = ApiClient.service.updateVehicle(t, id, builder.build())
            if (res.isSuccessful) {
                val updated = res.body()
                _scannedVehicle.value = updated
                _registeredVehicles.value = _registeredVehicles.value.map { if (it.id == id) updated ?: it else it }
                onSuccess()
            } else {
                Log.e("MainViewModel", "Update failed: ${res.code()} ${res.errorBody()?.string()}")
                _error.value = "Update failed: ${res.code()}"
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Update exception", e)
            _error.value = e.message
        } finally {
            _loading.value = false
        }
    }

    fun deleteVehicle(id: String, onSuccess: () -> Unit) = viewModelScope.launch {
        val t = token.value ?: return@launch
        try {
            val vehicle = _registeredVehicles.value.find { it.id == id } ?: _scannedVehicle.value
            val res = ApiClient.service.deleteVehicle(t, id,
                rcBookUrl = vehicle?.rcBookUrl,
                rentAgreementUrl = vehicle?.rentAgreementUrl,
                vehiclePhotoUrl = vehicle?.vehiclePhotoUrl
            )
            if (res.isSuccessful) {
                _registeredVehicles.value = _registeredVehicles.value.filter { it.id != id }
                if (_scannedVehicle.value?.id == id) _scannedVehicle.value = null
                onSuccess()
            } else {
                _error.value = "Delete failed: ${res.code()}"
            }
        } catch (e: Exception) { _error.value = e.message }
    }

    private val _history = MutableStateFlow<List<VisitorEntry>>(emptyList())
    val history: StateFlow<List<VisitorEntry>> = _history

    fun loadUsers() = viewModelScope.launch {
        val t = token.value ?: return@launch
        try {
            val res = ApiClient.service.getUsers(t)
            if (res.isSuccessful) _users.value = res.body() ?: emptyList()
        } catch (e: Exception) { /* ignore */ }
    }

    fun createUser(username: String, pin: String, role: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) = viewModelScope.launch {
        val t = token.value ?: return@launch
        try {
            val res = ApiClient.service.createUser(t, CreateUserRequest(username, pin, role, name))
            if (res.isSuccessful) { loadUsers(); onSuccess() }
            else onError(if (res.code() == 409) "Username already exists" else "Failed to create user")
        } catch (e: Exception) { onError(e.message ?: "Error") }
    }

    fun deleteUser(id: String) = viewModelScope.launch {
        val t = token.value ?: return@launch
        try {
            val res = ApiClient.service.deleteUser(t, id)
            if (res.isSuccessful) _users.value = _users.value.filter { it.id.toString() != id }
        } catch (e: Exception) { /* ignore */ }
    }

    fun loadHistory() = viewModelScope.launch {
        val t = token.value ?: return@launch
        try {
            val res = ApiClient.service.getHistory(t)
            if (res.isSuccessful) _history.value = res.body() ?: emptyList()
        } catch (e: Exception) { /* ignore */ }
    }

    fun loadVehicleHistory(vehicleNumber: String) = viewModelScope.launch {
        val t = token.value ?: return@launch
        try {
            val res = ApiClient.service.getVehicleEntryHistory(t, vehicleNumber)
            if (res.isSuccessful) _vehicleHistory.value = res.body() ?: emptyList()
        } catch (e: Exception) { /* ignore */ }
    }

    fun loadCachedVisitor(vehicleNumber: String) = viewModelScope.launch {
        _cachedVisitor.value = visitorCache.load(vehicleNumber)
    }

    fun loadVehicles() = viewModelScope.launch {
        val t = token.value ?: return@launch
        try {
            val res = ApiClient.service.getVehicles(t)
            if (res.isSuccessful) _registeredVehicles.value = res.body() ?: emptyList()
        } catch (e: Exception) { /* Log error */ }
    }

    fun loadActive() = viewModelScope.launch {
        val t = token.value ?: return@launch
        try {
            val res = ApiClient.service.getActiveEntries(t)
            if (res.isSuccessful) _activeEntries.value = res.body() ?: emptyList()
        } catch (e: Exception) { /* Log error */ }
    }

    fun markExit(id: String) = viewModelScope.launch {
        val t = token.value ?: return@launch
        try {
            val res = ApiClient.service.markExit(t, id)
            if (res.isSuccessful) loadActive()
        } catch (e: Exception) { /* Log error */ }
    }

    fun createVisitorEntry(
        visitorName: String,
        mobileNumber: String,
        building: String,
        flatNumber: String,
        vehicleNumber: String?,
        notes: String?,
        idType: String,
        idPhotoUri: Uri?,
        photoUri: Uri,
        onSuccess: () -> Unit
    ) = viewModelScope.launch {
        val t = token.value ?: return@launch
        _loading.value = true
        try {
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            builder.addFormDataPart("visitor_name", visitorName)
            builder.addFormDataPart("mobile_number", mobileNumber)
            builder.addFormDataPart("building", building)
            builder.addFormDataPart("flat_number", flatNumber)
            builder.addFormDataPart("id_type", idType)
            vehicleNumber?.let { builder.addFormDataPart("vehicle_number", it) }
            notes?.let { builder.addFormDataPart("notes", it) }
            
            processImage(photoUri, "visitor_${System.currentTimeMillis()}.jpg")?.let { file ->
                builder.addFormDataPart("photo", file.name, file.asRequestBody("image/jpeg".toMediaType()))
            }

            idPhotoUri?.let { uri ->
                processImage(uri, "id_${System.currentTimeMillis()}.jpg")?.let { file ->
                    builder.addFormDataPart("id_photo", file.name, file.asRequestBody("image/jpeg".toMediaType()))
                }
            }

            val res = ApiClient.service.createVisitorEntry(t, builder.build())
            
            if (res.isSuccessful) {
                vehicleNumber?.let { vn -> visitorCache.save(vn, visitorName, mobileNumber, building, flatNumber) }
                loadActive()
                onSuccess()
            } else {
                _error.value = "Failed to log entry (Error ${res.code()})"
            }
        } catch (e: Exception) {
            _error.value = e.message
        } finally {
            _loading.value = false
        }
    }

    fun selectEntry(entry: VisitorEntry) {
        _selectedEntry.value = entry
    }

    fun setScannedVehicle(vehicle: Vehicle) {
        _scannedVehicle.value = vehicle
    }

    fun clearScannedVehicle() {
        _scannedVehicle.value = null
        _searchedNumber.value = null
    }
}
