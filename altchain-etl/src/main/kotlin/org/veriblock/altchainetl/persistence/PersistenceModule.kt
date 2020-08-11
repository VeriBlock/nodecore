package org.veriblock.altchainetl.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.koin.dsl.module
import org.veriblock.core.utilities.Configuration
import java.sql.Connection

val persistenceModule = module {
    single(createdAtStart = true) {
        val config = get<Configuration>()

        val databaseConfig = config.extract("altchain-etl.database")
            ?: let {
                val dataDir = System.getenv("DB_DATA_DIR") ?: "db/data"

                DatabaseConfig(
                    url = "jdbc:sqlite:$dataDir/altchain_etl.db",
                    driver = "org.sqlite.JDBC"
                )
            }

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = databaseConfig.url
            driverClassName = databaseConfig.driver
            username = databaseConfig.username
            password = databaseConfig.password
        }

        Database.connect(HikariDataSource(hikariConfig)).apply {
            transactionManager.defaultIsolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED
            transaction(this) {
                SchemaUtils.createMissingTablesAndColumns()
            }
        }
    }
}

data class DatabaseConfig(
    val url: String,
    val driver: String,
    val username: String = "altchain_etl",
    val password: String = "altchain_etl"
)
