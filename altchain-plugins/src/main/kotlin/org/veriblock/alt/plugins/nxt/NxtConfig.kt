package org.veriblock.alt.plugins.nxt

import org.veriblock.sdk.alt.ChainConfig
import org.veriblock.sdk.alt.plugin.HttpAuthConfig
import org.veriblock.sdk.alt.plugin.PluginConfig

class NxtConfig(
    override val host: String = "http://localhost:8332",
    override val auth: HttpAuthConfig? = null,
    override val payoutAddress: String? = null,
    override val keystonePeriod: Int = 10,
    override val neededConfirmations: Int = 10,
    override val spFinalityDelay: Int = 100,
    override val payoutInterval: Int = 100,
    override val blockRoundIndices: List<Int> = listOf(4, 1, 2, 3, 1, 2, 3, 1, 2, 3),
    override val autoMineRounds: MutableSet<Int> = HashSet()
) : ChainConfig() {
    constructor(configuration: PluginConfig) : this(
        configuration.host ?: "http://localhost:8332",
        configuration.auth,
        configuration.payoutAddress,
        configuration.keystonePeriod ?: 10,
        configuration.neededConfirmations ?: 20,
        configuration.spFinalityDelay ?: 100,
        configuration.payoutInterval ?: 100,
        configuration.blockRoundIndices ?: listOf(4, 1, 2, 3, 1, 2, 3, 1, 2, 3),
        configuration.autoMineRounds.toMutableSet()
    )
}
