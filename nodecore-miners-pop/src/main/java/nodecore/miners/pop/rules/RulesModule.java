// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.rules;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import nodecore.miners.pop.rules.actions.MineAction;
import nodecore.miners.pop.rules.actions.Mining;
import nodecore.miners.pop.rules.actions.RuleAction;

public class RulesModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(new TypeLiteral<RuleAction<Integer>>() {})
                .annotatedWith(Mining.class)
                .to(MineAction.class)
                .in(Singleton.class);

        Multibinder<Rule> ruleBinder = Multibinder.newSetBinder(binder(), Rule.class);
        ruleBinder.addBinding().to(BlockHeightRule.class);
    }
}
