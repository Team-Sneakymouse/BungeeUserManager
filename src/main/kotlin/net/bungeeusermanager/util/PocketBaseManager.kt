package net.bungeeusermanager.util

import net.bungeeusermanager.BungeeUserManager
import net.md_5.bungee.api.connection.ProxiedPlayer
import okhttp3.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class PocketbaseManager {
    var authToken: String = ""

    /** Get a PocketBase auth token. Only run this asynchronously! */
    @Synchronized
    fun auth() {
        val url = BungeeUserManager.getInstance().getConfig().getString("pocketbase-auth-url")
        val email = BungeeUserManager.getInstance().getConfig().getString("pocketbase-email")
        val password = BungeeUserManager.getInstance().getConfig().getString("pocketbase-password")

        if (url.isNullOrEmpty() || email == null || password == null) return

        try {
            val client = OkHttpClient()
            val authRequestBody =
                    FormBody.Builder().add("identity", email).add("password", password).build()
            val authRequest = Request.Builder().url("$url").post(authRequestBody).build()
            val authResponse = client.newCall(authRequest).execute()

            if (authResponse.isSuccessful) {
                val responseBody = authResponse.body?.string()
                val jsonResponse = JSONObject(responseBody)

                authToken = jsonResponse.optString("token", "")

                if (authToken.isEmpty()) {
                    BungeeUserManager.log(
                            "Pocketbase authentication was succesfull but there was no token in the response."
                    )
                } else {
                    BungeeUserManager.log("Pocketbase authentication was successfull.")
                }
                authResponse.close()
            } else {
                BungeeUserManager.log("Pocketbase authentication failed: ${authResponse.code}")
            }
        } catch (e: Exception) {
            BungeeUserManager.log("Error occurred: ${e.message}")
        }
    }

    @Synchronized
    fun addConnection(players: List<ProxiedPlayer>) {
        players.forEach {
            val uuid = it.uniqueId
            val socketAddress = it.socketAddress.toString().substringAfter("/").substringBefore(":")
        }
    }
}
