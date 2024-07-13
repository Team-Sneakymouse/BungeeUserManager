package net.bungeeusermanager

import net.bungeeusermanager.commands.CommandBan
import net.bungeeusermanager.commands.CommandUnban
import net.bungeeusermanager.util.PocketbaseManager
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler

class BungeeUserManager : Plugin() {
    private lateinit var pocketBaseManager: PocketbaseManager
    private lateinit var config: Config

    override fun onEnable() {
        instance = this

        pocketBaseManager = PocketbaseManager()
        config = Config(this)

        // Register commands
        proxy.pluginManager.registerCommand(this, CommandBan(this))
        proxy.pluginManager.registerCommand(this, CommandUnban(this))
        proxy.pluginManager.registerListener(this, ConnectionListener())
        log("BungeeUserManager has been enabled")

        config.reloadConfigAsync()
    }

    override fun onDisable() {
        pocketBaseManager.saveQueueToFile()
        log("BungeeUserManager has been disabled")
    }

    fun getConfig(): Config {
        return config
    }

    companion object {
        private lateinit var instance: BungeeUserManager
            private set

        /** Logs a message using the plugin logger. */
        fun log(msg: String) {
            instance?.logger?.info(msg) ?: System.err.println("BungeeUserManager instance is null")
        }

        /** The running instance. */
        fun getInstance(): BungeeUserManager {
            return instance
        }

        /** Retrieves the job manager instance, creating a new one if necessary. */
        fun getPocketbaseManager(): PocketbaseManager {
            return instance?.pocketBaseManager
                    ?: PocketbaseManager().also { instance?.pocketBaseManager = it }
        }
    }
}

class ConnectionListener : Listener {
    @EventHandler
    fun onServerConnect(event: ServerConnectEvent) {
        BungeeUserManager.getPocketbaseManager().queueNewConnection(event.player)
        // event.setCancelled(true)
        // event.player.disconnect(TextComponent("Test Message"))
    }
}
