package net.bungeeusermanager

import net.bungeeusermanager.commands.BanbungeeCommand
import net.bungeeusermanager.commands.UnbanbungeeCommand
import net.md_5.bungee.api.plugin.Plugin

class BungeeUserManager : Plugin() {
    override fun onEnable() {
        // Register commands
        proxy.pluginManager.registerCommand(this, BanbungeeCommand(this))
        proxy.pluginManager.registerCommand(this, UnbanbungeeCommand(this))
        logger.info("BungeeUserManager has been enabled")
    }

    override fun onDisable() {
        logger.info("BungeeUserManager has been disabled")
    }
}
