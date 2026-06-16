package com.example.chisa.backend.model

data class FileState(
    val nodes: MutableMap<String, Node> = mutableMapOf()
)
