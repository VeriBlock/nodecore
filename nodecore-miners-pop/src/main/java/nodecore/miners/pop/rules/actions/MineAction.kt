// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.rules.actions

import nodecore.miners.pop.InternalEventBus
import nodecore.miners.pop.PoPMiner
import nodecore.miners.pop.events.ErrorMessageEvent
import org.apache.commons.lang3.StringUtils

class MineAction(
    private val miner: PoPMiner
) : RuleAction<Int> {
    override fun execute(value: Int?) {
        // The given value is the block index to mine (or null)
        val result = miner.mine(value)
        if (result.didFail()) {
            val errorMessage = StringBuilder()
            for (message in result.messages) {
                errorMessage.append(System.lineSeparator())
                    .append(message.message)
                    .append(": ")
                    .append(StringUtils.join(message.details, "; "))
            }
            InternalEventBus.getInstance().post(ErrorMessageEvent(String.format("Mine Action Failed: %s", errorMessage.toString())))
        }
    }
}
