package org.veriblock.alt.plugins.bitcoin

import org.veriblock.sdk.alt.ChainConfig

class BitcoinConfig(
    override val host: String = "http://localhost:8332",
    val username: String? = null,
    val password: String? = null,
    override val payoutAddress: String? = null,
    override val keystonePeriod: Int = 5,
    override val neededConfirmations: Int = 10,
    override val blockRoundIndices: IntArray = intArrayOf(4, 1, 2, 1, 2),
    override val autoMineRounds: List<Int> = emptyList()
) : ChainConfig()
