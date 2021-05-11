package org.veriblock.alt.plugins.test

import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.alt.ChainConfig
import org.veriblock.sdk.alt.ExplorerBaseUrls
import org.veriblock.sdk.alt.PayoutDetectionType
import org.veriblock.sdk.alt.plugin.HttpAuthConfig
import org.veriblock.sdk.alt.plugin.PluginConfig

class TestConfig(
    override val host: String = "http://localhost:10600/api",
    override val payoutAddress: String = "MY_TEST_ADDRESS".toByteArray().toHex(),
    override val keystonePeriod: Int = 10,
    override val neededConfirmations: Int = 20,
    override val spFinalityDelay: Int = 32,
    override val payoutDelay: Int = 100,
    override val blockRoundIndices: List<Int> = (1..keystonePeriod).map { 1 }.toList(),
    override val payoutDetectionType: PayoutDetectionType = PayoutDetectionType.COINBASE,
    override val explorerBaseUrls: ExplorerBaseUrls,
    val autoMinePeriod: Int? = null
) : ChainConfig() {
    override val auth: HttpAuthConfig? = null

    constructor(configuration: PluginConfig) : this(
        configuration.host ?: "http://localhost:18332",
        configuration.payoutAddress ?: "MY_TEST_ADDRESS".toByteArray().toHex(),
        configuration.keystonePeriod ?: 5,
        configuration.neededConfirmations ?: 10,
        configuration.spFinalityDelay ?: 32,
        configuration.payoutDelay ?: 100,
        configuration.blockRoundIndices ?: listOf(4, 1, 2, 1, 2),
        configuration.payoutDetectionType,
        configuration.explorerBaseUrls,
        configuration.extraConfig["autoMinePeriod"]?.toIntOrNull()
    )
}
