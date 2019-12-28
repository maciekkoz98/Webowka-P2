package com.example.androidclient.data

import com.example.androidclient.data.model.LoggedInUser
import java.io.IOException
import javax.security.auth.login.LoginException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    fun login(username: String, password: String): Result<LoggedInUser> {
        try {
            // TODO: handle loggedInUser authentication
            if (username == "Jack") {
                if (password == "gwiazdor") {
                    val loggedUser =
                        LoggedInUser(java.util.UUID.randomUUID().toString(), username)
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

    fun logout() {
        // TODO: revoke authentication
    }
}

