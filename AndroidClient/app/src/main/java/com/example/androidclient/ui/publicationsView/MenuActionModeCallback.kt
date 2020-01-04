package com.example.androidclient.ui.publicationsView

import androidx.appcompat.widget.Toolbar

interface MenuActionModeCallback {
    fun startActionMode(toolbar: Toolbar)
    fun finishActionMode()
}