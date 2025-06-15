package me.wolfity.playtime.tracking

import me.wolfity.playtime.afk.AFKDetector
import me.wolfity.playtime.db.PlayTime
import me.wolfity.playtime.plugin
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlaytimeManager() {

    private val activeSessions = ConcurrentHashMap<UUID, SessionData>()

    fun startSession(uuid: UUID) {
        activeSessions[uuid] = SessionData(uuid, System.currentTimeMillis(), 0L)
    }

    suspend fun endSession(uuid: UUID) {
        val session = activeSessions.remove(uuid) ?: return
        val nowMillis = System.currentTimeMillis()
        val sessionMillis = nowMillis - session.sessionStartMillis

        val afkSeconds = plugin.afkDetector.getTotalAfkTime(uuid)
        val netSessionSeconds = (sessionMillis / 1000) - afkSeconds

        val totalPlaytime = (loadTotalPlaytime(uuid) ?: 0L) + session.accumulatedSessionSeconds + netSessionSeconds.coerceAtLeast(0L)
        saveTotalPlaytime(uuid, totalPlaytime)
        plugin.afkDetector.resetAFK(uuid)
    }

    suspend fun getTotalPlaytime(uuid: UUID): Long {
        val storedSeconds = loadTotalPlaytime(uuid) ?: 0L
        val session = activeSessions[uuid]

        val sessionSeconds = session?.let {
            val nowMillis = System.currentTimeMillis()
            val elapsedSeconds = (nowMillis - it.sessionStartMillis) / 1000
            it.accumulatedSessionSeconds + elapsedSeconds
        } ?: 0L

        val afkSeconds = plugin.afkDetector.getTotalAfkTime(uuid)
        return (storedSeconds + sessionSeconds - afkSeconds).coerceAtLeast(0L)
    }

    suspend fun updatePlaytimeForPlayer(uuid: UUID) {
        val session = activeSessions[uuid] ?: return
        val nowMillis = System.currentTimeMillis()
        val sessionSeconds = (nowMillis - session.sessionStartMillis) / 1000

        val afkSeconds = plugin.afkDetector.getTotalAfkTime(uuid)
        val netSessionSeconds = sessionSeconds - afkSeconds

        if (netSessionSeconds >= 60) {
            session.accumulatedSessionSeconds += netSessionSeconds
            session.sessionStartMillis = nowMillis

            val totalPlaytime = (loadTotalPlaytime(uuid) ?: 0L) + session.accumulatedSessionSeconds
            saveTotalPlaytime(uuid, totalPlaytime)

            session.accumulatedSessionSeconds = 0L
            plugin.afkDetector.resetAFK(uuid)
        }
    }

    suspend fun loadTopPlaytime(limit: Int): List<Pair<UUID, Long>> = newSuspendedTransaction {
        val nowMillis = System.currentTimeMillis()

        val topStored = PlayTime.selectAll()
            .orderBy(PlayTime.totalPlaytimeSeconds, SortOrder.DESC)
            .limit(limit * 2).associate { it[PlayTime.uuid] to it[PlayTime.totalPlaytimeSeconds] }

        val activeUUIDs = activeSessions.keys

        val combinedUUIDs = (topStored.keys + activeUUIDs).toSet()

        val playtimeList = combinedUUIDs.map { uuid ->
            val storedSeconds = topStored[uuid] ?: 0L

            val session = activeSessions[uuid]
            val sessionSeconds = if (session != null) {
                val elapsedSessionSeconds = ((nowMillis - session.sessionStartMillis) / 1000)
                session.accumulatedSessionSeconds + elapsedSessionSeconds
            } else {
                0L
            }

            val afkSeconds = plugin.afkDetector.getTotalAfkTime(uuid)

            val totalSeconds = (storedSeconds + sessionSeconds - afkSeconds).coerceAtLeast(0L)
            uuid to totalSeconds
        }

        playtimeList.sortedByDescending { it.second }
            .take(limit)
    }


    private suspend fun loadTotalPlaytime(uuid: UUID): Long? = newSuspendedTransaction {
        PlayTime.selectAll().where { PlayTime.uuid eq uuid }
            .map { it[PlayTime.totalPlaytimeSeconds] }
            .singleOrNull()
    }

    private suspend fun saveTotalPlaytime(uuid: UUID, totalSeconds: Long) = newSuspendedTransaction {
        val nowSeconds = System.currentTimeMillis() / 1000
        val exists = PlayTime.selectAll().where { PlayTime.uuid eq uuid }.count() > 0

        if (exists) {
            PlayTime.update({ PlayTime.uuid eq uuid }) {
                it[totalPlaytimeSeconds] = totalSeconds
                it[lastUpdate] = nowSeconds
            }
        } else {
            PlayTime.insert {
                it[PlayTime.uuid] = uuid
                it[totalPlaytimeSeconds] = totalSeconds
                it[lastUpdate] = nowSeconds
            }
        }
    }
}
