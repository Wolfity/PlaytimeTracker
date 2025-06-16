package me.wolfity.playtime.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date

object PlayerRegistry : Table("player_registry") {
    val uuid = uuid("sender")
    val name = varchar("name", 32).index()
    val skin = text("skin").nullable()
    override val primaryKey: PrimaryKey = PrimaryKey(uuid)
}

object PlayTime : Table("play_time") {
    val uuid = uuid("uuid").references(PlayerRegistry.uuid)
    val totalPlaytimeSeconds = long("total_playtime_seconds").default(0L)
    val lastUpdate = long("last_update").default(0L)

    val lastLoginDate = date("last_login_date").nullable()
    val currentStreak = integer("current_streak").default(0)
    val longestStreak = integer("longest_streak").default(0)

    override val primaryKey: PrimaryKey = PrimaryKey(uuid)
}