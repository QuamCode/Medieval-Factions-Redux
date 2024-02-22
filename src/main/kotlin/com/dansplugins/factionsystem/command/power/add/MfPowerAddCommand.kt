package com.dansplugins.factionsystem.command.power.add

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.logging.Level

class MfPowerAddCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val decimalFormat = DecimalFormat("0.##", DecimalFormatSymbols.getInstance(plugin.language.locale))

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.power.add")) {
            sender.sendMessage("$RED${plugin.language["CommandPowerAddNoPermission"]}")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandPowerAddUsage"]}")
            return true
        }
        val target = plugin.server.getOfflinePlayer(args[0])
        if (!target.isOnline && !target.hasPlayedBefore()) {
            sender.sendMessage("$RED${plugin.language["CommandPowerAddInvalidTarget"]}")
            return true
        }
        val powerToAdd = args[1].toDoubleOrNull()
        if (powerToAdd == null) {
            sender.sendMessage("$RED${plugin.language["CommandPowerAddInvalidPower"]}")
            return true
        }

        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val targetMfPlayer = playerService.getPlayer(target)
                ?: return@Runnable sender.sendMessage("$RED${plugin.language["CommandPowerAddFailedToFindPlayer"]}")

            val newPower = (targetMfPlayer.power + powerToAdd).coerceAtMost(plugin.config.getDouble("players.maxPower"))
            playerService.save(targetMfPlayer.copy(power = newPower)).onFailure {
                sender.sendMessage("$RED${plugin.language["CommandPowerAddFailedToSaveTargetPlayer"]}")
                plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }

            sender.sendMessage(
                "$GREEN${
                    plugin.language["CommandPowerAddSuccess", target.name ?: plugin.language["UnknownPlayer"], decimalFormat.format(
                        powerToAdd
                    ), decimalFormat.format(newPower)]
                }"
            )
        })

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = when {
        args.isEmpty() -> plugin.server.offlinePlayers.mapNotNull { it.name }
        args.size == 1 -> plugin.server.offlinePlayers.filter {
            it.name?.lowercase()?.startsWith(args[0].lowercase()) == true
        }.mapNotNull { it.name }

        else -> emptyList()
    }
}