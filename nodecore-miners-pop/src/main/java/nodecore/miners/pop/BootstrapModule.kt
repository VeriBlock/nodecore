package nodecore.miners.pop

import com.google.gson.GsonBuilder
import nodecore.miners.pop.common.BitcoinNetwork
import nodecore.miners.pop.contracts.BlockStore
import nodecore.miners.pop.services.BitcoinBlockCache
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
import org.bitcoinj.core.Context
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.params.TestNet3Params
import org.koin.dsl.module
import org.veriblock.shell.CommandFactory

@JvmField
val bootstrapModule = module {
    single { ProgramOptions() }
    single { Configuration(get()) }

    single {
        val configuration: Configuration = get()
        val params: NetworkParameters
        params = when (configuration.bitcoinNetwork) {
            BitcoinNetwork.MainNet -> MainNetParams.get()
            BitcoinNetwork.TestNet -> TestNet3Params.get()
            BitcoinNetwork.RegTest -> RegTestParams.get()
            else -> RegTestParams.get()
        }
        Context(params)
    }

    single { BlockStore() }
    single { PoPEventEngine(get()) }
    single { ProcessManager(get(), get()) }
    single { BitcoinBlockCache() }
    single { ChannelBuilder(get()) }
    single { PoPMiner(get(), get(), get(), get(), get(), get()) }
    single { PoPStateService(get()) }
    single { NodeCoreService(get(), get(), get()) }
    single { BitcoinService(get(), get(), get()) }
    single { PoPMiningScheduler(get(), get(), get()) }

    single {
        CommandFactory().apply {
            val configuration: Configuration = get()
            val miner: PoPMiner = get()
            val nodeCoreService: NodeCoreService = get()
            val prettyPrintGson = GsonBuilder().setPrettyPrinting().create()
            standardCommands()
            configCommands(configuration)
            miningCommands(miner, prettyPrintGson)
            bitcoinWalletCommands(miner)
            veriBlockWalletCommands(nodeCoreService, prettyPrintGson)
            diagnosticCommands(miner)
        }
    }
    single { PopShell(get(), get()) }
}
