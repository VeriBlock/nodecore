// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource
import com.j256.ormlite.support.ConnectionSource
import org.koin.dsl.module
import org.veriblock.core.utilities.Configuration
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.Context
import org.veriblock.lite.params.NetworkConfig
import org.veriblock.lite.params.NetworkParameters
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.miners.pop.service.OperationService
import org.veriblock.miners.pop.shell.commands.altchainCommands
import org.veriblock.miners.pop.shell.configure
import org.veriblock.miners.pop.storage.KeyValueRepository
import org.veriblock.miners.pop.storage.OperationRepository
import org.veriblock.miners.pop.storage.ormlite.OrmLiteKeyValueRepository
import org.veriblock.miners.pop.storage.ormlite.OrmLitePoPRepository
import org.veriblock.miners.pop.tasks.WorkflowAuthority
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.Shell
import java.sql.SQLException

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
        single<Miner> { AltchainPopMiner(get(), get(), get(), get(), get(), get()) }
    } else {
        single<Miner> { MockMiner(get(), get()) }
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
    single { WorkflowAuthority(get(), get(), get()) }
    single { OperationService(get()) }
    single { PluginService(get()) }

    // Storage
    single<ConnectionSource> {
        val context: Context = get()
        val sqliteDbFile = context.directory.resolve("pop.dat")
        val url = "jdbc:sqlite:$sqliteDbFile"
        try {
            JdbcPooledConnectionSource(url)
        } catch (e: SQLException) {
            throw e
        }
    }

    single<OperationRepository> { OrmLitePoPRepository(get()) }
    single<KeyValueRepository> { OrmLiteKeyValueRepository(get()) }
}
