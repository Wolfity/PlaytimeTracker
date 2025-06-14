package me.wolfity.playtime.listeners

import me.wolfity.developmentutil.util.launchAsync
import me.wolfity.playtime.plugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListeners : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val playerManager = plugin.playerManager
        launchAsync {
            val player = event.player
            val storedData = playerManager.getDataByUUID(player.uniqueId)
            val extractedSkinTexture = extractSkinTexture(player)

            if (storedData == null) {
                playerManager.registerPlayer(player.uniqueId, player.name, extractedSkinTexture)
            } else {
                if (storedData.skin != extractedSkinTexture) {
                    playerManager.updatePlayerSkin(player.uniqueId, extractedSkinTexture)
                }
            }

            plugin.playtimeManager.startSession(event.player.uniqueId)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        launchAsync {
            plugin.playtimeManager.endSession(event.player.uniqueId)
        }
    }

    private fun extractSkinTexture(player: Player): String? {
        val texture = player.playerProfile.properties.firstOrNull { it.name.equals("textures") }
        return texture?.value
    }

}