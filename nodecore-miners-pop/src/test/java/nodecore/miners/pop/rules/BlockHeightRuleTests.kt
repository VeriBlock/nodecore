// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.rules;

import nodecore.miners.pop.contracts.Configuration;
import nodecore.miners.pop.contracts.VeriBlockHeader;
import nodecore.miners.pop.rules.actions.RuleAction;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class BlockHeightRuleTests {
    @Test
    public void evaluateWhenNoConditionsActive() {
        RuleAction action = (RuleAction<Integer>)mock(RuleAction.class);
        Configuration config = mock(Configuration.class);
        when(config.getBoolean(anyString())).thenReturn(false);

        VeriBlockHeader latest = mock(VeriBlockHeader.class);
        VeriBlockHeader previous = mock(VeriBlockHeader.class);

        when(latest.getHeight()).thenReturn(12343);
        when(previous.getHeight()).thenReturn(12339);

        RuleContext context = mock(RuleContext.class);
        when(context.getLatestBlock()).thenReturn(latest);
        when(context.getPreviousHead()).thenReturn(previous);

        BlockHeightRule rule = new BlockHeightRule(action, config);
        rule.evaluate(context);

        verify(action, never()).execute(any());
    }

    @Test
    public void evaluateWhenOnlyKeystoneConditionActive() {
        RuleAction action = (RuleAction<Integer>)mock(RuleAction.class);
        Configuration config = mock(Configuration.class);
        when(config.getBoolean(anyString())).thenReturn(false);
        when(config.getBoolean("auto.mine.round4")).thenReturn(true);

        VeriBlockHeader latest = mock(VeriBlockHeader.class);
        VeriBlockHeader previous = mock(VeriBlockHeader.class);

        when(latest.getHeight()).thenReturn(12343);
        when(previous.getHeight()).thenReturn(12339);

        RuleContext context = mock(RuleContext.class);
        when(context.getLatestBlock()).thenReturn(latest);
        when(context.getPreviousHead()).thenReturn(previous);

        BlockHeightRule rule = new BlockHeightRule(action, config);
        rule.evaluate(context);

        verify(action).execute(12340);
    }

    @Test
    public void evaluateWhenAllConditionsActive() {
        RuleAction action = (RuleAction<Integer>)mock(RuleAction.class);
        Configuration config = mock(Configuration.class);
        when(config.getBoolean(anyString())).thenReturn(true);

        VeriBlockHeader latest = mock(VeriBlockHeader.class);
        VeriBlockHeader previous = mock(VeriBlockHeader.class);

        when(latest.getHeight()).thenReturn(12343);
        when(previous.getHeight()).thenReturn(12339);

        RuleContext context = mock(RuleContext.class);
        when(context.getLatestBlock()).thenReturn(latest);
        when(context.getPreviousHead()).thenReturn(previous);

        BlockHeightRule rule = new BlockHeightRule(action, config);
        rule.evaluate(context);

        verify(action).execute(12340);
        verify(action).execute(12341);
        verify(action).execute(12342);
        verify(action).execute(12343);
    }

    @Test
    public void evaluateWhenSingleKeystone() {
        RuleAction action = (RuleAction<Integer>)mock(RuleAction.class);
        Configuration config = mock(Configuration.class);
        when(config.getBoolean(anyString())).thenReturn(true);

        VeriBlockHeader latest = mock(VeriBlockHeader.class);
        VeriBlockHeader previous = mock(VeriBlockHeader.class);

        when(latest.getHeight()).thenReturn(12340);
        when(previous.getHeight()).thenReturn(12339);

        RuleContext context = mock(RuleContext.class);
        when(context.getLatestBlock()).thenReturn(latest);
        when(context.getPreviousHead()).thenReturn(previous);

        BlockHeightRule rule = new BlockHeightRule(action, config);
        rule.evaluate(context);

        verify(action, times(1)).execute(anyInt());
    }

    @Test
    public void evaluateWhenSingleRound1() {
        RuleAction action = (RuleAction<Integer>)mock(RuleAction.class);
        Configuration config = mock(Configuration.class);
        when(config.getBoolean(anyString())).thenReturn(true);

        VeriBlockHeader latest = mock(VeriBlockHeader.class);
        VeriBlockHeader previous = mock(VeriBlockHeader.class);

        when(latest.getHeight()).thenReturn(12341);
        when(previous.getHeight()).thenReturn(12340);

        RuleContext context = mock(RuleContext.class);
        when(context.getLatestBlock()).thenReturn(latest);
        when(context.getPreviousHead()).thenReturn(previous);

        BlockHeightRule rule = new BlockHeightRule(action, config);
        rule.evaluate(context);

        verify(action, times(1)).execute(anyInt());
    }

    @Test
    public void evaluateWhenSingleRound2() {
        RuleAction action = (RuleAction<Integer>)mock(RuleAction.class);
        Configuration config = mock(Configuration.class);
        when(config.getBoolean(anyString())).thenReturn(true);

        VeriBlockHeader latest = mock(VeriBlockHeader.class);
        VeriBlockHeader previous = mock(VeriBlockHeader.class);

        when(latest.getHeight()).thenReturn(12342);
        when(previous.getHeight()).thenReturn(12341);

        RuleContext context = mock(RuleContext.class);
        when(context.getLatestBlock()).thenReturn(latest);
        when(context.getPreviousHead()).thenReturn(previous);

        BlockHeightRule rule = new BlockHeightRule(action, config);
        rule.evaluate(context);

        verify(action, times(1)).execute(anyInt());
    }

    @Test
    public void evaluateWhenSingleRound3() {
        RuleAction action = (RuleAction<Integer>)mock(RuleAction.class);
        Configuration config = mock(Configuration.class);
        when(config.getBoolean(anyString())).thenReturn(true);

        VeriBlockHeader latest = mock(VeriBlockHeader.class);
        VeriBlockHeader previous = mock(VeriBlockHeader.class);

        when(latest.getHeight()).thenReturn(12343);
        when(previous.getHeight()).thenReturn(12342);

        RuleContext context = mock(RuleContext.class);
        when(context.getLatestBlock()).thenReturn(latest);
        when(context.getPreviousHead()).thenReturn(previous);

        BlockHeightRule rule = new BlockHeightRule(action, config);
        rule.evaluate(context);

        verify(action, times(1)).execute(anyInt());
    }

    @Test
    public void evaluateWhenPreviousNotSet() {
        RuleAction action = (RuleAction<Integer>)mock(RuleAction.class);
        Configuration config = mock(Configuration.class);
        when(config.getBoolean(anyString())).thenReturn(true);

        VeriBlockHeader latest = mock(VeriBlockHeader.class);

        when(latest.getHeight()).thenReturn(12343);

        RuleContext context = mock(RuleContext.class);
        when(context.getLatestBlock()).thenReturn(latest);
        when(context.getPreviousHead()).thenReturn(null);

        BlockHeightRule rule = new BlockHeightRule(action, config);
        rule.evaluate(context);

        verify(action, times(1)).execute(anyInt());
        verify(action).execute(12343);
    }
}
