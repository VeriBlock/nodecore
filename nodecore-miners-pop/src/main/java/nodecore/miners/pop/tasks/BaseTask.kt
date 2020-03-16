// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.tasks;

import nodecore.miners.pop.core.MiningOperation;
import nodecore.miners.pop.model.TaskResult;
import nodecore.miners.pop.services.BitcoinService;
import nodecore.miners.pop.services.NodeCoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseTask {
    protected static final Logger logger = LoggerFactory.getLogger(BaseTask.class);
    protected final NodeCoreService nodeCoreService;
    protected final BitcoinService bitcoinService;

    public abstract BaseTask getNext();

    protected abstract TaskResult executeImpl(MiningOperation state);

    protected BaseTask(NodeCoreService nodeCoreService, BitcoinService bitcoinService) {
        this.nodeCoreService = nodeCoreService;
        this.bitcoinService = bitcoinService;
    }

    public TaskResult execute(MiningOperation state) {
        try {
            return executeImpl(state);
        } catch (Throwable t) {
            logger.error("Fatal error", t);
            return failProcess(state, "Fatal error");
        }
    }

    protected TaskResult failProcess(MiningOperation state, String reason) {
        logger.warn("Operation {} failed for reason: {}", state.getId(), reason);
        state.fail(reason);

        return TaskResult.fail(state);
    }

    protected TaskResult failTask(MiningOperation state, String reason) {
        String output = String.format("[%s] %s", state.getId(), reason);

        logger.error(output);

        return TaskResult.fail(state);
    }
}
