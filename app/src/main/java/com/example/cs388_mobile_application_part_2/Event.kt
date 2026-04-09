package com.example.cs388_mobile_application_part_2

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Event(
    @SerialName("title")
    val title: String?,
    @SerialName("content")
    val content: String?,
    @SerialName("time")
    val time: String?,
    @SerialName("id")
    val id: Long?,
    @SerialName("petId")
    val petId: Long?
)