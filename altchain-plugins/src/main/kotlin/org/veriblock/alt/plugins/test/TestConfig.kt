package org.veriblock.alt.plugins.test

import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.alt.ChainConfig
import org.veriblock.sdk.alt.plugin.PluginConfig

class TestConfig(
    override val host: String = "http://localhost:10600/api",
    override val payoutAddress: String = "give it to me".toByteArray().toHex(),
    override val keystonePeriod: Int = 10,
    override val neededConfirmations: Int = 20,
    override val spFinalityDelay: Int = 32,
    override val blockRoundIndices: IntArray = (1..keystonePeriod).map { 1 }.toIntArray(),
    val autoMinePeriod: Int? = null
) : ChainConfig() {
    constructor(configuration: PluginConfig) : this(
        configuration.host ?: "http://localhost:18332",
        configuration.payoutAddress ?: "give it to me".toByteArray().toHex(),
        configuration.keystonePeriod ?: 5,
        configuration.neededConfirmations ?: 10,
        configuration.spFinalityDelay ?: 32,
        configuration.blockRoundIndices ?: intArrayOf(4, 1, 2, 1, 2),
        configuration.extraConfig["autoMinePeriod"]?.toIntOrNull()
    )
}
