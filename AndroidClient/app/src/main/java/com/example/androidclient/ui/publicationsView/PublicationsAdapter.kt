package com.example.androidclient.ui.publicationsView

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.R
import kotlinx.android.synthetic.main.pubs_view.view.*

class PublicationsAdapter(private val pubsDataSet: List<Publication>) :
    RecyclerView.Adapter<PublicationsAdapter.PubsViewHolder>() {
    class PubsViewHolder(val linearLayout: LinearLayout) : RecyclerView.ViewHolder(linearLayout)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PubsViewHolder {
        val linearLayout = LayoutInflater.from(parent.context).inflate(
            R.layout.pubs_view,
            parent,
            false
        ) as LinearLayout
        return PubsViewHolder(linearLayout)
    }

    override fun onBindViewHolder(holder: PubsViewHolder, position: Int) {
        val publication = pubsDataSet[position]
        holder.linearLayout.title.text = publication.title
        holder.linearLayout.author.text = publication.author
        holder.linearLayout.publisher.text = publication.publisher
        holder.linearLayout.year.text = publication.year
    }

    override fun getItemCount() = pubsDataSet.size
}