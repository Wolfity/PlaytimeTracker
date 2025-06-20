package me.wolfity.playtime.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.wolfity.playtime.db.type.DatabaseType
import me.wolfity.playtime.plugin
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseManager {

    fun init() {
        val databaseType: DatabaseType = try {
            DatabaseType.valueOf(plugin.dbConfig.getString("database-type").uppercase())
        } catch (e: Exception) {
            throw IllegalArgumentException("Database type '${plugin.dbConfig.getString("database-type")}' is invalid! Choose between ${DatabaseType.entries.map { it.name }}")
        }

        val config = HikariConfig().apply {
            when (databaseType) {
                DatabaseType.SQLITE -> {
                    jdbcUrl = "jdbc:sqlite:plugins/PlaytimeTracker/database.db"
                    driverClassName = "org.sqlite.JDBC"
                }

                DatabaseType.MYSQL -> {
                    jdbcUrl = plugin.dbConfig.getString("mysql.jdbc-url")
                    username = plugin.dbConfig.getString("mysql.username")
                    password = plugin.dbConfig.getString("mysql.password")
                    driverClassName = "com.mysql.cj.jdbc.Driver"
                }
            }

            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.createMissingTablesAndColumns(PlayerRegistry, PlayTime, PlaytimeRewardLog)
        }
    }
}
