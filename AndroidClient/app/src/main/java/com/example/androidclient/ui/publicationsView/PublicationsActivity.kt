package com.example.androidclient.ui.publicationsView

import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
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
import com.example.androidclient.ui.login.PASSWORD
import kotlinx.android.synthetic.main.activity_pubs.*
import org.json.JSONObject

class PublicationsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: PublicationsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var requestQueue: RequestQueue
    private lateinit var username: String
    private lateinit var hashedPassword: String
    private var actionModeMenu: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pubs)
        setSupportActionBar(toolbar)
        val manager = SelfSignedManager()
        requestQueue =
            RequestQueueSingleton.getInstance(this.applicationContext, manager.makeHurlStack())
                .requestQueue
        //TODO do sth with username and pass when returning from view
//        val user = intent.getStringExtra(LOGIN)
//        val hashedPassword = intent.getStringExtra(PASSWORD)
        username = "Jack"
        hashedPassword = "85f293f02afec08cc90ec9b9501ff532c8c46c094850516700b5e8bd95bb570c"
        val json = intent.getStringExtra(JSON)
        if (json == null) {
            getJSONFromAPI(username, hashedPassword)
        } else {
            setRecyclerView(json)
        }

        fab.setOnClickListener {
            val intent = Intent(this, AddPublicationActivity::class.java).apply {
                putExtra(LOGIN, username)
                putExtra(PASSWORD, hashedPassword)
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

    private fun preparePubsList(json: String): ArrayList<Publication> {
        val publicationsArray = ArrayList<Publication>()
        val jsonObject = JSONObject(json)
        val publications = jsonObject.getJSONArray("publications")
        for (i in 0 until publications.length()) {
            val publicationJSON = publications.get(i) as String
            val pubArray = publicationJSON.split(":_+")
            val publication =
                Publication(pubArray[0].toInt(), pubArray[1], pubArray[2], pubArray[3], pubArray[4])
            publicationsArray.add(publication)
        }
        return publicationsArray
    }

    private fun setRecyclerView(json: String) {
        val publicationsList = preparePubsList(json)
        viewManager = LinearLayoutManager(this)
        viewAdapter = PublicationsAdapter(this, this.applicationContext, publicationsList)

        recyclerView = findViewById<RecyclerView>(R.id.pubs_recycler).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    fun startActionMode() {
        toolbar.startActionMode(PubsActionModeCallback())
    }

    fun stopActionMode() {
        actionModeMenu?.finish()
        actionModeMenu = null
    }

    inner class PubsActionModeCallback : ActionMode.Callback {
        private var shouldResetRecyclerView = false
        override fun onActionItemClicked(actionMode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.action_delete_file -> {
                    //TODO
                    // adapter usuwa link w publikacji i wysyła żądanie o usunięcie
                    viewAdapter.deleteSelectedFile()
                    shouldResetRecyclerView = true
                    actionMode.finish()
                    return true
                }
                R.id.action_delete_publication -> {
                    viewAdapter.deleteSelectedPublication(username, hashedPassword)
                    actionMode.finish()
                    return true
                }
                R.id.action_download -> {
                    //TODO handle downloading file
                    viewAdapter.downloadSelectedFile()
                    actionMode.finish()
                    return true
                }
            }
            return false
        }

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            actionModeMenu = mode
            val inflater = mode?.menuInflater
            inflater?.inflate(R.menu.contextual_menu, menu)
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return true
        }
    }
}
