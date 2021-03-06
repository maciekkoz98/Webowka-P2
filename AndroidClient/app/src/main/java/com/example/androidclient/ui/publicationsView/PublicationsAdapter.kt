package com.example.androidclient.ui.publicationsView

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.example.androidclient.R
import com.example.androidclient.data.RequestQueueSingleton
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kotlinx.android.synthetic.main.pubs_view.view.*
import org.json.JSONObject
import java.util.*

class PublicationsAdapter(
    private val parent: PublicationsActivity,
    private val context: Context,
    private val pubsDataSet: ArrayList<Publication>
) :
    RecyclerView.Adapter<PubsViewHolder>(), ViewHolderClickListener {
    var selectedID: Int = -1
    private var tappedID: Int = -1
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
            tappedID = index
            parent.stopActionMode()
        } else {
            if (pubsDataSet[index].filename != null && selectedID == -1) {
                tappedID = index
                downloadSelectedFile()
            }
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
        requestQueue =
            RequestQueueSingleton.getInstance(context.applicationContext)
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

    fun deleteSelectedFile(username: String, pubID: Int) {
        val publication = pubsDataSet[pubID]
        val token = createDeleteFileToken()
        var deleteLink = publication.deleteLink
        deleteLink ?: return
        deleteLink =
            "https://10.0.2.2$deleteLink?username=$username"
        val stringRequest = object :
            StringRequest(Method.GET, deleteLink, Response.Listener<String> {
                publication.filename = null
                publication.deleteLink = null
                publication.downloadLink = null
                notifyItemChanged(pubID)
            }, Response.ErrorListener {
                makeErrorToast()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        requestQueue.add(stringRequest)
    }

    fun deleteSelectedPublication(username: String, pubID: Int) {
        val id = pubsDataSet[pubID].id
        val token = createDeletePubToken(id.toString())
        val url =
            "https://10.0.2.2/publications/delete/$id?username=$username"
        val stringRequest =
            object : StringRequest(Method.GET, url, Response.Listener<String> {
                pubsDataSet.removeAt(pubID)
                notifyDataSetChanged()
            }, Response.ErrorListener {
                makeErrorToast()
            }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Bearer $token"
                    return headers
                }
            }
        requestQueue.add(stringRequest)
    }

    fun downloadSelectedFile() {
        parent.createFile()
    }

    fun uploadFile() {
        parent.uploadFile()
    }

    fun updatePublication(pub: Publication, username: String) {
        val index = pubsDataSet.indexOf(pub)
        val token = createListToken()
        val url = "https://10.0.2.2/publications?username=$username"
        val jsonObjectRequest = object :
            JsonObjectRequest(Method.GET, url, null, Response.Listener { response ->
                val json = JSONObject(response.toString())
                val links = json.getString("_links")
                val linksMap = parent.prepareMap(links)
                pubsDataSet[index].filename = linksMap["${pub.id}:_+filename"]
                pubsDataSet[index].downloadLink = linksMap["${pub.id}:_+download"]
                pubsDataSet[index].deleteLink = linksMap["${pub.id}:_+delete"]
                notifyItemChanged(index)
            }, Response.ErrorListener {
                makeErrorToast()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        requestQueue.add(jsonObjectRequest)
    }

    private fun makeErrorToast() {
        val internetError = context.getString(R.string.internet_error)
        val dataSync = context.getString(R.string.data_sync_error)
        Toast.makeText(
            context.applicationContext,
            "$internetError\n$dataSync",
            Toast.LENGTH_LONG
        ).show()
    }

    fun getSelectedPublication(): Publication {
        return pubsDataSet[selectedID]
    }

    fun getTappedPublication(): Publication {
        val pub = pubsDataSet[tappedID]
        tappedID = -1
        return pub
    }

    private fun createDeletePubToken(pubID: String): String {
        val jwt = Jwts.builder().setIssuer("filesapi.company.com")
        jwt.claim("action", "deletePub")
        jwt.claim("pubID", pubID)
        val date = Date().time + 30000
        jwt.setExpiration(Date(date))
        val key = Keys.hmacShaKeyFor("sekretnehaslosekretnehaslosekretnehaslo".toByteArray())
        jwt.signWith(key)
        return jwt.compact()
    }

    private fun createListToken(): String {
        val jwt = Jwts.builder().setIssuer("filesapi.company.com")
        jwt.claim("action", "listPubs")
        val date = Date().time + 30000
        jwt.setExpiration(Date(date))
        val key = Keys.hmacShaKeyFor("sekretnehaslosekretnehaslosekretnehaslo".toByteArray())
        jwt.signWith(key)
        return jwt.compact()
    }

    private fun createDeleteFileToken(): String {
        val jwt = Jwts.builder().setIssuer("filesapi.company.com")
        jwt.claim("action", "deleteFile")
        val date = Date().time + 30000
        jwt.setExpiration(Date(date))
        val key = Keys.hmacShaKeyFor("sekretnehaslosekretnehaslosekretnehaslo".toByteArray())
        jwt.signWith(key)
        return jwt.compact()
    }
}