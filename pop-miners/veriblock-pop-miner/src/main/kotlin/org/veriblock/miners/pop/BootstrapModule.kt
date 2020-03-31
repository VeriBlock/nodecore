package org.veriblock.miners.pop

import org.koin.dsl.module
import org.veriblock.miners.pop.automine.AutoMineEngine
import org.veriblock.miners.pop.model.BlockStore
import org.veriblock.miners.pop.schedule.PoPMiningScheduler
import org.veriblock.miners.pop.services.BitcoinService
import org.veriblock.miners.pop.services.ChannelBuilder
import org.veriblock.miners.pop.services.MinerService
import org.veriblock.miners.pop.services.NodeCoreService
import org.veriblock.miners.pop.services.PopStateService
import org.veriblock.miners.pop.shell.PopShell
import org.veriblock.miners.pop.shell.commands.bitcoinWalletCommands
import org.veriblock.miners.pop.shell.commands.configCommands
import org.veriblock.miners.pop.shell.commands.diagnosticCommands
import org.veriblock.miners.pop.shell.commands.miningCommands
import org.veriblock.miners.pop.shell.commands.standardCommands
import org.veriblock.miners.pop.shell.commands.veriBlockWalletCommands
import org.veriblock.miners.pop.tasks.ProcessManager
import org.veriblock.shell.CommandFactory

@JvmField
val bootstrapModule = module {
    single { BlockStore() }
    single { AutoMineEngine(get(), get()) }
    single { ProcessManager(get(), get()) }
    single { ChannelBuilder(get()) }
    single { MinerService(get(), get(), get(), get(), get(), get()) }
    single { PopStateService(get(), get()) }
    single { NodeCoreService(get(), get(), get()) }
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
}
