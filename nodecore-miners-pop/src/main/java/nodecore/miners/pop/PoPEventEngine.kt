// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop;

import com.google.common.eventbus.Subscribe;
import nodecore.miners.pop.events.NewVeriBlockFoundEvent;
import nodecore.miners.pop.rules.Rule;
import nodecore.miners.pop.rules.RuleContext;
import nodecore.miners.pop.services.NodeCoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class PoPEventEngine {
    private static final Logger logger = LoggerFactory.getLogger(PoPEventEngine.class);

    private final NodeCoreService nodeCoreService;
    private final Set<Rule> rules;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public PoPEventEngine(NodeCoreService nodeCoreService, Set<Rule> rules) {
        this.nodeCoreService = nodeCoreService;
        this.rules = rules;
        InternalEventBus.getInstance().register(this);
    }

    public void run() {
        running.set(true);
        logger.info("Event engine is now running, found {} rules", rules.size());
    }

    public void shutdown() {
        InternalEventBus.getInstance().unregister(this);
    }

    private void evaluate(RuleContext context) {
        for (Rule rule : this.rules) {
            try {
                rule.evaluate(context);
            } catch (Exception e) {
                logger.error("Error evaluating and executing rule", e);
            }
        }
    }

    @Subscribe
    public void onNewVeriBlockFound(NewVeriBlockFoundEvent event) {
        try {
            RuleContext context = new RuleContext(
                    event.getPreviousHead(),
                    event.getBlock()
            );

            evaluate(context);
        } catch (Exception e) {
            logger.error("Error handling new block", e);
        }
    }
}
