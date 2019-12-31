package org.veriblock.sdk.alt

abstract class ChainConfig {
    abstract val host: String
    open val autoMine: AutoMineConfig? = null

    fun shouldAutoMine(): Boolean {
        val autoMine = autoMine
        return autoMine != null && autoMine.enabledRounds.isNotEmpty()
    }

    fun shouldAutoMine(blockHeight: Int): Boolean {
        val autoMine = autoMine
            ?: return false
        val blockIndex = blockHeight % autoMine.keystonePeriod
        val roundIndex = autoMine.blockRoundIndices[blockIndex]
        return autoMine.enabledRounds.contains(roundIndex)
    }
}

class AutoMineConfig(
    val keystonePeriod: Int,
    val blockRoundIndices: IntArray,
    val enabledRounds: List<Int> = emptyList()
) {
    init {
        require(keystonePeriod > 0) {
            "The keystone period must be a positive non-zero number!"
        }
        require(blockRoundIndices.size == keystonePeriod) {
            "You must specify a round index for each block along the keystone period! ($keystonePeriod)"
        }
        val roundIndices = blockRoundIndices.distinct()
        for (enabledRound in enabledRounds) {
            require(enabledRound in roundIndices) {
                "Round $enabledRound is not defined in the block round indices!"
            }
        }
    }
}
