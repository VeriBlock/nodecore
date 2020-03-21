package nodecore.miners.pop

import com.google.gson.GsonBuilder
import nodecore.miners.pop.model.BlockStore
import nodecore.miners.pop.schedule.PoPMiningScheduler
import nodecore.miners.pop.services.BitcoinService
import nodecore.miners.pop.services.ChannelBuilder
import nodecore.miners.pop.services.NodeCoreService
import nodecore.miners.pop.services.PoPStateService
import nodecore.miners.pop.shell.PopShell
import nodecore.miners.pop.shell.commands.bitcoinWalletCommands
import nodecore.miners.pop.shell.commands.configCommands
import nodecore.miners.pop.shell.commands.diagnosticCommands
import nodecore.miners.pop.shell.commands.miningCommands
import nodecore.miners.pop.shell.commands.standardCommands
import nodecore.miners.pop.shell.commands.veriBlockWalletCommands
import nodecore.miners.pop.tasks.ProcessManager
import org.koin.dsl.module
import org.veriblock.core.utilities.Configuration
import org.veriblock.shell.CommandFactory

@JvmField
val bootstrapModule = module {
    single { BlockStore() }
    single { EventEngine(get()) }
    single { ProcessManager(get(), get()) }
    single { ChannelBuilder(get()) }
    single { MinerService(get(), get(), get(), get(), get(), get()) }
    single { PoPStateService(get(), get()) }
    single { NodeCoreService(get(), get(), get()) }
    single { BitcoinService(get()) }
    single { PoPMiningScheduler(get(), get()) }

    single {
        CommandFactory().apply {
            val configuration: Configuration = get()
            val minerService: MinerService = get()
            val nodeCoreService: NodeCoreService = get()
            val prettyPrintGson = GsonBuilder().setPrettyPrinting().create()
            standardCommands()
            configCommands(configuration)
            miningCommands(minerService, prettyPrintGson)
            bitcoinWalletCommands(minerService)
            veriBlockWalletCommands(nodeCoreService, prettyPrintGson)
            diagnosticCommands(minerService)
        }
    }
    single { PopShell(get(), get()) }
}
