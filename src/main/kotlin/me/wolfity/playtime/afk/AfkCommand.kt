package me.wolfity.playtime.afk

import me.wolfity.developmentutil.ext.uuid
import me.wolfity.developmentutil.util.TimeFormatConfig
import me.wolfity.developmentutil.util.TimeUnitFormat
import me.wolfity.developmentutil.util.formatTime
import me.wolfity.developmentutil.util.sendStyled
import me.wolfity.playtime.plugin
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.bukkit.annotation.CommandPermission

class AfkCommand {

    @Command("afkcheck")
    @CommandPermission("playtime.view")
    fun onAfkCheck(sender: Player, target: Player) {
        val afkTime = plugin.afkDetector.getCurrentAfkDuration(target.uuid)
        sender.sendStyled("<yellow>${target.name} has been AFK for <gold>${formatTime(afkTime, TimeFormatConfig(listOf(TimeUnitFormat.DAYS, TimeUnitFormat.HOURS, TimeUnitFormat.MINUTES, TimeUnitFormat.SECONDS)))}")
    }
}