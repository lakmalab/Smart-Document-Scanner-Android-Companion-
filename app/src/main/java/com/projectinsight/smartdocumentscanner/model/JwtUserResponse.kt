package com.projectinsight.smartdocumentscanner.model

import com.projectinsight.smartdocumentscanner.util.UserResponse

data class JwtUserResponse(
    val token: String,
    val user: UserResponse
)
