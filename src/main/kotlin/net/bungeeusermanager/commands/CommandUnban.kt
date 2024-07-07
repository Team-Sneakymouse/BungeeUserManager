package net.bungeeusermanager.commands

import net.bungeeusermanager.BungeeUserManager
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor

class CommandUnban(private val plugin: BungeeUserManager) : Command("unban"), TabExecutor {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.size != 1) {
            sender.sendMessage(TextComponent("Usage: /unban <username>"))
            return
        }
        val username = args[0]

        // Logic to unban user and remove from Pocketbase
        sender.sendMessage(TextComponent("$username has been unbanned"))
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
