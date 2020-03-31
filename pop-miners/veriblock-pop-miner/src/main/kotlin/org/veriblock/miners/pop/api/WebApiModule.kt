package org.veriblock.miners.pop.api

import org.koin.dsl.module
import org.veriblock.miners.pop.api.controller.ConfigurationController
import org.veriblock.miners.pop.api.controller.LastBitcoinBlockController
import org.veriblock.miners.pop.api.controller.MiningController
import org.veriblock.miners.pop.api.controller.QuitController
import org.veriblock.miners.pop.api.controller.WalletController

@JvmField
val webApiModule = module {
    single { ConfigurationController(get(), get(), get()) }
    single { MiningController(get()) }
    single { WalletController(get()) }
    single { LastBitcoinBlockController(get()) }
    single { QuitController() }

    single {
        ApiServer(
            get(),
            listOf(
                get<ConfigurationController>(),
                get<MiningController>(),
                get<WalletController>(),
                get<LastBitcoinBlockController>(),
                get<QuitController>()
            )
        )
    }
}
