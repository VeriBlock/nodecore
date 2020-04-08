// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.automine

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.veriblock.miners.pop.AutoMineConfig
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.NewVeriBlockFoundEventDto
import org.veriblock.miners.pop.VpmConfig
import org.veriblock.miners.pop.model.VeriBlockHeader
import org.veriblock.miners.pop.service.MinerService

class AutomineEngineTests {
    @Test
    fun onNewVeriBlockFound() {
        // Given
        val latest: VeriBlockHeader = mockk {
            every { getHeight() } returns 1000
        }
        val event = NewVeriBlockFoundEventDto(latest, null)
        val config = VpmConfig(autoMine = AutoMineConfig(true, true, true, true))
        val miner: MinerService = mockk(relaxed = true)
        val engine = AutoMineEngine(config, miner)

        // When
        engine.run()
        EventBus.newVeriBlockFoundEvent.trigger(event)

        // Then
        verify(exactly = 1) {
            miner.mine(1000)
        }
    }
}
