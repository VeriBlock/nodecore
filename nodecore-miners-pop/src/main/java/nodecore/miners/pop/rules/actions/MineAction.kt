// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.rules.actions

import nodecore.miners.pop.MinerService
import org.apache.commons.lang3.StringUtils
import org.veriblock.core.utilities.createLogger

private val logger = createLogger {}

class MineAction(
    private val minerService: MinerService
) : RuleAction<Int> {
    override fun execute(value: Int?) {
        // The given value is the block index to mine (or null)
        val result = minerService.mine(value)
        if (result.didFail()) {
            val errorMessage = StringBuilder()
            for (message in result.messages) {
                errorMessage.append(System.lineSeparator())
                    .append(message.message)
                    .append(": ")
                    .append(StringUtils.join(message.details, "; "))
            }

            logger.error { "Mine Action Failed: $errorMessage" }
        }
    }
}
