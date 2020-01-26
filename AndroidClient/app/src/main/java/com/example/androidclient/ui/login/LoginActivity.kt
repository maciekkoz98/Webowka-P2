package com.example.androidclient.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.example.androidclient.R
import com.example.androidclient.data.RequestQueueSingleton
import com.example.androidclient.ui.publicationsView.PublicationsActivity
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.*
import kotlin.collections.HashMap

const val LOGIN = "com.example.androidclient.ui.login.LOGIN"
const val PASSWORD = "com.example.androidclient.ui.login.PASSWORD"
const val JSON = "filesapi.company.com.JSON"

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val username = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)
        val login = findViewById<Button>(R.id.login)
        val loading = findViewById<ProgressBar>(R.id.loading)

        loginViewModel = ViewModelProviders.of(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
                return@Observer
            }
            if (loginResult.success != null) {
                if (!updateUiWithUser(loginResult.success)) {
                    return@Observer
                }
            }
        })

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                loginViewModel.login(username.text.toString(), password.text.toString())
            }
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView): Boolean {
        val username = model.username
        val hashedPassword = model.hashedPassword
        getJSON(username, hashedPassword)
        return false
    }

    private fun getJSON(username: String, hashedPassword: String) {
        val loading = findViewById<ProgressBar>(R.id.loading)
        loading.visibility = View.VISIBLE

        val requestQueue =
            RequestQueueSingleton.getInstance(this.applicationContext)
                .requestQueue

        val token = createListToken();
        val url = "https://10.0.2.2/publications?username=$username"
        val jsonObjectRequest = object :
            JsonObjectRequest(Request.Method.GET, url, null, Response.Listener { response ->
                loading.visibility = View.GONE
                val pubsJSON = response.toString()
                val intent = Intent(this, PublicationsActivity::class.java).apply {
                    putExtra(LOGIN, username)
                    putExtra(PASSWORD, hashedPassword)
                    putExtra(JSON, pubsJSON)
                }
                startActivity(intent)
                setResult(Activity.RESULT_OK)
                finish()
            }, Response.ErrorListener {
                loading.visibility = View.GONE
                val internetError = getString(R.string.internet_error)
                val tryAgainLater = getString(R.string.try_again_later)
                Toast.makeText(
                    applicationContext,
                    "$internetError\n$tryAgainLater",
                    Toast.LENGTH_LONG
                ).show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        requestQueue.add(jsonObjectRequest)
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

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
