package me.wolfity.playtime.tasks

import me.wolfity.developmentutil.util.launchAsync
import me.wolfity.playtime.plugin
import me.wolfity.playtime.tracking.PlaytimeManager
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class PlaytimeUpdateTask(private val playtimeManager: PlaytimeManager) : BukkitRunnable() {

    override fun run() {
        plugin.logger.info("[PlayTimeTracker] Executed auto save task")
        launchAsync {
            Bukkit.getOnlinePlayers().forEach { p -> playtimeManager.updatePlaytimeForPlayer(p.uniqueId) }
        }
    }
}