package me.wolfity.playtime

import me.wolfity.developmentutil.ext.registerListener
import me.wolfity.developmentutil.ext.uuid
import me.wolfity.developmentutil.files.CustomConfig
import me.wolfity.developmentutil.util.launchAsync
import me.wolfity.developmentutil.util.toSeconds
import me.wolfity.playtime.commands.PlaytimeCommand
import me.wolfity.playtime.player.PlayerManager
import me.wolfity.playtime.commands.UserCommandParameter
import me.wolfity.playtime.commands.UserParameterType
import me.wolfity.playtime.db.DatabaseManager
import me.wolfity.playtime.listeners.PlayerListeners
import me.wolfity.playtime.tasks.PlaytimeUpdateTask
import me.wolfity.playtime.tracking.PlaytimeManager
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import revxrsal.commands.Lamp
import revxrsal.commands.bukkit.BukkitLamp
import revxrsal.commands.bukkit.actor.BukkitCommandActor

lateinit var plugin: PlaytimeTracker

class PlaytimeTracker : JavaPlugin() {

    lateinit var dbConfig: CustomConfig

    companion object {
        // TODO in 1.1.0
//        const val RESOURCE_ID: Int = -1
    }

    private lateinit var lamp: Lamp<BukkitCommandActor>
    private lateinit var _playtimeManager: PlaytimeManager
    private lateinit var _playerManager: PlayerManager

    val playtimeManager: PlaytimeManager
        get() = _playtimeManager

    val playerManager: PlayerManager
        get() = _playerManager

    override fun onEnable() {
        plugin = this
        loadFiles()

        DatabaseManager.init()
        this._playtimeManager = PlaytimeManager()
        this._playerManager = PlayerManager()

        setupLamp()

        registerCommands()
        registerListeners()

        val updateTaskDuration = dbConfig.getLong("save-play-time-interval-seconds").toSeconds().toLong()
        PlaytimeUpdateTask(playtimeManager).runTaskTimerAsynchronously(
            this,
            updateTaskDuration,
            updateTaskDuration
        )
    }

    override fun onDisable() {
        plugin.logger.info("[PlaytimeTracker] Disabling... saving playtime to database")
        launchAsync {
            Bukkit.getOnlinePlayers().forEach {
                playtimeManager.updatePlaytimeForPlayer(it.uuid)
            }
        }
    }

    private fun registerCommands() {
        lamp.register(PlaytimeCommand())
    }

    private fun registerListeners() {
        PlayerListeners().registerListener(this)
    }

    private fun setupLamp() {
        this.lamp = BukkitLamp.builder(this)
            .parameterTypes {
                it.addParameterType(UserCommandParameter::class.java, UserParameterType())
            }
            .build()
    }


    private fun loadFiles() {
        saveDefaultConfig()
        dbConfig = CustomConfig(this, "db.yml")
    }

//    private fun updateCheck() {
//        UpdateChecker.getVersion { version ->
//            if (this.description.version == version) {
//                logger.info("There is not a new update available.");
//            } else {
//                logger.info("There is a new update available for Simple Chat Moderation");
//            }
//        }
//    }
}
