package com.example.androidclient.ui.publicationsView

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.R
import com.example.androidclient.ui.addPublicationView.AddPublicationActivity
import com.example.androidclient.ui.login.JSON
import com.example.androidclient.ui.login.LOGIN
import com.example.androidclient.ui.login.PASSWORD

import kotlinx.android.synthetic.main.activity_pubs.*
import org.json.JSONObject

class PublicationsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pubs)
        setSupportActionBar(toolbar)
        val user = intent.getStringExtra(LOGIN)
        val hashedPassword = intent.getStringExtra(PASSWORD)
        val json = intent.getStringExtra(JSON)

        val publicationsDataset = preparePubsDataSet(json!!)
        viewManager = LinearLayoutManager(this)
        viewAdapter = PublicationsAdapter(publicationsDataset)

        recyclerView = findViewById<RecyclerView>(R.id.pubs_recycler).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        fab.setOnClickListener {
            val intent = Intent(this, AddPublicationActivity::class.java).apply {
                putExtra(LOGIN, user)
                putExtra(JSON, json)
            }
            startActivity(intent)
        }
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

}
