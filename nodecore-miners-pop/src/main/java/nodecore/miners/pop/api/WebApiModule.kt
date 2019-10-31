package nodecore.miners.pop.api

import nodecore.miners.pop.api.controller.ConfigurationController
import nodecore.miners.pop.api.controller.MiningController
import nodecore.miners.pop.api.controller.WalletController
import org.koin.core.qualifier.named
import org.koin.dsl.module

@JvmField
val webApiModule = module {
    single(named("configController")) { ConfigurationController(get()) }
    single(named("miningController")) { MiningController(get()) }
    single(named("walletController")) { WalletController(get()) }

    single { ApiServer(listOf(
        get(named("configController")),
        get(named("miningController")),
        get(named("walletController"))
    )) }
}
