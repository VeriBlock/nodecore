package org.veriblock.miners.pop.api

import org.koin.dsl.module
import org.veriblock.miners.pop.api.controller.MiningController

@JvmField
val webApiModule = module {
    single { MiningController(get()) }

    single {
        ApiServer(listOf(
            get<MiningController>()
        ))
    }
}
