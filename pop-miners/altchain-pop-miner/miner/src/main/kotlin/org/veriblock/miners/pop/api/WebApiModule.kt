package org.veriblock.miners.pop.api

import org.koin.dsl.module
import org.veriblock.miners.pop.api.controller.MiningController

val webApiModule = module {
    single { MiningController(get()) }

    single {
        ApiServer(
            get(),
            listOf(
                get<MiningController>()
            )
        )
    }
}
