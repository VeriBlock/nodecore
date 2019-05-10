// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.rules.actions;

import nodecore.miners.pop.contracts.MineResult;
import nodecore.miners.pop.contracts.PoPMiner;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MineActionTests {
    @Test
    public void executeWhenHeightSupplied() {
        PoPMiner miner = mock(PoPMiner.class);
        when(miner.mine(100)).thenReturn(new MineResult());

        MineAction sut = new MineAction(miner);
        sut.execute(100);

        verify(miner).mine(100);
    }

    @Test
    public void executeWhenHeightNull() {
        PoPMiner miner = mock(PoPMiner.class);
        when(miner.mine(null)).thenReturn(new MineResult());

        MineAction sut = new MineAction(miner);
        sut.execute(null);

        verify(miner).mine(null);
    }
}
