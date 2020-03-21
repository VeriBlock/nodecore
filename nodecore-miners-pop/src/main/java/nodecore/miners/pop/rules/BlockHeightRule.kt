// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.rules

import nodecore.miners.pop.VpmConfig
import nodecore.miners.pop.rules.actions.RuleAction
import nodecore.miners.pop.rules.conditions.KeystoneBlockCondition
import nodecore.miners.pop.rules.conditions.Round1Condition
import nodecore.miners.pop.rules.conditions.Round2Condition
import nodecore.miners.pop.rules.conditions.Round3Condition

class BlockHeightRule(
    var action: RuleAction<Int>,
    private val configuration: VpmConfig
) : Rule {

    val conditions = listOf(
        KeystoneBlockCondition(),
        Round1Condition(),
        Round2Condition(),
        Round3Condition()
    )

    override fun evaluate(context: RuleContext) {
        val previousHead = context.previousHead
        val latestBlock = context.latestBlock
        var start: Int
        val end: Int
        end = latestBlock!!.getHeight()
        start = end
        if (previousHead != null) {
            start = previousHead.getHeight() + 1
        }
        for (i in start..end) {
            for (condition in conditions) {
                if (condition.isActive(configuration) && condition.evaluate(i)) {
                    action.execute(i)
                }
            }
        }
    }
}
