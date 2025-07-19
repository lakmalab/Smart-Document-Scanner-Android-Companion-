package com.projectinsight.smartdocumentscanner.util

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserResponse(
    val name: String,
    val email: String
)