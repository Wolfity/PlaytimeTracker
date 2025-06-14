package me.wolfity.playtime.tracking

import me.wolfity.developmentutil.ext.uuid
import me.wolfity.playtime.db.PlayTime
import org.bukkit.Bukkit
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlaytimeManager {

    private val activeSessions = ConcurrentHashMap<UUID, SessionData>()

    fun startSession(uuid: UUID) {
        activeSessions[uuid] = SessionData(uuid, System.currentTimeMillis(), 0)
    }

    suspend fun endSession(uuid: UUID) {
        val session = activeSessions.remove(uuid) ?: return
        val now = System.currentTimeMillis()
        val sessionSeconds = (now - session.sessionStartMillis) / 1000
        val totalPlaytime = (loadTotalPlaytime(uuid) ?: 0L) + session.accumulatedSessionSeconds + sessionSeconds
        saveTotalPlaytime(uuid, totalPlaytime)
    }

    suspend fun getTotalPlaytime(uuid: UUID): Long {
        val storedTime = loadTotalPlaytime(uuid) ?: 0L
        val session = activeSessions[uuid]

        val sessionTime = session?.let {
            val now = System.currentTimeMillis()
            val elapsed = (now - it.sessionStartMillis) / 1000
            it.accumulatedSessionSeconds + elapsed
        } ?: 0L

        return storedTime + sessionTime
    }

    suspend fun updatePlaytimeForPlayer(uuid: UUID) {
        val session = activeSessions[uuid] ?: return
        val now = System.currentTimeMillis()
        val sessionSeconds = (now - session.sessionStartMillis) / 1000

        if (sessionSeconds >= 60) {
            session.accumulatedSessionSeconds += sessionSeconds

            session.sessionStartMillis = now

            val totalPlaytime = (loadTotalPlaytime(uuid) ?: 0L) + session.accumulatedSessionSeconds
            saveTotalPlaytime(uuid, totalPlaytime)

            session.accumulatedSessionSeconds = 0L
        }
    }

    suspend fun loadTopPlaytime(limit: Int): List<Pair<UUID, Long>> = newSuspendedTransaction {
        val now = System.currentTimeMillis()

        val topStored = PlayTime.selectAll()
            .orderBy(PlayTime.totalPlaytimeSeconds, SortOrder.DESC)
            .limit(limit)
            .map { it[PlayTime.uuid] to it[PlayTime.totalPlaytimeSeconds] }

        topStored.map { (uuid, storedTime) ->
            val session = activeSessions[uuid]
            val sessionTime = if (session != null) {
                val elapsedSeconds = (now - session.sessionStartMillis) / 1000
                session.accumulatedSessionSeconds + (if (elapsedSeconds > 0) elapsedSeconds else 0)
            } else {
                0L
            }
            uuid to (storedTime + sessionTime)
        }
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
