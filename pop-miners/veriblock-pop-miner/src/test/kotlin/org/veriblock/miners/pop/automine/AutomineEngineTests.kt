// VeriBlock PoP Miner
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.automine

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.veriblock.miners.pop.AutoMineConfig
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.NewVeriBlockFoundEventDto
import org.veriblock.miners.pop.VpmConfig
import org.veriblock.miners.pop.model.VeriBlockHeader
import org.veriblock.miners.pop.service.BitcoinService
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.service.NodeCoreService

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
        val nodeCoreService: NodeCoreService = mockk(relaxed = true)
        val bitcoinService: BitcoinService = mockk(relaxed = true)
        val engine = AutoMineEngine(config, miner, nodeCoreService, bitcoinService)

        every { nodeCoreService.isReady() } returns true
        every { bitcoinService.isServiceReady() } returns true
        every { bitcoinService.isBlockchainDownloaded() } returns true
        every { bitcoinService.isSufficientlyFunded() } returns true

        // When
        engine.run()
        EventBus.newVeriBlockFoundEvent.trigger(event)

        // Then
        coVerify(exactly = 1) {
            miner.mine(1000)
        }
    }
}
