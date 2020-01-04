package com.example.androidclient.ui.publicationsView

data class Publication(
    val id: Int,
    val title: String,
    val author: String,
    val publisher: String,
    val year: String
) {
    var filename: String? = null
    var downloadLink: String? = null
    var deleteLink: String? = null
}