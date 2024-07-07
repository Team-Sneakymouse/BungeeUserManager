package net.bungeeusermanager

import net.bungeeusermanager.commands.CommandBan
import net.bungeeusermanager.commands.CommandUnban
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler

class BungeeUserManager : Plugin() {
    override fun onEnable() {
        // Register commands
        proxy.pluginManager.registerCommand(this, CommandBan(this))
        proxy.pluginManager.registerCommand(this, CommandUnban(this))
        proxy.pluginManager.registerListener(this, ConnectionListener())
        logger.info("BungeeUserManager has been enabled")
    }

    override fun onDisable() {
        logger.info("BungeeUserManager has been disabled")
    }
}

class ConnectionListener : Listener {
    @EventHandler
    fun onServerConnect(event: ServerConnectEvent) {
        println(event.player.uniqueId)
        //event.setCancelled(true)
        //event.player.disconnect(TextComponent("Test Message"))
    }
}
