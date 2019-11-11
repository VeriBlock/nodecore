package nodecore.miners.pop.api

import nodecore.miners.pop.api.controller.ConfigurationController
import nodecore.miners.pop.api.controller.LastBitcoinBlockController
import nodecore.miners.pop.api.controller.MiningController
import nodecore.miners.pop.api.controller.QuitController
import nodecore.miners.pop.api.controller.WalletController
import org.koin.dsl.module

@JvmField
val webApiModule = module {
    single { ConfigurationController(get()) }
    single { MiningController(get()) }
    single { WalletController(get()) }
    single { LastBitcoinBlockController(get()) }
    single { QuitController() }

    single {
        ApiServer(listOf(
            get<ConfigurationController>(),
            get<MiningController>(),
            get<WalletController>(),
            get<LastBitcoinBlockController>(),
            get<QuitController>()
        ))
    }
}
