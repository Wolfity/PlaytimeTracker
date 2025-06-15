package me.wolfity.playtime.afk

import me.wolfity.playtime.plugin
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class AFKDetector(private val afkMarkMillis: Long) : Listener {

    private val lastMovement = ConcurrentHashMap<UUID, Long>()
    private val afkStartTime = ConcurrentHashMap<UUID, Long>()
    private val totalAfkTime = ConcurrentHashMap<UUID, Long>()

    init {
        object : BukkitRunnable() {
            override fun run() {
                checkAFKStatus()
            }
        }.runTaskTimerAsynchronously(plugin, 20, 20)
    }

    fun isAFK(uuid: UUID): Boolean {
        return afkStartTime.containsKey(uuid)
    }

    /**
     * Returns total AFK time in **seconds**
     */
    fun getTotalAfkTime(uuid: UUID): Long {
        val start = afkStartTime[uuid]
        val previousMillis = totalAfkTime[uuid] ?: 0L
        val totalMillis = if (start != null) {
            previousMillis + (System.currentTimeMillis() - start)
        } else {
            previousMillis
        }
        return totalMillis / 1000
    }

    fun getCurrentAfkDuration(uuid: UUID): Long {
        val start = afkStartTime[uuid] ?: return 0L
        val now = System.currentTimeMillis()
        return (now - start) / 1000
    }

    fun resetAFK(uuid: UUID) {
        afkStartTime.remove(uuid)
        totalAfkTime.remove(uuid)
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        val uuid = player.uniqueId
        if (!event.hasExplicitlyChangedBlock()) return
        trackAfk(uuid)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val uuid = player.uniqueId
        trackAfk(uuid)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        lastMovement.remove(uuid)
        afkStartTime.remove(uuid)
        totalAfkTime.remove(uuid)
    }

    private fun checkAFKStatus() {
        val now = System.currentTimeMillis()
        for (uuid in lastMovement.keys) {
            val lastActive = lastMovement[uuid] ?: continue
            if (!afkStartTime.containsKey(uuid) && now - lastActive >= afkMarkMillis) {
                afkStartTime[uuid] = now
            }
        }
    }

    private fun trackAfk(uuid: UUID) {
        lastMovement[uuid] = System.currentTimeMillis()

        afkStartTime.remove(uuid)?.let { afkStart ->
            val duration = System.currentTimeMillis() - afkStart
            totalAfkTime[uuid] = (totalAfkTime[uuid] ?: 0L) + duration
        }
    }
}
