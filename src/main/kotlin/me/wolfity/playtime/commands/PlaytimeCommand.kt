package me.wolfity.playtime.commands

import me.wolfity.developmentutil.util.TimeFormatConfig
import me.wolfity.developmentutil.util.TimeUnitFormat
import me.wolfity.developmentutil.util.formatTime
import me.wolfity.developmentutil.util.launchAsync
import me.wolfity.developmentutil.util.sendStyled
import me.wolfity.developmentutil.util.style
import me.wolfity.playtime.plugin
import me.wolfity.playtime.util.formatPlaytimeMessage
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Named
import revxrsal.commands.annotation.Optional
import revxrsal.commands.bukkit.annotation.CommandPermission

class PlaytimeCommand {


    @Command("playtime", "pt")
    @CommandPermission("playtime.view")
    fun onPlaytime(
        sender: Player,
        @Named("player") @Optional target: UserCommandParameter?
    ) {
        launchAsync {
            if (target == null || target.name.equals(sender.name, true)) {
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
}