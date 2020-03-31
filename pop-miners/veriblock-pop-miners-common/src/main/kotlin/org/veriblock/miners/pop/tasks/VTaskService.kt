package org.veriblock.miners.pop.tasks

import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.VpmContext
import org.veriblock.miners.pop.core.VpmMerklePath
import org.veriblock.miners.pop.core.VpmSpBlock
import org.veriblock.miners.pop.core.VpmSpTransaction
import org.veriblock.miners.pop.model.PopMiningInstruction

private val logger = createLogger {}

abstract class VTaskService<
    MO : MiningOperation<
        out PopMiningInstruction,
        out VpmSpTransaction,
        out VpmSpBlock,
        out VpmMerklePath,
        out VpmContext
        >
    > : TaskService<MO>()
