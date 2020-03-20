// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import java.util.concurrent.atomic.AtomicBoolean;

public final class InternalEventBus {
    private static InternalEventBus ourInstance = new InternalEventBus();
    public static InternalEventBus getInstance() {
        return ourInstance;
    }

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final EventBus eventBus;
    private final AsyncEventBus asyncEventBus;
    private InternalEventBus() {
        this.eventBus = new EventBus("task-bus");
        this.asyncEventBus = new AsyncEventBus("async-bus", Threading.EVENT_BUS_POOL);
        this.running.set(true);
    }

    public void register(Object object) {
        eventBus.register(object);
        asyncEventBus.register(object);
    }

    public void post(Object event) {
        if (running.get())
            eventBus.post(event);
    }

    public void postAsync(Object event) {
        if (running.get())
            asyncEventBus.post(event);
    }

    public void unregister(Object object) {
        eventBus.unregister(object);
        asyncEventBus.unregister(object);
    }

    public void shutdown() {
        running.set(false);
        Threading.shutdown(Threading.EVENT_BUS_POOL);
    }
}
