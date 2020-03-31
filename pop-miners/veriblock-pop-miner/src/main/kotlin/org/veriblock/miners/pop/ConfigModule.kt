package org.veriblock.miners.pop

import org.koin.core.module.Module
import org.koin.dsl.module
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.bootOption
import org.veriblock.core.utilities.bootOptions
import org.veriblock.miners.pop.common.ApiConfig
import org.veriblock.miners.pop.common.BitcoinConfig
import org.veriblock.miners.pop.common.NodeCoreRpcConfig
import java.io.File

fun configModule(args: Array<String>): Module {
    // Load boot options
    val bootOptions = bootOptions(
        listOf(
            bootOption(
                opt = "skipAck",
                desc = "Bypasses acknowledgement of seed words on first run",
                keyMapping = "vpm.skipAck"
            )
        ),
        args
    )

    // Load config with the boot options
    val configuration = Configuration(bootOptions = bootOptions)
    val vpmConfig = configuration.extract("vpm") ?: VpmConfig()

    return module {
        single { configuration }
        single { vpmConfig }
        single { vpmConfig.nodeCoreRpc }
    }
}

fun Configuration.getDatabasePath(): String {
    val dataDir = if (getDataDirectory().isNotBlank()) {
        getDataDirectory() + File.separator
    } else {
        ""
    }
    return dataDir + Constants.DEFAULT_DATA_FILE
}

data class VpmConfig(
    val bitcoin: BitcoinConfig = BitcoinConfig(),
    val nodeCoreRpc: NodeCoreRpcConfig = NodeCoreRpcConfig(),
    val api: ApiConfig = ApiConfig(),
    val autoMine: AutoMineConfig = AutoMineConfig(),
    val skipAck: Boolean = false,
    val cronSchedule: String = ""
)

data class AutoMineConfig(
    val round1: Boolean = false,
    val round2: Boolean = false,
    val round3: Boolean = false,
    val round4: Boolean = false
)
