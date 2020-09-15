package org.veriblock.sdk.alt

import org.veriblock.sdk.alt.plugin.HttpAuthConfig

abstract class ChainConfig {
    abstract val host: String
    abstract val auth: HttpAuthConfig?
    abstract val keystonePeriod: Int
    abstract val neededConfirmations: Int
    abstract val spFinalityDelay: Int
    abstract val payoutAddress: String?
    abstract val payoutDelay: Int
    abstract val blockRoundIndices: List<Int>
    open val autoMineRounds: MutableSet<Int> = HashSet()

    val availableRoundIndices: Set<Int> get() = blockRoundIndices.distinct().toSet()

    fun shouldAutoMine(): Boolean {
        return autoMineRounds.isNotEmpty()
    }

    fun shouldAutoMine(blockHeight: Int): Boolean {
        val blockIndex = blockHeight % keystonePeriod
        val roundIndex = blockRoundIndices[blockIndex]
        return autoMineRounds.contains(roundIndex)
    }

    fun checkValidity() {
        require(keystonePeriod > 0) {
            "The keystone period must be a positive non-zero number!"
        }
        require(blockRoundIndices.size == keystonePeriod) {
            "You must specify a round index for each block along the keystone period! ($keystonePeriod)"
        }
        val roundIndices = availableRoundIndices
        for (enabledRound in autoMineRounds) {
            require(enabledRound in roundIndices) {
                "Round $enabledRound is not defined in the chain's block round indices!"
            }
        }
    }
}
