package me.wolfity.playtime.util

import me.wolfity.developmentutil.util.TimeFormatConfig
import me.wolfity.developmentutil.util.TimeUnitFormat
import me.wolfity.developmentutil.util.formatTime
import me.wolfity.playtime.plugin

fun formatPlaytime(time: Long): String {
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

fun formatPlaytimeMessage(name: String, time: Long): String {
    val formattedTime = formatPlaytime(time)
    return plugin.config.getString("playtime-message")!!
        .replace("{player}", name)
        .replace("{playtime}", formattedTime)
}