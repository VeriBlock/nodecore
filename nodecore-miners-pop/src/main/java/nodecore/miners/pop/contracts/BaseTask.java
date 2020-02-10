// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import nodecore.miners.pop.InternalEventBus;
import nodecore.miners.pop.events.ErrorMessageEvent;
import nodecore.miners.pop.services.BitcoinService;
import nodecore.miners.pop.services.NodeCoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseTask {
    protected static final Logger logger = LoggerFactory.getLogger(BaseTask.class);
    protected final NodeCoreService nodeCoreService;
    protected final BitcoinService bitcoinService;

    public abstract BaseTask getNext();

    protected abstract TaskResult executeImpl(PoPMiningOperationState state);

    protected BaseTask(NodeCoreService nodeCoreService, BitcoinService bitcoinService) {
        this.nodeCoreService = nodeCoreService;
        this.bitcoinService = bitcoinService;
    }

    public TaskResult execute(PoPMiningOperationState state) {
        try {
            return executeImpl(state);
        } catch (Throwable t) {
            logger.error("Fatal error", t);
            return failProcess(state, "Fatal error");
        }
    }

    protected TaskResult failProcess(PoPMiningOperationState state, String reason) {
        logger.warn("Operation {} failed for reason: {}", state.getOperationId(), reason);
        state.fail(reason);

        return TaskResult.fail(state);
    }

    protected TaskResult failTask(PoPMiningOperationState state, String reason) {
        String output = String.format("[%s] %s", state.getOperationId(), reason);
        InternalEventBus.getInstance().post(new ErrorMessageEvent(output));

        return TaskResult.fail(state);
    }
}
