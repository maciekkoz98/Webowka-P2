package com.example.androidclient.ui.addPublicationView

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.example.androidclient.R
import com.example.androidclient.data.RequestQueueSingleton
import com.example.androidclient.ui.login.JSON
import com.example.androidclient.ui.login.LOGIN
import com.example.androidclient.ui.publicationsView.PublicationsActivity
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class AddPublicationActivity : AppCompatActivity() {

    private var username: String? = null
    private var json: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_publication)

        username = intent.getStringExtra(LOGIN)
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
        val token = createListToken(title + author + year + publisher)
        sendJSONRequest(pubsJSON, token)
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
        return pubsJSON
    }

    private fun sendJSONRequest(jsonObject: JSONObject, token: String) {
        val requestQueue =
            RequestQueueSingleton.getInstance(this.applicationContext)
                .requestQueue
        val url = "https://10.0.2.2/publications"
        val jsonObjectRequest =
            object : JsonObjectRequest(
                Method.POST, url, jsonObject, null,
                Response.ErrorListener {}) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Bearer $token"
                    return headers
                }
            }
        requestQueue.add(jsonObjectRequest)
    }

    private fun createListToken(publication: String): String {
        val jwt = Jwts.builder().setIssuer("filesapi.company.com")
        jwt.claim("action", "addPub")
        jwt.claim("publication", publication)
        val date = Date().time + 30000
        jwt.setExpiration(Date(date))
        val key = Keys.hmacShaKeyFor("sekretnehaslosekretnehaslosekretnehaslo".toByteArray())
        jwt.signWith(key)
        return jwt.compact()
    }
}
