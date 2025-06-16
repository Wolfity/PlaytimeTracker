package me.wolfity.playtime.commands

import me.wolfity.developmentutil.util.launchAsync
import me.wolfity.developmentutil.util.sendStyled
import me.wolfity.developmentutil.util.style
import me.wolfity.playtime.plugin
import me.wolfity.playtime.util.formatPlaytime
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.bukkit.annotation.CommandPermission

class LeaderboardCommands {

    @Command("leaderboard", "lb")
    fun onLeaderboard(
        sender: Player,
        leaderboardType: LeaderboardType
    ) {
        launchAsync {
            displayLeaderboard(sender, leaderboardType)
        }
    }

    // Old leaderboard command, now that there are multiple leaderboards, we will still support this one
    // To not have current downloaders confused, but we will move to /leaderboard <type>
    @Command("playtime leaderboard", "pt leaderboard", "ptlb", "pt lb", "playtimelb")
    @CommandPermission("playtime.view")
    fun onLeaderboard(sender: Player) {
        launchAsync {
            displayLeaderboard(sender, LeaderboardType.PLAYTIME)
        }
    }

    private suspend fun displayLeaderboard(
        sender: Player,
        leaderboardType: LeaderboardType,
    )  {
        val topPlayers = plugin.playtimeManager.loadLeaderboard(10, leaderboardType)
        if (topPlayers.isEmpty()) {
            sender.sendStyled("<red>No leaderboard data found.")
            return
        }

        val format = leaderboardType.getFormat()

        sender.sendStyled(format.header)

        val body = topPlayers.mapIndexed { index, (uuid, value) ->
            val name = plugin.playerManager.getDataByUUID(uuid)?.name ?: "Unknown"
            format.entry
                .replace("{position}", (index + 1).toString())
                .replace("{player}", name)
                .replace("{playtime}", format.valueFormatter(value)) // fallback
                .replace("{currentLoginStreak}", format.valueFormatter(value))
                .replace("{maxLoginStreak}", format.valueFormatter(value))
        }

        sender.sendStyled(style(body, JoinConfiguration.newlines()))

        if (format.footer.isNotEmpty()) {
            sender.sendStyled(format.footer)
        }
    }

    data class LeaderboardFormat(
        val header: String,
        val entry: String,
        val footer: String,
        val valueFormatter: (Long) -> String
    )

    private fun LeaderboardType.getFormat(): LeaderboardFormat {
        return when (this) {
            LeaderboardType.PLAYTIME -> LeaderboardFormat(
                header = plugin.config.getString("playtime-leaderboard-header") ?: "",
                entry = plugin.config.getString("playtime-leaderboard-entry") ?: "",
                footer = plugin.config.getString("playtime-leaderboard-footer") ?: "",
                valueFormatter = { formatPlaytime(it) }
            )
            LeaderboardType.CURRENT_LOGIN_STREAK -> LeaderboardFormat(
                header = plugin.config.getString("login-streak-leaderboard-header") ?: "",
                entry = plugin.config.getString("login-streak-leaderboard-entry") ?: "",
                footer = plugin.config.getString("login-streak-leaderboard-footer") ?: "",
                valueFormatter = { "$it days" }
            )
            LeaderboardType.MAX_LOGIN_STREAK -> LeaderboardFormat(
                header = plugin.config.getString("max-login-streak-leaderboard-header") ?: "",
                entry = plugin.config.getString("max-login-streak-leaderboard-entry") ?: "",
                footer = plugin.config.getString("max-login-streak-leaderboard-footer") ?: "",
                valueFormatter = { "$it days" }
            )
        }
    }

}