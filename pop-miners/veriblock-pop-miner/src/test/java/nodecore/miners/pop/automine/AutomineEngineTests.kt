// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.automine

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nodecore.miners.pop.AutoMineConfig
import nodecore.miners.pop.EventBus
import nodecore.miners.pop.MinerService
import nodecore.miners.pop.NewVeriBlockFoundEventDto
import nodecore.miners.pop.VpmConfig
import nodecore.miners.pop.model.VeriBlockHeader
import org.junit.Test

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
