package com.example.androidclient.ui.publicationsView

import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView

class PubsViewHolder(val linearLayout: LinearLayout, private val tap: ViewHolderClickListener) :
    RecyclerView.ViewHolder(linearLayout), View.OnClickListener, View.OnLongClickListener {

    init {
        linearLayout.setOnLongClickListener(this)
        linearLayout.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        tap.onTap(adapterPosition)
    }

    override fun onLongClick(v: View?): Boolean {
        tap.onLongTap(adapterPosition)
        return true
    }
}