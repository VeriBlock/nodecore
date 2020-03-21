// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop

import nodecore.miners.pop.rules.Rule
import nodecore.miners.pop.rules.RuleContext
import org.veriblock.core.utilities.createLogger
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class EventEngine(
    private val rules: Set<Rule>
) {
    private val running = AtomicBoolean(false)

    fun run() {
        EventBus.newVeriBlockFoundEvent.register(this, ::onNewVeriBlockFound)
        running.set(true)
        logger.info { "Event engine is now running, found ${rules.size} rules" }
    }

    fun shutdown() {
        EventBus.newVeriBlockFoundEvent.unregister(this)
    }

    private fun evaluate(context: RuleContext) {
        for (rule in rules) {
            try {
                rule.evaluate(context)
            } catch (e: Exception) {
                logger.error("Error evaluating and executing rule", e)
            }
        }
    }

    fun onNewVeriBlockFound(event: NewVeriBlockFoundEventDto) {
        try {
            val context = RuleContext(
                event.previousHead,
                event.block
            )
            evaluate(context)
        } catch (e: Exception) {
            logger.error("Error handling new block", e)
        }
    }
}
