package nodecore.miners.pop.api

import nodecore.miners.pop.api.controller.ConfigurationController
import nodecore.miners.pop.api.controller.MiningController
import nodecore.miners.pop.api.controller.WalletController
import org.koin.dsl.module

@JvmField
val webApiModule = module {
    single { ConfigurationController(get()) }
    single { MiningController(get()) }
    single { WalletController(get()) }

    single {
        ApiServer(listOf(
            get<ConfigurationController>(),
            get<MiningController>(),
            get<WalletController>()
        ))
    }
}
