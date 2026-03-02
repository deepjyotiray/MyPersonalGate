package com.ananta.towers.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

class VisitorCache(private val context: Context) {
    private fun nameKey(v: String) = stringPreferencesKey("vc_name_$v")
    private fun mobileKey(v: String) = stringPreferencesKey("vc_mobile_$v")
    private fun buildingKey(v: String) = stringPreferencesKey("vc_building_$v")
    private fun flatKey(v: String) = stringPreferencesKey("vc_flat_$v")

    suspend fun save(vehicleNumber: String, name: String, mobile: String, building: String, flat: String) {
        context.dataStore.edit {
            it[nameKey(vehicleNumber)] = name
            it[mobileKey(vehicleNumber)] = mobile
            it[buildingKey(vehicleNumber)] = building
            it[flatKey(vehicleNumber)] = flat
        }
    }

    suspend fun load(vehicleNumber: String): CachedVisitor? {
        val prefs = context.dataStore.data.first()
        val name = prefs[nameKey(vehicleNumber)] ?: return null
        return CachedVisitor(
            name = name,
            mobile = prefs[mobileKey(vehicleNumber)] ?: "",
            building = prefs[buildingKey(vehicleNumber)] ?: "",
            flat = prefs[flatKey(vehicleNumber)] ?: ""
        )
    }
}

data class CachedVisitor(val name: String, val mobile: String, val building: String, val flat: String)
