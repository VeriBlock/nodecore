package org.veriblock.core.contracts

interface MiningInstruction : WithDetailedInfo {
    val endorsedBlockHeight: Int
}
