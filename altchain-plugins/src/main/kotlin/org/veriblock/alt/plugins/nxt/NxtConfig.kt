package org.veriblock.alt.plugins.nxt

import org.veriblock.sdk.alt.ChainConfig
import org.veriblock.sdk.alt.plugin.PluginConfig

class NxtConfig(
    override val host: String = "http://localhost:8332",
    val username: String? = null,
    val password: String? = null,
    override val payoutAddress: String? = null,
    override val keystonePeriod: Int = 10,
    override val neededConfirmations: Int = 10,
    override val spFinalityDelay: Int = 100,
    override val blockRoundIndices: IntArray = intArrayOf(4, 1, 2, 3, 1, 2, 3, 1, 2, 3),
    override val autoMineRounds: List<Int> = emptyList()
) : ChainConfig() {
    constructor(configuration: PluginConfig) : this(
        configuration.host ?: "http://localhost:8332",
        configuration.username,
        configuration.password,
        configuration.payoutAddress,
        configuration.keystonePeriod ?: 10,
        configuration.neededConfirmations ?: 20,
        configuration.spFinalityDelay ?: 100,
        configuration.blockRoundIndices ?: intArrayOf(4, 1, 2, 3, 1, 2, 3, 1, 2, 3),
        configuration.autoMineRounds
    )
}
