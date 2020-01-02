package com.example.androidclient.ui.publicationsView

data class Publication(
    val id: Int,
    val title: String,
    val author: String,
    val publisher: String,
    val year: String
) {
    private var filename: String? = null
}