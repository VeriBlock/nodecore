// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
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
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.miners.pop.service.ApmTaskService
import org.veriblock.miners.pop.service.ApmOperationExplainer
import org.veriblock.miners.pop.service.DiagnosticService
import org.veriblock.miners.pop.service.OperationSerializer
import org.veriblock.miners.pop.service.OperationService
import org.veriblock.miners.pop.shell.configure
import org.veriblock.miners.pop.storage.KeyValueRepository
import org.veriblock.miners.pop.storage.KeyValueTable
import org.veriblock.miners.pop.storage.ApmOperationRepository
import org.veriblock.miners.pop.storage.ApmOperationStateTable
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.Shell
import java.sql.Connection
import javax.sql.DataSource

val minerModule = module {
    single { ApmOperationExplainer(get()) }
    single { SecurityInheritingService(get(), get(), get()) }
    single {
        CommandFactory().apply {
            configure(get(), get(), get(), get(), get(), get(), get())
        }
    }
    single { Shell(get()) }
    single { ApmTaskService(get()) }
    single { OperationSerializer(get(), get()) }
    single { OperationService(get(), get()) }
    single { PluginService(get()) }
    single { DiagnosticService(get(), get(), get(), get(), get()) }

    // Storage
    single<DataSource> {
        val context: ApmContext = get()
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
                    ApmOperationStateTable,
                    KeyValueTable
                )
            }
        }
    }

    single { ApmOperationRepository(get()) }
    single { KeyValueRepository(get()) }
}
