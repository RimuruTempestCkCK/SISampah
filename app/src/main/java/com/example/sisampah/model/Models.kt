package com.example.sisampah.model

enum class UserRole {
    MASYARAKAT, PETUGAS_LPS, ADMIN, DLH
}

data class User(
    val id: String,
    val username: String,
    val role: UserRole,
    val name: String
)

data class TrashReport(
    val id: String,
    val reporterName: String,
    val location: String,
    val description: String,
    val status: String, // e.g., "Menunggu", "Diproses", "Selesai"
    val timestamp: String
)

data class PickupSchedule(
    val id: String,
    val area: String,
    val day: String,
    val time: String,
    val officerName: String
)


