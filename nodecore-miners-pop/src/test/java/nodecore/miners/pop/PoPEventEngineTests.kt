// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop;

import nodecore.miners.pop.contracts.NodeCoreService;
import nodecore.miners.pop.contracts.VeriBlockHeader;
import nodecore.miners.pop.events.NewVeriBlockFoundEvent;
import nodecore.miners.pop.rules.Rule;
import nodecore.miners.pop.rules.RuleContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PoPEventEngineTests {
    @Test
    public void onNewVeriBlockFound() {
        NewVeriBlockFoundEvent event = mock(NewVeriBlockFoundEvent.class);
        VeriBlockHeader latest = mock(VeriBlockHeader.class);
        VeriBlockHeader previous = mock(VeriBlockHeader.class);

        when(event.getBlock()).thenReturn(latest);
        when(event.getPreviousHead()).thenReturn(previous);

        Rule rule = mock(Rule.class);
        Set<Rule> rules = new HashSet<>();
        rules.add(rule);

        ArgumentCaptor<RuleContext> argumentCaptor = ArgumentCaptor.forClass(RuleContext.class);

        PoPEventEngine engine = new PoPEventEngine(mock(NodeCoreService.class), rules);
        engine.onNewVeriBlockFound(event);

        verify(rule).evaluate(argumentCaptor.capture());
        RuleContext captured = argumentCaptor.getValue();
        Assert.assertEquals(latest, captured.getLatestBlock());
        Assert.assertEquals(previous, captured.getPreviousHead());
    }
}
