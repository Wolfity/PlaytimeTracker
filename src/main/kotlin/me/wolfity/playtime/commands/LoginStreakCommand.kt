package me.wolfity.playtime.commands

import me.wolfity.developmentutil.ext.uuid
import me.wolfity.developmentutil.util.launchAsync
import me.wolfity.developmentutil.util.sendStyled
import me.wolfity.developmentutil.util.style
import me.wolfity.playtime.plugin
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Named
import revxrsal.commands.annotation.Optional
import kotlin.math.log

class LoginStreakCommand {

    @Command("loginstreak")
    fun onStreak(sender: Player, @Optional @Named("player") target: UserCommandParameter?) {
        val header = plugin.config.getString("login-streak-info-header")!!
        val body = plugin.config.getStringList("login-streak-body")
        launchAsync {
            val actualTarget = target?.let { plugin.playerManager.getDataByName(it.name) }

            if (target != null && actualTarget == null) {
                sender.sendStyled("<red>This player does not exist!")
                return@launchAsync
            }

            val targetName = actualTarget?.name ?: sender.name
            val targetUuid = actualTarget?.uuid ?: sender.uuid

            val loginStreakData = plugin.playtimeManager.getLoginStreakData(targetUuid)
            val currentStreak = loginStreakData?.currentStreak ?: 0
            val maxStreak = loginStreakData?.longestStreak ?: 0

            val header = header.replace("{player}", targetName)
            val styledBody = body.map {
                it.replace("{currentStreak}", currentStreak.toString())
                    .replace("{maxStreak}", maxStreak.toString())
            }

            sender.sendStyled(header)
            sender.sendStyled(style(styledBody, JoinConfiguration.newlines()))
        }
    }

}