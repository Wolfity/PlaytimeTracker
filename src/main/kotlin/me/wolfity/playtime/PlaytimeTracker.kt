package me.wolfity.playtime

import me.wolfity.developmentutil.ext.registerListener
import me.wolfity.developmentutil.ext.uuid
import me.wolfity.developmentutil.files.CustomConfig
import me.wolfity.developmentutil.misc.UpdateChecker
import me.wolfity.developmentutil.util.launchAsync
import me.wolfity.developmentutil.util.toSeconds
import me.wolfity.playtime.afk.AFKDetector
import me.wolfity.playtime.afk.AfkCommand
import me.wolfity.playtime.commands.LeaderboardCommands
import me.wolfity.playtime.commands.LeaderboardType
import me.wolfity.playtime.commands.LoginStreakCommand
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
        const val RESOURCE_ID: Int = 126039
    }

    private lateinit var lamp: Lamp<BukkitCommandActor>
    private lateinit var _playtimeManager: PlaytimeManager
    private lateinit var _playerManager: PlayerManager
    private lateinit var _afkDetector: AFKDetector

    private lateinit var updateChecker: UpdateChecker

    val afkDetector: AFKDetector
        get() = _afkDetector

    val playtimeManager: PlaytimeManager
        get() = _playtimeManager

    val playerManager: PlayerManager
        get() = _playerManager

    override fun onEnable() {
        plugin = this
        loadFiles()

        this.updateChecker = UpdateChecker(this, plugin.description.version, RESOURCE_ID)

        updateCheck()

        DatabaseManager.init()
        this._playtimeManager = PlaytimeManager()
        this._afkDetector = AFKDetector(plugin.config.getLong("afk-mark-minutes") * 60 * 1000).also { it.registerListener(this) }
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
        lamp.register(AfkCommand())
        lamp.register(LoginStreakCommand())
        lamp.register(LeaderboardCommands())
    }

    private fun registerListeners() {
        PlayerListeners().registerListener(this)
        updateChecker.registerListener(this)
    }

    private fun setupLamp() {
        this.lamp = BukkitLamp.builder(this)
            .parameterTypes {
                it.addParameterType(UserCommandParameter::class.java, UserParameterType())
            }
            .suggestionProviders { providers ->
                providers.addProvider(LeaderboardType::class.java) {
                    LeaderboardType.entries.map { it.name }
                }
            }
            .build()
    }


    private fun loadFiles() {
        saveDefaultConfig()
        dbConfig = CustomConfig(this, "db.yml")
    }

    private fun updateCheck() {
        updateChecker.getVersion(plugin, RESOURCE_ID) { version ->
            if (this.description.version == version) {
                logger.info("There is not a new update available.");
            } else {
                logger.info("There is a new update available for ${plugin.description.name}");
            }
        }
    }
}
