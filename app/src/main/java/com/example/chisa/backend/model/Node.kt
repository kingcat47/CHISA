package com.example.chisa.backend.model

data class Node(
    var name: String,
    val type: String,
    var parent: String? = null,
    val children: MutableList<String> = mutableListOf(),
    var summary: String? = null,
    var content: String? = null
)
