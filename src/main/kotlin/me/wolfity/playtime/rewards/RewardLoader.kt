package me.wolfity.playtime.rewards

import me.wolfity.playtime.plugin

class RewardLoader() {

    val playtimeRewards: Map<Int, String> = loadRewards()

    private fun loadRewards(): Map<Int, String> {
        val section = plugin.config.getConfigurationSection("play-time-rewards")
        val rewardsMap = mutableMapOf<Int, String>()
        section?.getKeys(false)?.forEach { key ->
            val seconds = key.toIntOrNull()
            val command = section.getString(key)
            if (seconds != null && command != null) {
                rewardsMap[seconds] = command
            } else {
                plugin.logger.warning("Invalid playtime-reward entry: $key -> $command")
            }
        }

        return rewardsMap
    }
}

