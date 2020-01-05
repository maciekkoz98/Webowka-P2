package com.example.androidclient.ui.addPublicationView

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.example.androidclient.R
import com.example.androidclient.data.RequestQueueSingleton
import com.example.androidclient.ui.login.JSON
import com.example.androidclient.ui.login.LOGIN
import com.example.androidclient.ui.login.PASSWORD
import com.example.androidclient.ui.publicationsView.PublicationsActivity
import org.json.JSONObject

class AddPublicationActivity : AppCompatActivity() {

    private var username: String? = null
    private var password: String? = null
    private var json: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_publication)

        username = intent.getStringExtra(LOGIN)
        password = intent.getStringExtra(PASSWORD)
        json = intent.getStringExtra(JSON)
    }

    fun addPublication(view: View) {
        val title = findViewById<TextView>(R.id.title_setter).text.toString()
        val author = findViewById<TextView>(R.id.author_setter).text.toString()
        val publisher = findViewById<TextView>(R.id.publisher_setter).text.toString()
        val year = findViewById<TextView>(R.id.year_setter).text.toString()
        if (title == "" || author == "" || publisher == "" || year == "") {
            return
        }
        val pubsJSON = prepareJSON(title, author, publisher, year)
        sendJSONRequest(pubsJSON)
        val intent = Intent(this, PublicationsActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun prepareJSON(
        title: String,
        author: String,
        publisher: String,
        year: String
    ): JSONObject {
        val pubsJSON = JSONObject()
        pubsJSON.put("title", title)
        pubsJSON.put("author", author)
        pubsJSON.put("publisher", publisher)
        pubsJSON.put("year", year)
        pubsJSON.put("username", username)
        pubsJSON.put("password", password)
        return pubsJSON
    }

    private fun sendJSONRequest(jsonObject: JSONObject) {
        val requestQueue =
            RequestQueueSingleton.getInstance(this.applicationContext)
                .requestQueue
        val url = "https://10.0.2.2/publications"
        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, jsonObject, null,
            Response.ErrorListener {})
        requestQueue.add(jsonObjectRequest)
    }
}
