package com.example.androidclient.ui.publicationsView

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.example.androidclient.R
import com.example.androidclient.data.RequestQueueSingleton
import com.example.androidclient.data.SelfSignedManager
import kotlinx.android.synthetic.main.pubs_view.view.*

class PublicationsAdapter(
    private val parent: PublicationsActivity,
    private val context: Context,
    private val pubsDataSet: ArrayList<Publication>
) :
    RecyclerView.Adapter<PubsViewHolder>(), ViewHolderClickListener {
    private var selectedID: Int = -1
    private lateinit var requestQueue: RequestQueue

    override fun onLongTap(index: Int) {
        if (selectedID != -1) {
            onTap(selectedID)
        }
        setIDSelected(index)
        if (pubsDataSet[index].filename != null) {
            parent.startActionMode(true)
        } else {
            parent.startActionMode(false)
        }
    }

    override fun onTap(index: Int) {
        if (selectedID == index) {
            setIDSelected(index)
            parent.stopActionMode()
        } else {
            if (pubsDataSet[index].filename != null)
                downloadSelectedFile()
        }
    }

    private fun setIDSelected(index: Int) {
        selectedID = when (index) {
            selectedID -> -1
            else -> index
        }
        notifyItemChanged(index)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PubsViewHolder {
        val manager = SelfSignedManager()
        requestQueue =
            RequestQueueSingleton.getInstance(context.applicationContext, manager.makeHurlStack())
                .requestQueue
        val linearLayout = LayoutInflater.from(parent.context).inflate(
            R.layout.pubs_view,
            parent,
            false
        ) as LinearLayout
        return PubsViewHolder(linearLayout, this)
    }

    override fun onBindViewHolder(holder: PubsViewHolder, position: Int) {
        val publication = pubsDataSet[position]
        holder.linearLayout.title.text = publication.title
        holder.linearLayout.author.text = publication.author
        holder.linearLayout.publisher.text = publication.publisher
        holder.linearLayout.year.text = publication.year
        if (publication.filename != null) {
            holder.linearLayout.file_link.text = publication.filename
        } else {
            holder.linearLayout.file_link.text = ""
        }

        if (position == selectedID) {
            holder.linearLayout.background =
                ColorDrawable(ContextCompat.getColor(context, R.color.highlightedPub))
        } else {
            holder.linearLayout.background =
                ColorDrawable(ContextCompat.getColor(context, R.color.defaultColor))
        }
    }

    override fun getItemCount() = pubsDataSet.size

    fun deleteSelectedFile() {
        val publication = pubsDataSet[selectedID]
    }

    fun deleteSelectedFile(index: Int) {
        val publication = pubsDataSet[index]
    }

    fun deleteSelectedPublication(username: String, hashedPassword: String) {
        val id = pubsDataSet[selectedID].id
        val url =
            "https://10.0.2.2/publications/delete/$id?username=$username&password=$hashedPassword"
        val stringRequest = StringRequest(Request.Method.GET, url, Response.Listener<String> {
            pubsDataSet.removeAt(selectedID)
            notifyDataSetChanged()
            setIDSelected(selectedID)
        }, Response.ErrorListener {
            val internetError = context.getString(R.string.internet_error)
            val dataSync = context.getString(R.string.data_sync_error)
            Toast.makeText(
                context.applicationContext,
                "$internetError\n$dataSync",
                Toast.LENGTH_LONG
            ).show()
            setIDSelected(selectedID)
        })

        requestQueue.add(stringRequest)
    }

    fun downloadSelectedFile() {

    }

    fun uploadFile() {

    }
}