package com.example.androidclient.ui.publicationsView

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.example.androidclient.R

class WithFileActionModeCallback(
    private var actionMode: ActionMode?,
    private val viewAdapter: PublicationsAdapter,
    private val username: String,
    private val hashedPassword: String,
    private val title: String
) : ActionMode.Callback, MenuActionModeCallback {


    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete_file -> {
                viewAdapter.deleteSelectedFile(username, viewAdapter.selectedID)
                mode.finish()
                return true
            }
            R.id.action_delete_publication -> {
                viewAdapter.deleteSelectedPublication(
                    username,
                    viewAdapter.selectedID
                )
                mode.finish()
                return true
            }
            R.id.action_download -> {
                viewAdapter.downloadSelectedFile()
                mode.finish()
                return true
            }
        }
        return false
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
        mode.title = title
        actionMode = mode
        val inflater = mode.menuInflater
        inflater?.inflate(R.menu.with_file_menu, menu)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        if (viewAdapter.selectedID != -1) {
            viewAdapter.onTap(viewAdapter.selectedID)
        }
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu?): Boolean {
        return true
    }

    override fun startActionMode(toolbar: Toolbar) {
        toolbar.startActionMode(this)
    }

    override fun finishActionMode() {
        actionMode?.finish()
    }

}