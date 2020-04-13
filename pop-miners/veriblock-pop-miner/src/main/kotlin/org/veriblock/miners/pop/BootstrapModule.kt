package org.veriblock.miners.pop

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.koin.dsl.module
import org.veriblock.core.utilities.Configuration
import org.veriblock.miners.pop.automine.AutoMineEngine
import org.veriblock.miners.pop.model.BlockStore
import org.veriblock.miners.pop.schedule.PoPMiningScheduler
import org.veriblock.miners.pop.service.BitcoinService
import org.veriblock.miners.pop.service.ChannelBuilder
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.service.NodeCoreGateway
import org.veriblock.miners.pop.service.NodeCoreService
import org.veriblock.miners.pop.service.PopStateService
import org.veriblock.miners.pop.shell.PopShell
import org.veriblock.miners.pop.shell.commands.bitcoinWalletCommands
import org.veriblock.miners.pop.shell.commands.configCommands
import org.veriblock.miners.pop.shell.commands.diagnosticCommands
import org.veriblock.miners.pop.shell.commands.miningCommands
import org.veriblock.miners.pop.shell.commands.standardCommands
import org.veriblock.miners.pop.shell.commands.veriBlockWalletCommands
import org.veriblock.miners.pop.storage.KeyValueRepository
import org.veriblock.miners.pop.storage.KeyValueTable
import org.veriblock.miners.pop.storage.OperationRepository
import org.veriblock.miners.pop.storage.OperationStateTable
import org.veriblock.miners.pop.tasks.ProcessManager
import org.veriblock.miners.pop.tasks.VpmTaskService
import org.veriblock.shell.CommandFactory
import java.sql.Connection
import javax.sql.DataSource

@JvmField
val bootstrapModule = module {
    single { BlockStore() }
    single { AutoMineEngine(get(), get()) }
    single { VpmTaskService(get(), get()) }
    single { ProcessManager(get(), get()) }
    single { ChannelBuilder(get()) }
    single { MinerService(get(), get(), get(), get(), get(), get(), get()) }
    single { PopStateService(get(), get()) }
    single { NodeCoreService(get(), get(), get()) }
    single { NodeCoreGateway(get()) }
    single { BitcoinService(get()) }
    single { PoPMiningScheduler(get(), get()) }

    single {
        CommandFactory().apply {
            standardCommands()
            configCommands(get(), get(), get())
            miningCommands(get())
            bitcoinWalletCommands(get())
            veriBlockWalletCommands(get())
            diagnosticCommands(get())
        }
    }
    single { PopShell(get(), get()) }

    // Storage
    single<DataSource> {
        val configuration: Configuration = get()
        val url = "jdbc:sqlite:${configuration.getDatabasePath()}"
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
