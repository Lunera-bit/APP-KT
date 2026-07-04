package com.cyryel.data.user

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val documentNumber: String = "",
    val ruc: String = "",
    val addresses: List<Address> = emptyList(),
    val role: String = "user",
    val fcmToken: String = "",
    val points: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Address(
    val id: String = "",
    val name: String = "",
    val type: String = "home",
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val reference: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isDefault: Boolean = false
)
