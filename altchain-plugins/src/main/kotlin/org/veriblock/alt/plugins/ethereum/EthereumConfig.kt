package org.veriblock.alt.plugins.ethereum

import org.veriblock.sdk.alt.ChainConfig
import org.veriblock.sdk.alt.PayoutDetectionType
import org.veriblock.sdk.alt.plugin.HttpAuthConfig
import org.veriblock.sdk.alt.plugin.PluginConfig

class EthereumConfig(
    override val host: String = "http://localhost:18332",
    override val auth: HttpAuthConfig? = null,
    override val payoutAddress: String? = null,
    override val keystonePeriod: Int = 5,
    override val neededConfirmations: Int = 10,
    override val spFinalityDelay: Int = 100,
    override val payoutDelay: Int = 50,
    override val blockRoundIndices: List<Int> = listOf(4, 2, 3, 1, 2),
    override val autoMineRounds: MutableSet<Int> = HashSet(),
    override val payoutDetectionType: PayoutDetectionType = PayoutDetectionType.BALANCE_DELTA,
    val requestLogsPath: String? = null,
    val daemonConnectionTimeout: Int = 5000
) : ChainConfig() {
    constructor(configuration: PluginConfig) : this(
        configuration.host ?: "http://localhost:18332",
        configuration.auth,
        configuration.payoutAddress,
        configuration.keystonePeriod ?: 5,
        configuration.neededConfirmations ?: 10,
        configuration.spFinalityDelay ?: 100,
        configuration.payoutDelay ?: 500,
        configuration.blockRoundIndices ?: listOf(4, 2, 3, 1, 2),
        configuration.autoMineRounds.toMutableSet(),
        configuration.payoutDetectionType,
        configuration.extraConfig["requestLogsPath"],
        configuration.extraConfig["daemonConnectionTimeout"]?.toInt() ?: 5000
    )
}
