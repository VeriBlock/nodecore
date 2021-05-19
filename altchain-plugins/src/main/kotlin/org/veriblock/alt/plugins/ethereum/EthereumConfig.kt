package org.veriblock.alt.plugins.ethereum

import org.veriblock.sdk.alt.ChainConfig
import org.veriblock.sdk.alt.ExplorerBaseUrls
import org.veriblock.sdk.alt.PayoutDetectionType
import org.veriblock.sdk.alt.plugin.HttpAuthConfig
import org.veriblock.sdk.alt.plugin.PluginConfig

class EthereumConfig(
    override val host: String = "http://localhost:18332",
    override val auth: HttpAuthConfig? = null,
    override val payoutAddress: String? = null,
    override val blockPeriodSeconds: Int = 15,
    override val keystonePeriod: Int = 40,
    override val neededConfirmations: Int = 10,
    override val spFinalityDelay: Int = 100,
    override val payoutDelay: Int = 400,
    override val blockRoundIndices: List<Int> = (0..keystonePeriod).map {
        if (it == 0) {
            4
        } else {
            it % 3 + 1
        }
    },
    override val autoMineRounds: MutableSet<Int> = HashSet(),
    override val payoutDetectionType: PayoutDetectionType = PayoutDetectionType.BALANCE_DELTA,
    override val explorerBaseUrls: ExplorerBaseUrls,
    val requestLogsPath: String? = null,
    val daemonConnectionTimeout: Int = 5000
) : ChainConfig() {
    constructor(configuration: PluginConfig) : this(
        configuration.host ?: "http://localhost:18332",
        configuration.auth,
        configuration.payoutAddress,
        configuration.blockPeriodSeconds ?: 15,
        configuration.keystonePeriod ?: 40,
        configuration.neededConfirmations ?: 10,
        configuration.spFinalityDelay ?: 100,
        configuration.payoutDelay ?: 400,
        configuration.blockRoundIndices ?: (0..(configuration.keystonePeriod ?: 40)).map {
            if (it == 0) {
                4
            } else {
                it % 3 + 1
            }
        },
        configuration.autoMineRounds.toMutableSet(),
        configuration.payoutDetectionType,
        configuration.explorerBaseUrls,
        configuration.extraConfig["requestLogsPath"],
        configuration.extraConfig["daemonConnectionTimeout"]?.toInt() ?: 5000
    )
}
