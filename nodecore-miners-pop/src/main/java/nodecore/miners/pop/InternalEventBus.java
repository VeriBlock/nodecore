// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InternalEventBus {
    private static final Logger logger = LoggerFactory.getLogger(InternalEventBus.class);

    private static InternalEventBus ourInstance = new InternalEventBus();

    public static InternalEventBus getInstance() {
        return ourInstance;
    }

    private final EventBus eventBus;

    private InternalEventBus() {
        this.eventBus = new EventBus();
    }

    public void register(Object object) {
        eventBus.register(object);
    }

    public void post(Object event) {
        try {
            logger.debug("Posted event: {}", event.toString());
            eventBus.post(event);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void unregister(Object object) {
        eventBus.unregister(object);
    }
}
