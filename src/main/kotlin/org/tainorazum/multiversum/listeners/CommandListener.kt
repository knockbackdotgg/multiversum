package org.tainorazum.multiversum.listeners

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.command.CommandExecuteEvent
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent
import com.velocitypowered.api.event.player.TabCompleteEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import jakarta.inject.Inject
import net.kyori.adventure.text.Component
import org.tainorazum.multiversum.utility.Colours

class CommandListener @Inject constructor(
    private val server: ProxyServer
) {

    private fun hasCommandPermission(player: Player, commandName: String): Boolean {
        return player.hasPermission("command.*") || player.hasPermission("command.$commandName")
    }

    @Subscribe
    fun onCommandExecute(event: CommandExecuteEvent) {
        val player = event.commandSource as? Player ?: return

        val rootCommand = event.command.trimStart('/').substringBefore(' ').lowercase()

        if (!hasCommandPermission(player, rootCommand)) {
            event.result = CommandExecuteEvent.CommandResult.denied()

            player.sendMessage(
                Component.text("You are not allowed to execute this command.", Colours.RED)
            )
        }
    }

    @Subscribe
    fun onPlayerAvailableCommands(event: PlayerAvailableCommandsEvent) {
        val player = event.player

        // Full bypass if player has command.*
        if (player.hasPermission("command.*")) return

        event.rootNode.children.removeIf { node ->
            val commandName = node.name.lowercase()
            !hasCommandPermission(player, commandName)
        }
    }

    @Subscribe
    fun onTabComplete(event: TabCompleteEvent) {
        val player: Player = event.player
        val rawInput = event.partialMessage.trimStart('/')

        if (rawInput.contains(" ")) {
            val rootCommand = rawInput.substringBefore(' ').lowercase()
            if (!hasCommandPermission(player, rootCommand)) {
                event.suggestions.clear()
            }
        }
    }
}