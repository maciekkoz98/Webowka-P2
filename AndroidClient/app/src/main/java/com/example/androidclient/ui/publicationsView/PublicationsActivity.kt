package com.example.androidclient.ui.publicationsView

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.R

import kotlinx.android.synthetic.main.activity_pubs.*

class PublicationsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pubs)
        setSupportActionBar(toolbar)

        //TODO get publicationsDataset
        val macPub = Publication("mactitle", "macAuthor", "macPub", "1234")
        val kacPub = Publication("kactitle", "kacAuthor", "kacPub", "2137")
        val pacPub = Publication("pactitle", "pacAuthor", "pacPub", "9876")
        val publicationsDataset = arrayOf(macPub, kacPub, pacPub)
        viewManager = LinearLayoutManager(this)
        viewAdapter = PublicationsAdapter(publicationsDataset)

        recyclerView = findViewById<RecyclerView>(R.id.pubs_recycler).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Tu będzie Dodaj Publikację!", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

}
