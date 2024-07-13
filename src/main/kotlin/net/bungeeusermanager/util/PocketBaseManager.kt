package net.bungeeusermanager.util

import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import net.bungeeusermanager.BungeeUserManager
import net.md_5.bungee.api.connection.ProxiedPlayer
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class PocketbaseManager {
    var authToken: String = ""
    var spoofingMap: MutableMap<String, String>? = null
    private val connectionQueue = ConcurrentLinkedQueue<String>()
    private val executor = Executors.newSingleThreadScheduledExecutor()

    init {
        // Load persisted queue if exists
        loadQueueFromFile()
        // Schedule queue processing
        executor.scheduleAtFixedRate({ processQueue() }, 0, 1, TimeUnit.MINUTES)
    }

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
            val authRequest = Request.Builder().url(url).post(authRequestBody).build()
            val authResponse = client.newCall(authRequest).execute()

            if (authResponse.isSuccessful) {
                val responseBody = authResponse.body?.string()
                val jsonResponse = JSONObject(responseBody)

                authToken = jsonResponse.optString("token", "")

                if (authToken.isEmpty()) {
                    BungeeUserManager.log(
                            "Pocketbase authentication was successful but there was no token in the response."
                    )
                } else {
                    BungeeUserManager.log("Pocketbase authentication was successful.")
                }
                authResponse.close()
            } else {
                BungeeUserManager.log("Pocketbase authentication failed: ${authResponse.code}")
            }
        } catch (e: Exception) {
            BungeeUserManager.log("Error occurred: ${e.message}")
        }
    }

    fun queueNewConnection(player: ProxiedPlayer) {
        queueNewConnection(
                player.uniqueId.toString(),
                player.socketAddress.toString().substringAfter("/").substringBefore(":")
        )
    }

    fun queueNewConnection(uuid: String, socketAddress_: String) {
        val socketAddress =
                spoofSocketAddress(
                        uuid,
                        socketAddress_.toString().substringAfter("/").substringBefore(":")
                )
        connectionQueue.add("$uuid|$socketAddress")
        saveQueueToFile()
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

    private fun processQueue() {
        if (connectionQueue.isEmpty()) return

        val connections = mutableListOf<String>()
        while (connectionQueue.isNotEmpty()) {
            connectionQueue.poll()?.let { connections.add(it) }
        }

        if (connections.isNotEmpty()) {
            sendConnectionsToPocketBase(connections)
        }
    }

    private fun sendConnectionsToPocketBase(connections: List<String>) {
        val url =
                BungeeUserManager.getInstance().getConfig().getString("pocketbase-connections-url")
        if (url.isNullOrEmpty()) return

        try {
            if (authToken.isEmpty()) auth()

            if (authToken.isNotEmpty()) {
                val client = OkHttpClient()
                val failedConnections = mutableListOf<String>()

                connections.forEach { connection ->
                    val (uuid, socketAddress) = connection.split("|")

                    val connectionData =
                            mapOf("user_uuid" to uuid, "socket_address" to socketAddress)

                    val requestBody =
                            Gson().toJson(connectionData)
                                    .toRequestBody("application/json".toMediaType())

                    val request =
                            Request.Builder()
                                    .url(url)
                                    .header("Authorization", "Bearer $authToken")
                                    .post(requestBody)
                                    .build()

                    try {
                        val response = client.newCall(request).execute()

                        if (!response.isSuccessful) {
                            BungeeUserManager.log(
                                    "Failed to send connection to PocketBase: ${response.code} - ${response.body?.string()}"
                            )
                            failedConnections.add(connection)
                        }
                        response.close()
                    } catch (e: Exception) {
                        BungeeUserManager.log(
                                "Error occurred while sending connection to PocketBase: ${e.message}"
                        )
                        failedConnections.add(connection)
                    }
                }

                // Re-add failed connections to the queue
                if (failedConnections.isNotEmpty()) {
                    connectionQueue.addAll(failedConnections)
                }
            }
        } catch (e: Exception) {
            BungeeUserManager.log("Error occurred while processing connections: ${e.message}")
            // Re-add all connections to the queue in case of a critical failure
            connectionQueue.addAll(connections)
        }
    }

    fun saveQueueToFile() {
        CompletableFuture.runAsync {
            try {
                val file = File(BungeeUserManager.getInstance().dataFolder, "connectionQueue.txt")
                file.writeText(connectionQueue.joinToString("\n"))
            } catch (e: IOException) {
                BungeeUserManager.log("Failed to save connection queue: ${e.message}")
            }
        }
    }

    private fun loadQueueFromFile() {
        CompletableFuture.runAsync {
            try {
                val file = File(BungeeUserManager.getInstance().dataFolder, "connectionQueue.txt")
                if (file.exists()) {
                    file.readLines().forEach { connectionQueue.add(it) }
                    file.delete() // Delete the file after reading its contents
                }
            } catch (e: IOException) {
                BungeeUserManager.log("Failed to load connection queue: ${e.message}")
            }
        }
    }
}
