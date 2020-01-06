package com.example.androidclient.data

import com.example.androidclient.data.model.LoggedInUser
import java.io.IOException
import java.security.MessageDigest
import javax.security.auth.login.LoginException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    fun login(username: String, password: String): Result<LoggedInUser> {
        try {
            if (username == "Jack") {
                val hashedPassword = hashPassword(password)
                if (hashedPassword == "85f293f02afec08cc90ec9b9501ff532c8c46c094850516700b5e8bd95bb570c") {
                    val loggedUser =
                        LoggedInUser(java.util.UUID.randomUUID().toString(), username, hashedPassword)
                    return Result.Success(loggedUser)
                } else {
                    throw LoginException("Bad password")
                }
            } else {
                throw LoginException("Bad login")
            }
        } catch (e: LoginException) {
            return Result.Error(IOException("Error logging in", e))
        }
    }

    private fun hashPassword(password: String) : String{
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray())
        return bytes.fold("", { str, it -> str + "%02x".format(it) })
    }
}

