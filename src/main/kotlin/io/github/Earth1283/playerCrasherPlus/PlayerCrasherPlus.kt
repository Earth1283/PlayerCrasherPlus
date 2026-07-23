package io.github.earth1283.playerCrasherPlus

import org.bukkit.plugin.java.JavaPlugin

class PlayerCrasherPlus : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()
        getCommand("crash")?.setExecutor(CrashCommand(this))
    }

    override fun onDisable() {
    }
}
