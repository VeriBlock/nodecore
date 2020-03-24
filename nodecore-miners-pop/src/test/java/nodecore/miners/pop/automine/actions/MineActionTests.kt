// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.automine.actions

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nodecore.miners.pop.MinerService
import nodecore.miners.pop.model.result.MineResult
import org.junit.Test

class MineActionTests {
    @Test
    fun executeWhenHeightSupplied() {
        val minerService: MinerService = mockk {
            every { mine(100) } returns MineResult("")
        }
        val sut = MineAction(minerService)
        sut.execute(100)
        verify {
            minerService.mine(100)
        }
    }

    @Test
    fun executeWhenHeightNull() {
        val minerService: MinerService = mockk {
            every { mine(null) } returns MineResult("")
        }
        val sut = MineAction(minerService)
        sut.execute(null)
        verify {
            minerService.mine(null)
        }
    }
}
