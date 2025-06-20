package me.wolfity.playtime.rewards

import me.wolfity.developmentutil.util.TimeFormatConfig
import me.wolfity.developmentutil.util.formatTime
import me.wolfity.developmentutil.util.launchAsync
import me.wolfity.developmentutil.util.sendStyled
import me.wolfity.playtime.db.PlaytimeRewardLog
import me.wolfity.playtime.plugin
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import org.jetbrains.exposed.sql.*

class RewardManager(
    private val rewardLoader: RewardLoader
) {

    private val CHECK_INTERVAL: Long = 20L

    init {
        start()
    }

    private fun start() {
        object : BukkitRunnable() {
            override fun run() {
                launchAsync {
                    checkAndRewardPlayers()
                }
            }
        }.runTaskTimerAsynchronously(plugin, CHECK_INTERVAL, CHECK_INTERVAL)
    }

    private suspend fun checkAndRewardPlayers() {
        val rewards = rewardLoader.playtimeRewards
        Bukkit.getOnlinePlayers().forEach { player ->
            val uuid = player.uniqueId
            val playtime = plugin.playtimeManager.getTotalPlaytime(player.uniqueId)

            val pendingRewards = rewards
                .filterKeys { it <= playtime }
                .filterNot { (seconds, _) -> hasReceivedReward(uuid, seconds) }
            for ((seconds, commandTemplate) in pendingRewards) {
                val command = commandTemplate
                    .replace("{player}", player.name)
                object : BukkitRunnable() {
                    override fun run() {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                    }
                }.runTask(plugin)

                player.sendStyled(plugin.config.getString("reward-received-message")!!
                    .replace("{playtime}", formatTime(seconds.toLong(), (TimeFormatConfig()))))
                markRewardGiven(uuid, seconds)
            }
        }
    }


    private suspend fun hasReceivedReward(player: UUID, rewardSeconds: Int): Boolean {
        return newSuspendedTransaction {
            PlaytimeRewardLog
                .selectAll()
                .where { (PlaytimeRewardLog.uuid eq player) and (PlaytimeRewardLog.rewardTimeSeconds eq rewardSeconds) }
                .count() > 0
        }
    }

    private suspend fun markRewardGiven(uuid: UUID, rewardSeconds: Int) {
        newSuspendedTransaction {
            PlaytimeRewardLog.insertIgnore {
                it[PlaytimeRewardLog.uuid] = uuid
                it[PlaytimeRewardLog.rewardTimeSeconds] = rewardSeconds
                it[PlaytimeRewardLog.receivedAt] = System.currentTimeMillis()
            }
        }
    }

}