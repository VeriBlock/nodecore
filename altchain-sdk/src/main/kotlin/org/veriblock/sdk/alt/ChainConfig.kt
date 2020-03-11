package org.veriblock.sdk.alt

abstract class ChainConfig {
    abstract val host: String
    abstract val keystonePeriod: Int
    abstract val neededConfirmations: Int
    abstract val spFinalityDelay: Int
    abstract val payoutAddress: String?
    abstract val blockRoundIndices: IntArray
    open val autoMineRounds: List<Int> = emptyList()

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
        val roundIndices = blockRoundIndices.distinct()
        for (enabledRound in autoMineRounds) {
            require(enabledRound in roundIndices) {
                "Round $enabledRound is not defined in the chain's block round indices!"
            }
        }
    }
}
