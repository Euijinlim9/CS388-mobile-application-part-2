package com.example.cs388_mobile_application_part_2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    @SerialName("schemaVersion") val schemaVersion: Int = 1,
    @SerialName("pets") val pets: List<BackupPet>,
    @SerialName("events") val events: List<BackupEvent>
)

@Serializable
data class BackupPet(
    @SerialName("legacyId") val legacyId: Long,
    @SerialName("name") val name: String,
    @SerialName("age") val age: Int
)

@Serializable
data class BackupEvent(
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("time") val time: Long,
    @SerialName("petLegacyId") val petLegacyId: Long
)

