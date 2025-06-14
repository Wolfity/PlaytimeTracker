package me.wolfity.playtime.tracking

import java.util.UUID

data class SessionData(
    val uuid: UUID,
    var sessionStartMillis: Long,
    var accumulatedSessionSeconds: Long = 0L
)