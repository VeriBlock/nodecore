// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop

import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import nodecore.miners.pop.events.NewVeriBlockFoundEventDto
import nodecore.miners.pop.model.VeriBlockHeader
import nodecore.miners.pop.rules.Rule
import nodecore.miners.pop.rules.RuleContext
import org.junit.Test

class PoPEventEngineTests {
    @Test
    fun onNewVeriBlockFound() {
        // Given
        val latest: VeriBlockHeader = mockk()
        val previous: VeriBlockHeader = mockk()
        val event = NewVeriBlockFoundEventDto(latest, previous)
        val rule: Rule = mockk()
        val rules: Set<Rule> = setOf(rule)
        val ruleContextSlot = slot<RuleContext>()
        val engine = PoPEventEngine(rules)

        // When
        engine.onNewVeriBlockFound(event)

        // Then
        verify(exactly = 1) {
            rule.evaluate(capture(ruleContextSlot))
        }
        val captured: RuleContext = ruleContextSlot.captured
        captured.latestBlock shouldBeSameInstanceAs latest
        captured.previousHead shouldBeSameInstanceAs previous
    }
}
