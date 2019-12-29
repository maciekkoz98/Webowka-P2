package com.example.androidclient.ui.login

/**
 * User details post authentication that is exposed to the UI
 */
data class LoggedInUserView(
    val username: String,
    val hashedPassword: String
    //... other data fields that may be accessible to the UI
)
