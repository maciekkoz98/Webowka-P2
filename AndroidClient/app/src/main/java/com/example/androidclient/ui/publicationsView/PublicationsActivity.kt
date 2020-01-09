package com.example.androidclient.ui.publicationsView

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.Menu
import android.view.MenuItem
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
import com.example.androidclient.data.FileSendRequestVolley
import com.example.androidclient.data.RequestQueueSingleton
import com.example.androidclient.ui.addPublicationView.AddPublicationActivity
import com.example.androidclient.ui.login.JSON
import com.example.androidclient.ui.login.LOGIN
import com.example.androidclient.ui.login.LoginActivity
import com.example.androidclient.ui.login.PASSWORD
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kotlinx.android.synthetic.main.activity_pubs.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

const val REQUEST_WRITE_EXTERNAL = 2137
const val PICK_PDF_FILE = 2

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
        username = "jack@pw.edu.pl"
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
            publication.filename = linksMap["${publication.id}:_+filename"]
            publication.downloadLink = linksMap["${publication.id}:_+download"]
            publication.deleteLink = linksMap["${publication.id}:_+delete"]
            publicationsArray.add(publication)
        }
        return publicationsArray
    }

    fun prepareMap(links: String): HashMap<String, String> {
        val betterLinks = links.replace("\\/", "/")
        val linksArray = betterLinks.split("\"")
        val onlyLinks = ArrayList<String>()
        for (i in 1 until linksArray.size step 2) {
            onlyLinks.add(linksArray[i])
        }
        val map = HashMap<String, String>()
        for (i in 0 until onlyLinks.size step 3) {
            val key = onlyLinks[i]
            val index = key[0]
            val link = onlyLinks[i + 2]
            if (key == "self") {
                break
            } else if (key == "$index:_+download") {
                val filename = link.substring(39)
                map["$index:_+filename"] = filename
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
            val publication: Publication
            if (viewAdapter.selectedID != -1) {
                publication = viewAdapter.getSelectedPublication()
                viewAdapter.onTap(viewAdapter.selectedID)
            } else {
                publication = viewAdapter.getTappedPublication()
            }
            val token = getJWTToken(publication.filename)
            val url =
                "https://10.0.2.2" + publication.downloadLink?.substring(29) + "?token=$token"
            val request =
                DownloadManager.Request(Uri.parse(url))
            request.apply {
                setTitle(publication.filename)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setMimeType("application/pdf")
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    publication.filename
                )
            }
            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        }
    }

    private fun getJWTToken(filename: String?): String {
        val jwt = Jwts.builder().setIssuer("fileshare.company.com")
        jwt.claim("file_id", filename)
        jwt.claim("action", "download")
        val date = Date().time + 30000
        jwt.setExpiration(Date(date))
        val key = Keys.hmacShaKeyFor("sekretnehaslosekretnehaslosekretnehaslo".toByteArray())
        jwt.signWith(key)
        return jwt.compact()
    }

    fun uploadFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        startActivityForResult(intent, PICK_PDF_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == PICK_PDF_FILE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.moveToFirst()
                val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val filename = cursor?.getString(nameIndex!!)
                cursor?.close()
                val fileStream = contentResolver.openInputStream(uri)
                val fileBytes = ByteArray(fileStream!!.available())
                fileStream.read(fileBytes)
                fileStream.close()
                val publication = viewAdapter.getTappedPublication()
                val url = "https://10.0.2.2/publications/${publication.id}"
                val request =
                    FileSendRequestVolley(url, Response.Listener {
                        viewAdapter.updatePublication(publication, username, hashedPassword)
                    }, Response.ErrorListener {
                        val internetError = getString(R.string.internet_error)
                        val dataSync = getString(R.string.data_sync_error)
                        Toast.makeText(
                            applicationContext,
                            "$internetError\n$dataSync",
                            Toast.LENGTH_LONG
                        ).show()
                    })
                request.paramsMap = prepareParamsMap()
                request.dataMap = prepareDataMap(filename!!, fileBytes)
                requestQueue.add(request)
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun prepareParamsMap(): Map<String, String> {
        val map = HashMap<String, String>()
        map["username"] = username
        map["password"] = hashedPassword
        return map
    }

    private fun prepareDataMap(
        filename: String,
        content: ByteArray
    ): Map<String, FileSendRequestVolley.DataPart> {
        val map = HashMap<String, FileSendRequestVolley.DataPart>()
        map["file"] = FileSendRequestVolley.DataPart(filename, content, "application/pdf")
        return map
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.logout_option, menu)
        return true
    }

    fun logoutUser(menuItem: MenuItem) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        setResult(Activity.RESULT_OK)
        finish()
    }

}
