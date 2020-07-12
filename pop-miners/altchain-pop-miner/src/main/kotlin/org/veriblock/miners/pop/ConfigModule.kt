package org.veriblock.miners.pop

import com.typesafe.config.ConfigException
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
    val configuration = try {
        Configuration()
    } catch (e: ConfigException) {
        error("Unable to parse the configuration: ${e.message}")
    }
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
