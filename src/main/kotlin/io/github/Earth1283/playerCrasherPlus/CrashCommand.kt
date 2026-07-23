package io.github.earth1283.playerCrasherPlus

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class CrashCommand(private val plugin: JavaPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("playercrasherplus.crash")) {
            sender.sendMessage(msg("no-permission"))
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage(msg("usage"))
            return true
        }

        val target = Bukkit.getPlayerExact(args[0])
        if (target == null) {
            sender.sendMessage(msg("player-not-found").replace("%player%", args[0]))
            return true
        }

        val sizeMb: Double
        if (args.size >= 2) {
            val parsed = args[1].toDoubleOrNull()
            if (parsed == null || parsed <= 0) {
                sender.sendMessage(msg("invalid-size"))
                return true
            }
            sizeMb = parsed
        } else {
            sizeMb = plugin.config.getDouble("default-size-mb", 5.0)
        }

        val totalBytes = (sizeMb * 1024 * 1024).toLong()
        val targetName = target.name

        plugin.logger.info("${sender.name} used /crash on $targetName (${sizeMb}MB)")
        sender.sendMessage(msg("sent").replace("%target%", targetName).replace("%size%", sizeMb.toString()))

        PacketInjector.blast(plugin, target, totalBytes)

        val delayTicks = plugin.config.getLong("fallback.delay-ticks", 20)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val stillOnline = Bukkit.getPlayerExact(targetName)
            if (stillOnline == null || !stillOnline.isOnline) {
                sender.sendMessage(msg("result-crashed").replace("%target%", targetName))
                return@Runnable
            }

            val ghostEnabled = plugin.config.getBoolean("ghost.enabled", true)
            if (ghostEnabled && plugin.config.getBoolean("ghost.prefer-first", false)) {
                GhostHandler.ghost(plugin, stillOnline)
                sender.sendMessage(msg("result-ghosted").replace("%target%", targetName))
                return@Runnable
            }

            @Suppress("DEPRECATION")
            stillOnline.kickPlayer(msg("fallback.kick-message"))
            sender.sendMessage(msg("result-fallback").replace("%target%", targetName))

            if (ghostEnabled) {
                scheduleGhostCheck(targetName, sender)
            }
        }, delayTicks)

        return true
    }

    private fun scheduleGhostCheck(targetName: String, sender: CommandSender) {
        val ghostDelay = plugin.config.getLong("ghost.delay-ticks", 20)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val stillHere = Bukkit.getPlayerExact(targetName)
            if (stillHere != null && stillHere.isOnline) {
                GhostHandler.ghost(plugin, stillHere)
                sender.sendMessage(msg("result-ghosted").replace("%target%", targetName))
            }
        }, ghostDelay)
    }

    private fun msg(path: String): String {
        val raw = plugin.config.getString(if (path.contains('.')) path else "messages.$path") ?: ""
        return ChatColor.translateAlternateColorCodes('&', raw)
    }
}
