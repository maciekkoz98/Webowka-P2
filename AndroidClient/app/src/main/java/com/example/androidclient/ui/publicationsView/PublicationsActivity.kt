package com.example.androidclient.ui.publicationsView

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.example.androidclient.R
import com.example.androidclient.data.RequestQueueSingleton
import com.example.androidclient.data.SelfSignedManager
import com.example.androidclient.ui.addPublicationView.AddPublicationActivity
import com.example.androidclient.ui.login.JSON
import com.example.androidclient.ui.login.LOGIN
import kotlinx.android.synthetic.main.activity_pubs.*
import org.json.JSONObject

class PublicationsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var requestQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pubs)
        setSupportActionBar(toolbar)
        val manager = SelfSignedManager()
        //requestQueue = Volley.newRequestQueue(this, manager.makeHurlStack())
        requestQueue =
            RequestQueueSingleton.getInstance(this.applicationContext, manager.makeHurlStack())
                .requestQueue
        //TODO do sth with username and pass when returning from view
//        val user = intent.getStringExtra(LOGIN)
//        val hashedPassword = intent.getStringExtra(PASSWORD)
        val user = "Jack"
        val hashedPassword = "85f293f02afec08cc90ec9b9501ff532c8c46c094850516700b5e8bd95bb570c"
        val json = intent.getStringExtra(JSON)
        if (json == null) {
            getJSONFromAPI(user, hashedPassword)
        } else {
            setRecyclerView(json)
        }

        fab.setOnClickListener {
            val intent = Intent(this, AddPublicationActivity::class.java).apply {
                putExtra(LOGIN, user)
                putExtra(JSON, json)
            }
            startActivity(intent)
        }
    }

    private fun getJSONFromAPI(username: String?, hashedPassword: String?) {
        val url = "https://10.0.2.2/publications?username=$username&password=$hashedPassword"
        val jsonObjectRequest =
            JsonObjectRequest(Request.Method.GET, url, null, Response.Listener { response ->
                val pubsJSON = response.toString()
                setRecyclerView(pubsJSON)
            }, Response.ErrorListener {
                val internetError = getString(R.string.internet_error)
                val tryAgainLater = getString(R.string.try_again_later)
                Toast.makeText(
                    applicationContext,
                    "$internetError\n$tryAgainLater",
                    Toast.LENGTH_LONG
                ).show()
            })
        requestQueue.add(jsonObjectRequest)
    }

    private fun preparePubsDataSet(json: String): List<Publication> {
        val publicationsArray = ArrayList<Publication>()
        val jsonObject = JSONObject(json)
        val publications = jsonObject.getJSONArray("publications")
        for (i in 0 until publications.length()) {
            val publicationJSON = publications.get(i) as String
            val pubArray = publicationJSON.split(":_+")
            val publication = Publication(pubArray[1], pubArray[2], pubArray[3], pubArray[4])
            publicationsArray.add(publication)
        }
        return publicationsArray
    }

    private fun setRecyclerView(json: String) {
        val publicationsDataset = preparePubsDataSet(json)
        viewManager = LinearLayoutManager(this)
        viewAdapter = PublicationsAdapter(publicationsDataset)

        recyclerView = findViewById<RecyclerView>(R.id.pubs_recycler).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

}
