package org.veriblock.miners.pop.api

import org.koin.dsl.module
import org.veriblock.miners.pop.api.controller.ConfigurationController
import org.veriblock.miners.pop.api.controller.DiagnosticController
import org.veriblock.miners.pop.api.controller.MiningController
import org.veriblock.miners.pop.api.controller.QuitController

val webApiModule = module {
    single { MiningController(get()) }
    single { DiagnosticController(get()) }
    single { ConfigurationController(get(), get(), get()) }
    single { QuitController() }
    single {
        ApiServer(
            get(),
            listOf(
                get<MiningController>(),
                get<DiagnosticController>(),
                get<ConfigurationController>()
                get<QuitController>()
            )
        )
    }
}
