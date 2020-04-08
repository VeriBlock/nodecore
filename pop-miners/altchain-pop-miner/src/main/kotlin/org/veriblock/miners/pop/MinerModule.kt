// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.koin.dsl.module
import org.veriblock.core.utilities.Configuration
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.Context
import org.veriblock.lite.params.NetworkConfig
import org.veriblock.lite.params.NetworkParameters
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.miners.pop.service.AltchainPopMinerService
import org.veriblock.miners.pop.service.ApmTaskService
import org.veriblock.miners.pop.service.MinerConfig
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.service.MockMinerService
import org.veriblock.miners.pop.service.OperationSerializer
import org.veriblock.miners.pop.service.OperationService
import org.veriblock.miners.pop.shell.commands.altchainCommands
import org.veriblock.miners.pop.shell.configure
import org.veriblock.miners.pop.storage.KeyValueRepository
import org.veriblock.miners.pop.storage.KeyValueTable
import org.veriblock.miners.pop.storage.OperationRepository
import org.veriblock.miners.pop.storage.OperationStateTable
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.Shell
import java.sql.Connection
import javax.sql.DataSource

val minerModule = module {
    // Config
    val configuration = Configuration()
    val minerConfig: MinerConfig = configuration.extract("miner") ?: MinerConfig()
    single { configuration }
    single { minerConfig }

    // Context
    single {
        val config = configuration.extract("nodecore")
            ?: NetworkConfig()
        NetworkParameters(config)
    }
    single { Context(get(), get()) }

    // Miner
    if (!minerConfig.mock) {
        single { NodeCoreLiteKit(get()) }
        single<MinerService> { AltchainPopMinerService(get(), get(), get(), get(), get(), get(), get()) }
    } else {
        single<MinerService> { MockMinerService(get(), get()) }
    }
    single { SecurityInheritingService(get(), get()) }
    single {
        CommandFactory().apply {
            configure(get(), get(), get())
            if (!minerConfig.mock) {
                altchainCommands(get(), get(), get())
            }
        }
    }
    single { Shell(get()) }
    single { ApmTaskService(get(), get()) }
    single { OperationSerializer(get(), get()) }
    single { OperationService(get(), get()) }
    single { PluginService(get()) }

    // Storage
    single<DataSource> {
        val context: Context = get()
        val sqliteDbFile = context.directory.resolve("altchain-pop-miner.db")
        val url = "jdbc:sqlite:$sqliteDbFile"
        val hikariConfig = HikariConfig().apply {
            driverClassName = "org.sqlite.JDBC"
            jdbcUrl = url
        }
        HikariDataSource(hikariConfig)
    }

    single {
        Database.connect(get<DataSource>()).apply {
            transactionManager.defaultIsolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED
            transaction(this) {
                SchemaUtils.createMissingTablesAndColumns(
                    OperationStateTable,
                    KeyValueTable
                )
            }
        }
    }

    single { OperationRepository(get()) }
    single { KeyValueRepository(get()) }
}
