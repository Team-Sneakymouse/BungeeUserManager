package net.bungeeusermanager

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration

class Config(private val instance: BungeeUserManager) {

    private lateinit var configFile: File
    private lateinit var configuration: Configuration
    private val cachedValues: MutableMap<String, Any?> = mutableMapOf()

    fun reloadConfigAsync(): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            try {
                configFile = File(instance.dataFolder, "config.yml")

                if (!configFile.exists()) {
                    BungeeUserManager.log("config.yml not found, creating default...")
                    instance.dataFolder.mkdirs()
                    saveResourceAsync("config.yml", false).join()
                }

                configuration =
                        ConfigurationProvider.getProvider(YamlConfiguration::class.java)
                                .load(configFile)
                cacheConfigValues()
                BungeeUserManager.log("Config.yml loaded successfully.")
                BungeeUserManager.getPocketbaseManager().auth()
            } catch (ex: IOException) {
                BungeeUserManager.log("Failed to load config.yml: ${ex.message}")
            }
        }
    }

    private fun saveResourceAsync(resourcePath: String, replace: Boolean): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val resourceStream: java.io.InputStream? = instance.getResourceAsStream(resourcePath)
            if (resourceStream == null) {
                BungeeUserManager.log("Resource '$resourcePath' not found in plugin JAR.")
                return@runAsync
            }

            val outFile = File(instance.dataFolder, resourcePath)
            if (outFile.exists() && !replace) {
                BungeeUserManager.log("File '$resourcePath' already exists, skipping save.")
                return@runAsync
            }

            try {
                java.nio.file.Files.copy(
                        resourceStream,
                        outFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
                BungeeUserManager.log("Saved default resource: $resourcePath")
            } catch (ex: IOException) {
                BungeeUserManager.log("Failed to save resource: $resourcePath")
            }
        }
    }

    private fun cacheConfigValues() {
        cachedValues.clear()
        for (key in configuration.getKeys()) {
            val value = configuration.get(key)
            cachedValues[key] = value
        }
    }

    fun getString(key: String): String? {
        val value = cachedValues[key]
        return if (value is String) value else null
    }

    fun getStringList(key: String): List<String> {
        val value = cachedValues[key]
        return if (value is List<*> && value.all { it is String }) value as List<String>
        else emptyList()
    }
}
