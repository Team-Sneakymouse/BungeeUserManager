package net.bungeeusermanager.util

import net.bungeeusermanager.BungeeUserManager
import net.md_5.bungee.api.connection.ProxiedPlayer
import okhttp3.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class PocketbaseManager {
    var authToken: String = ""
    var spoofingMap: MutableMap<String, String>? = null

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
            val uuid = it.uniqueId.toString()
            val socketAddress =
                    spoofSocketAddress(
                            uuid,
                            it.socketAddress.toString().substringAfter("/").substringBefore(":")
                    )
        }
    }

    fun spoofSocketAddress(uuid: String, socketAddress: String): String {
        if (spoofingMap == null) {
            val map = mutableMapOf<String, String>()
            val regex =
                    Regex(
                            "^(?:(?:(?:25[0-5]|2[0-4][0-9]|1?[0-9]{1,2})\\.){3}(?:25[0-5]|2[0-4][0-9]|1?[0-9]{1,2})|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12})\\|(?:(?:25[0-5]|2[0-4][0-9]|1?[0-9]{1,2})\\.){3}(?:25[0-5]|2[0-4][0-9]|1?[0-9]{1,2})$"
                    )

            BungeeUserManager.getInstance().getConfig().getStringList("ip-spoofing").forEach {
                if (!regex.matches(it)) {
                    BungeeUserManager.log(
                            "There is an improperly formatted string in the ip-spoofing config. Please ensure that it is formatted as 'UUID|ip' or 'ip|ip', including dots and dashes: '$it'"
                    )
                } else {
                    val split = it.split("|")
                    map[split[0]] = split[1]
                }
            }
            spoofingMap = map
        }

        val map = spoofingMap ?: return socketAddress

        return map[uuid] ?: map[socketAddress] ?: socketAddress
    }
}
