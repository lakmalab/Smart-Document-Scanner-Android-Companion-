package com.projectinsight.smartdocumentscanner.util


import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserResponse(
    val userId: Long,
    val name: String,
    val email: String,
    val role: String? = null
)