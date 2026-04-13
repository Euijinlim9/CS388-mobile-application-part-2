package com.example.cs388_mobile_application_part_2

data class BoardGame(
    val id: String,
    val rank: Int,
    val name: String,
    val thumbnail: String,
    val yearPublished: String = "",
    val rating: String = "",
    val description: String = ""
)
