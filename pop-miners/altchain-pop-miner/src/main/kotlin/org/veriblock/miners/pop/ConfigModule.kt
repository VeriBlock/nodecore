package org.veriblock.miners.pop

import org.koin.core.module.Module
import org.koin.dsl.module
import org.veriblock.core.params.NetworkConfig
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.params.defaultRegTestProgPoWParameters
import org.veriblock.core.params.getDefaultNetworkParameters
import org.veriblock.core.utilities.Configuration
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.service.AltchainPopMinerService

fun configModule(): Module {
    // Config
    val configuration = Configuration()
    val minerConfig: MinerConfig = configuration.extract("miner") ?: MinerConfig()

    return module {
        single { configuration }
        single { minerConfig }

        // Context
        single { getDefaultNetworkParameters(minerConfig.network, minerConfig.progpowGenesis) }
        single { ApmContext(get(), get()) }

        single { AltchainPopMinerService(get(), get(), get(), get(), get(), get()) }
    }
}

class MinerConfig(
    val network: String = "testnet",
    var feePerByte: Long = 1_000,
    var maxFee: Long = 10_000_000,
    val connectDirectlyTo: List<String> = emptyList(),
    val mock: Boolean = false,
    val progpowGenesis: Boolean = false
)
