// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.rules

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nodecore.miners.pop.Configuration
import nodecore.miners.pop.contracts.VeriBlockHeader
import nodecore.miners.pop.rules.actions.RuleAction
import org.junit.Test

class BlockHeightRuleTests {
    @Test
    fun evaluateWhenNoConditionsActive() {
        // Given
        val action: RuleAction<Int> = mockk(relaxed = true)
        val config: Configuration = mockk {
            every { getBoolean(any()) } returns false
        }
        val latest: VeriBlockHeader = mockk {
            every { getHeight() } returns 12343
        }
        val previous: VeriBlockHeader = mockk {
            every { getHeight() } returns 12339
        }
        val context: RuleContext = mockk {
            every { latestBlock } returns latest
            every { previousHead } returns previous
        }

        // When
        val rule = BlockHeightRule(action, config)
        rule.evaluate(context)

        // Then
        verify(exactly = 0) {
            action.execute(any())
        }
    }

    @Test
    fun evaluateWhenOnlyKeystoneConditionActive() {
        // Given
        val action: RuleAction<Int> = mockk(relaxed = true)
        val config: Configuration = mockk {
            every { getBoolean(any()) } returns false
            every { getBoolean("auto.mine.round4") } returns true
        }
        val latest: VeriBlockHeader = mockk {
            every { getHeight() } returns 12343
        }
        val previous: VeriBlockHeader = mockk {
            every { getHeight() } returns 12339
        }
        val context: RuleContext = mockk {
            every { latestBlock } returns latest
            every { previousHead } returns previous
        }

        // When
        val rule = BlockHeightRule(action, config)
        rule.evaluate(context)

        // Then
        verify(exactly = 1) {
            action.execute(12340)
        }
    }

    @Test
    fun evaluateWhenAllConditionsActive() {
        // Given
        val action: RuleAction<Int> = mockk(relaxed = true)
        val config: Configuration = mockk {
            every { getBoolean(any()) } returns true
        }
        val latest: VeriBlockHeader = mockk {
            every { getHeight() } returns 12343
        }
        val previous: VeriBlockHeader = mockk {
            every { getHeight() } returns 12339
        }
        val context: RuleContext = mockk {
            every { latestBlock } returns latest
            every { previousHead } returns previous
        }

        // When
        val rule = BlockHeightRule(action, config)
        rule.evaluate(context)

        // Then
        verify(exactly = 1) {
            action.execute(12340)
            action.execute(12341)
            action.execute(12342)
            action.execute(12343)
        }
    }

    @Test
    fun evaluateWhenSingleKeystone() {
        // Given
        val action: RuleAction<Int> = mockk(relaxed = true)
        val config: Configuration = mockk {
            every { getBoolean(any()) } returns true
        }
        val latest: VeriBlockHeader = mockk {
            every { getHeight() } returns 12340
        }
        val previous: VeriBlockHeader = mockk {
            every { getHeight() } returns 12339
        }
        val context: RuleContext = mockk {
            every { latestBlock } returns latest
            every { previousHead } returns previous
        }

        // When
        val rule = BlockHeightRule(action, config)
        rule.evaluate(context)

        // Then
        verify(exactly = 1) {
            action.execute(12340)
        }
    }

    @Test
    fun evaluateWhenSingleRound1() {
        // Given
        val action: RuleAction<Int> = mockk(relaxed = true)
        val config: Configuration = mockk {
            every { getBoolean(any()) } returns true
        }
        val latest: VeriBlockHeader = mockk {
            every { getHeight() } returns 12341
        }
        val previous: VeriBlockHeader = mockk {
            every { getHeight() } returns 12340
        }
        val context: RuleContext = mockk {
            every { latestBlock } returns latest
            every { previousHead } returns previous
        }

        // When
        val rule = BlockHeightRule(action, config)
        rule.evaluate(context)

        // Then
        verify(exactly = 1) {
            action.execute(12341)
        }
    }

    @Test
    fun evaluateWhenSingleRound2() {
        // Given
        val action: RuleAction<Int> = mockk(relaxed = true)
        val config: Configuration = mockk {
            every { getBoolean(any()) } returns true
        }
        val latest: VeriBlockHeader = mockk {
            every { getHeight() } returns 12342
        }
        val previous: VeriBlockHeader = mockk {
            every { getHeight() } returns 12341
        }
        val context: RuleContext = mockk {
            every { latestBlock } returns latest
            every { previousHead } returns previous
        }

        // When
        val rule = BlockHeightRule(action, config)
        rule.evaluate(context)

        // Then
        verify(exactly = 1) {
            action.execute(12342)
        }
    }

    @Test
    fun evaluateWhenSingleRound3() {
        // Given
        val action: RuleAction<Int> = mockk(relaxed = true)
        val config: Configuration = mockk {
            every { getBoolean(any()) } returns true
        }
        val latest: VeriBlockHeader = mockk {
            every { getHeight() } returns 12343
        }
        val previous: VeriBlockHeader = mockk {
            every { getHeight() } returns 12342
        }
        val context: RuleContext = mockk {
            every { latestBlock } returns latest
            every { previousHead } returns previous
        }

        // When
        val rule = BlockHeightRule(action, config)
        rule.evaluate(context)

        // Then
        verify(exactly = 1) {
            action.execute(12343)
        }
    }

    @Test
    fun evaluateWhenPreviousNotSet() {
        // Given
        val action: RuleAction<Int> = mockk(relaxed = true)
        val config: Configuration = mockk {
            every { getBoolean(any()) } returns true
        }
        val latest: VeriBlockHeader = mockk {
            every { getHeight() } returns 12343
        }
        val context: RuleContext = mockk {
            every { latestBlock } returns latest
            every { previousHead } returns null
        }

        // When
        val rule = BlockHeightRule(action, config)
        rule.evaluate(context)

        // Then
        verify(exactly = 1) {
            action.execute(12343)
        }
    }
}
