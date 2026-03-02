package com.ananta.towers.data

import com.google.gson.annotations.SerializedName

data class AppUser(
    val id: Int,
    val username: String,
    val role: String,
    val name: String,
    @SerializedName("is_active") val isActive: Int
)

data class CreateUserRequest(val username: String, val pin: String, val role: String, val name: String)

data class Vehicle(
    val id: String,
    @SerializedName("vehicle_number") val vehicleNumber: String,
    @SerializedName("owner_name") val ownerName: String,
    val building: String,
    @SerializedName("flat_number") val flatNumber: String,
    @SerializedName("mobile_number") val mobileNumber: String,
    @SerializedName("vehicle_type") val vehicleType: String,
    @SerializedName("society_sticker_number") val societyStickerNumber: String?,
    @SerializedName("tag_number") val tagNumber: String?,
    @SerializedName("rc_book_url") val rcBookUrl: String?,
    @SerializedName("rent_agreement_url") val rentAgreementUrl: String?,
    @SerializedName("is_tenant") val isTenant: Boolean = false,
    @SerializedName("vehicle_photo_url") val vehiclePhotoUrl: String?,
    @SerializedName("is_active") val isActive: Boolean
)

data class Visitor(
    val id: String,
    @SerializedName("visitor_name") val visitorName: String,
    @SerializedName("mobile_number") val mobileNumber: String,
    @SerializedName("photo_url") val photoUrl: String?,
    @SerializedName("id_photo_url") val idPhotoUrl: String?
)

enum class EntryStatus { INSIDE, EXITED }

data class VisitorEntry(
    val id: String,
    @SerializedName("visitor_id") val visitorId: String,
    val visitor: Visitor?,
    val name: String?,
    @SerializedName("vehicle_number") val vehicleNumber: String?,
    val building: String,
    @SerializedName("flat_number") val flatNumber: String,
    @SerializedName("entry_time") val entryTime: String,
    @SerializedName("exit_time") val exitTime: String?,
    @SerializedName("guard_id") val guardId: String,
    val status: EntryStatus,
    val notes: String?
)
