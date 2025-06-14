package me.wolfity.playtime.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.wolfity.playtime.db.PlayerRegistry
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

data class PlayerData(val uuid: UUID, val name: String, val skin: String?)

class PlayerManager {

    suspend fun registerPlayer(uuid: UUID, name: String, skin: String?) = withContext(Dispatchers.IO) {
        transaction {
            PlayerRegistry.insertIgnore {
                it[PlayerRegistry.uuid] = uuid
                it[PlayerRegistry.name] = name
                it[PlayerRegistry.skin] = skin
            }
        }
    }

    suspend fun getDataByName(name: String): PlayerData? {
        return withContext(Dispatchers.IO) {
            transaction {
                PlayerRegistry
                    .selectAll().where { PlayerRegistry.name.lowerCase() eq name.lowercase() }
                    .map { PlayerData(it[PlayerRegistry.uuid], it[PlayerRegistry.name], it[PlayerRegistry.skin]) }
                    .singleOrNull()
            }
        }
    }

    suspend fun getDataByUUID(uuid: UUID): PlayerData? {
        return withContext(Dispatchers.IO) {
            transaction {
                PlayerRegistry
                    .selectAll().where { PlayerRegistry.uuid eq uuid }
                    .map { PlayerData(it[PlayerRegistry.uuid], it[PlayerRegistry.name], it[PlayerRegistry.skin]) }
                    .singleOrNull()
            }
        }
    }

    suspend fun updatePlayerSkin(uuid: UUID, skin: String?) = withContext(Dispatchers.IO) {
        transaction {
            PlayerRegistry.update({ PlayerRegistry.uuid eq uuid }) {
                it[PlayerRegistry.skin] = skin
            }
        }
    }
}