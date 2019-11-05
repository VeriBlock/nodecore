package nodecore.miners.pop

import nodecore.miners.pop.common.BitcoinNetwork
import nodecore.miners.pop.contracts.BlockStore
import nodecore.miners.pop.services.*
import nodecore.miners.pop.shell.PopShell
import nodecore.miners.pop.tasks.ProcessManager
import org.bitcoinj.core.Context
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.params.TestNet3Params
import org.koin.dsl.module

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
    single { PoPEventEngine(get(), get()) }
    single { ProcessManager(get(), get()) }
    single { BitcoinBlockCache() }
    single { ChannelBuilder(get()) }
    single { MessageService() }
    single { PoPMiner(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { PoPStateService(get()) }
    single { NodeCoreService(get(), get(), get()) }
    single { BitcoinService(get(), get(), get()) }
    single { PoPMiningScheduler(get(), get(), get()) }
    single { PopShell(get(), get(), get(), get()) }
}
