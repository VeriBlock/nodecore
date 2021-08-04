package org.veriblock.miners.pop.api

import org.koin.dsl.module
import org.veriblock.miners.pop.api.controller.ConfigurationController
import org.veriblock.miners.pop.api.controller.DiagnosticController
import org.veriblock.miners.pop.api.controller.MiningController
import org.veriblock.miners.pop.api.controller.NetworkController
import org.veriblock.miners.pop.api.controller.QuitController
import org.veriblock.miners.pop.api.controller.VersionController
import org.veriblock.miners.pop.api.controller.WalletController

val webApiModule = module {
    single { MiningController(get(), get(), get()) }
    single { DiagnosticController(get()) }
    single { ConfigurationController(get(), get(), get()) }
    single { WalletController(get()) }
    single { NetworkController(get()) }
    single { QuitController() }
    single { VersionController() }

    single {
        ApiServer(
            get(),
            listOf(
                get<MiningController>(),
                get<DiagnosticController>(),
                get<ConfigurationController>(),
                get<QuitController>(),
                get<WalletController>(),
                get<NetworkController>(),
                get<VersionController>()
            )
        )
    }
}
