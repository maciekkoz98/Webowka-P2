package com.example.androidclient.ui.addPublicationView

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.androidclient.R
import com.example.androidclient.ui.login.JSON

class AddPublicationActivity : AppCompatActivity() {

    private var json: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_publication)

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

        //Dodaj do json i wyślij żądanie o dodanie na serwer
        //czy da się mieć jedną kolejkę na całą apkę?
    }
}
