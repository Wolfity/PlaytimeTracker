package me.wolfity.playtime.commands

import me.wolfity.developmentutil.util.TimeFormatConfig
import me.wolfity.developmentutil.util.TimeUnitFormat
import me.wolfity.developmentutil.util.formatTime
import me.wolfity.developmentutil.util.launchAsync
import me.wolfity.developmentutil.util.sendStyled
import me.wolfity.developmentutil.util.style
import me.wolfity.playtime.plugin
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Named
import revxrsal.commands.annotation.Optional
import revxrsal.commands.bukkit.annotation.CommandPermission

class PlaytimeCommand {

    @Command("playtime leaderboard", "pt leaderboard", "ptlb", "pt lb", "playtimelb")
    @CommandPermission("playtime.view")
    fun onLeaderboard(sender: Player) {
        launchAsync {
            val topPlayers = plugin.playtimeManager.loadTopPlaytime(10)
            if (topPlayers.isEmpty()) {
                sender.sendStyled("<red>No playtime data found.")
                return@launchAsync
            }

            val header = plugin.config.getString("playtime-leaderboard-header")!!
            val footer = plugin.config.getString("playtime-leaderboard-footer")!!
            val entryFormat = plugin.config.getString("playtime-leaderboard-entry")!!

            sender.sendStyled(header)
            val body = topPlayers.mapIndexed { index, (uuid, playtimeSeconds) ->
                val name = plugin.playerManager.getDataByUUID(uuid)?.name ?: "Unknown"
                val formattedTime = formatPlaytime(playtimeSeconds)
                entryFormat
                    .replace("{position}", (index + 1).toString())
                    .replace("{player}", name)
                    .replace("{playtime}", formattedTime)
            }
            sender.sendStyled(style(body, JoinConfiguration.newlines()))
            sender.sendStyled(footer)
        }
    }


    @Command("playtime", "pt")
    @CommandPermission("playtime.view")
    fun onPlaytime(
        sender: Player,
        @Named("player") @Optional target: UserCommandParameter?
    ) {
        launchAsync {
            if (target == null || target.name == sender.name) {
                val playTime = plugin.playtimeManager.getTotalPlaytime(sender.uniqueId)
                sender.sendStyled(formatPlaytimeMessage(sender.name, playTime))
            } else {
                val targetUser = plugin.playerManager.getDataByName(target.name)
                if (targetUser == null) {
                    sender.sendStyled("<red>This player does not exist!")
                    return@launchAsync
                }

                val targetPlaytime = plugin.playtimeManager.getTotalPlaytime(targetUser.uuid)
                sender.sendStyled(formatPlaytimeMessage(targetUser.name, targetPlaytime))
            }
        }
    }

    private fun formatPlaytime(time: Long): String {
        return formatTime(
            time, TimeFormatConfig(
                listOf(
                    TimeUnitFormat.DAYS,
                    TimeUnitFormat.HOURS,
                    TimeUnitFormat.MINUTES,
                )
            )
        )
    }

    private fun formatPlaytimeMessage(name: String, time: Long): String {
        val formattedTime = formatPlaytime(time)
        return plugin.config.getString("playtime-message")!!
            .replace("{player}", name)
            .replace("{playtime}", formattedTime)
    }
}