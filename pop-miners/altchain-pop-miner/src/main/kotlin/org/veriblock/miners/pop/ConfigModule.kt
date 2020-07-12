package org.veriblock.miners.pop

import org.koin.core.module.Module
import org.koin.dsl.module
import org.veriblock.core.params.NetworkConfig
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.Configuration
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.service.AltchainPopMinerService
import org.veriblock.miners.pop.service.MinerConfig
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.service.mockmining.MockMinerService

fun configModule(): Module {
    // Config
    val configuration = Configuration()
    val minerConfig: MinerConfig = configuration.extract("miner") ?: MinerConfig()

    return module {
        single { configuration }
        single { minerConfig }

        // Context
        single {
            val config = configuration.extract("nodecore")
                ?: NetworkConfig()
            NetworkParameters(config)
        }
        single { Context(get(), get()) }

        if (!minerConfig.mock) {
            single { NodeCoreLiteKit(get(), get()) }
            single<MinerService> { AltchainPopMinerService(get(), get(), get(), get(), get(), get(), get()) }
        } else {
            single<MinerService> { MockMinerService(get(), get()) }
        }
    }
}
