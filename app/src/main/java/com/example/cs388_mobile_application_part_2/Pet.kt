package com.example.cs388_mobile_application_part_2

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Pet(
    @SerialName("name")
    val name: String?,
    @SerialName("age")
    val age: Int?,
    @SerialName("id")
    val id: Long?
)