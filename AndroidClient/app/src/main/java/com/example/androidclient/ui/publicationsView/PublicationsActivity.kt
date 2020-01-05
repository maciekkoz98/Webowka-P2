package com.example.androidclient.ui.publicationsView

import android.Manifest
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.example.androidclient.R
import com.example.androidclient.data.RequestQueueSingleton
import com.example.androidclient.ui.addPublicationView.AddPublicationActivity
import com.example.androidclient.ui.login.JSON
import com.example.androidclient.ui.login.LOGIN
import com.example.androidclient.ui.login.PASSWORD
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kotlinx.android.synthetic.main.activity_pubs.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

const val REQUEST_WRITE_EXTERNAL = 2137

class PublicationsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: PublicationsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var requestQueue: RequestQueue
    private lateinit var username: String
    private lateinit var hashedPassword: String
    private lateinit var menuActionModeCallback: MenuActionModeCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pubs)
        setSupportActionBar(toolbar)
        requestQueue =
            RequestQueueSingleton.getInstance(this.applicationContext)
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
        val linksMap = prepareMap(jsonObject.getString("_links"))
        for (i in 0 until publications.length()) {
            val publicationJSON = publications.get(i) as String
            val pubArray = publicationJSON.split(":_+")
            val publication =
                Publication(pubArray[0].toInt(), pubArray[1], pubArray[2], pubArray[3], pubArray[4])
            publication.filename = linksMap["$i:_+filename"]
            publication.downloadLink = linksMap["$i:_+download"]
            publication.deleteLink = linksMap["$i:_+delete"]
            publicationsArray.add(publication)
        }
        return publicationsArray
    }

    private fun prepareMap(links: String): HashMap<String, String> {
        val betterLinks = links.replace("\\/", "/")
        val linksArray = betterLinks.split("\"")
        val onlyLinks = ArrayList<String>()
        for (i in 1 until linksArray.size step 2) {
            onlyLinks.add(linksArray[i])
        }
        val map = HashMap<String, String>()
        var index = 0
        for (i in 0 until onlyLinks.size step 3) {
            val key = onlyLinks[i]
            val link = onlyLinks[i + 2]
            if (key == "self") {
                break
            } else if (key == "$index:_+download") {
                val filename = link.substring(39)
                map["$index:_+filename"] = filename
                index += 1
            }
            map[key] = link
        }
        return map
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

    fun startActionMode(menuWithFile: Boolean) {
        if (menuWithFile) {
            menuActionModeCallback = WithFileActionModeCallback(
                null,
                viewAdapter,
                username,
                hashedPassword,
                getString(R.string.selected)
            )
        } else {
            menuActionModeCallback = WithoutFileActionModeCallback(
                null,
                viewAdapter,
                username,
                hashedPassword,
                getString(R.string.selected)
            )
        }
        menuActionModeCallback.startActionMode(toolbar)
    }

    fun stopActionMode() {
        menuActionModeCallback.finishActionMode()
    }

    fun createFile() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL
            )
        } else {
            val publication = viewAdapter.getSelectedPublication()
            viewAdapter.onTap(viewAdapter.selectedID)
            println(publication.downloadLink)
            val token = getJWTToken(publication.filename)
            val url = "https://10.0.2.2" + publication.downloadLink?.substring(29) + "?token=$token"
            println(url)
            val request =
                DownloadManager.Request(Uri.parse(url))
            request.apply {
                allowScanningByMediaScanner()
                setTitle(publication.filename)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                addRequestHeader("Content-Type", "multipart/form-data")
                setMimeType("multipart/form-data")
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    publication.filename
                )
            }
            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val downloadID = downloadManager.enqueue(request)
        }
    }

    private fun getJWTToken(filename: String?): String {
        val jwt = Jwts.builder().setIssuer("fileshare.company.com")
        jwt.claim("file_id", filename)
        jwt.claim("action", "download")
        val date = Date().time + 30000
        println(Date(date))
        jwt.setExpiration(Date(date))
        val key = Keys.hmacShaKeyFor("sekretnehaslosekretnehaslosekretnehaslo".toByteArray())
        println(key)
        jwt.signWith(key)
        return jwt.compact()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_WRITE_EXTERNAL -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createFile()
                } else {
                    val noWriteMes = getString(R.string.no_write_permission)
                    Toast.makeText(
                        applicationContext,
                        noWriteMes,
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
            else -> {

            }
        }
    }
}
