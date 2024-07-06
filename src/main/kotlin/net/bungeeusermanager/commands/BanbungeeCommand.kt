package net.bungeeusermanager.commands

import net.bungeeusermanager.BungeeUserManager
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor

class BanbungeeCommand(private val plugin: BungeeUserManager) : Command("banbungee"), TabExecutor {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(TextComponent("Usage: /banbungee <username> <reason>"))
            return
        }
        val username = args[0]
        val reason = args.slice(1 until args.size).joinToString(" ")

        // Logic to ban user and store in Pocketbase

        sender.sendMessage(TextComponent("$username has been banned for: $reason"))
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1) {
            return plugin.proxy.players.map { it.name }.filter {
                it.startsWith(args[0], ignoreCase = true)
            }
        }
        return emptyList()
    }
}
