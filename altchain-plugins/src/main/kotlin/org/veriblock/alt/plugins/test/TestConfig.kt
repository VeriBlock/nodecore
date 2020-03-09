package org.veriblock.alt.plugins.test

import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.alt.ChainConfig

class TestConfig(
    override val host: String = "http://localhost:10600/api",
    override val keystonePeriod: Int = 10,
    override val neededConfirmations: Int = 20,
    override val payoutAddress: String = "give it to me".toByteArray().toHex(),
    override val blockRoundIndices: IntArray = (1..keystonePeriod).map { 1 }.toIntArray(),
    val autoMinePeriod: Int? = null
) : ChainConfig()
